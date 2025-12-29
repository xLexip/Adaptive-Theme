/*
 * Copyright (C) 2025 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0
 *
 * Please see the License for specific terms regarding permissions and limitations.
 */

package dev.lexip.hecate.ui.setup

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.lexip.hecate.Application
import dev.lexip.hecate.R
import dev.lexip.hecate.data.UserPreferencesRepository
import dev.lexip.hecate.logging.Logger
import dev.lexip.hecate.services.BroadcastReceiverService
import dev.lexip.hecate.ui.navigation.NavigationEvent
import dev.lexip.hecate.ui.navigation.NavigationManager
import dev.lexip.hecate.ui.navigation.SetupRoute
import dev.lexip.hecate.util.shizuku.ShizukuAvailability
import dev.lexip.hecate.util.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.util.concurrent.atomic.AtomicBoolean

private const val AUTO_ADVANCE_DELAY = 2
private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
private const val REQUEST_CODE_SHIZUKU = 1001

/**
 * UI state for the setup flow, observed by setup screens.
 */
data class SetupUiState(
	val currentStep: SetupRoute = SetupRoute.DeveloperMode,
	val pendingAdbCommand: String = "",
	val isShizukuInstalled: Boolean = false,
	val isDeveloperOptionsEnabled: Boolean = false,
	val isUsbDebuggingEnabled: Boolean = false,
	val isUsbConnected: Boolean = false,
	val hasWriteSecureSettings: Boolean = false,
	val isSetupCompleted: Boolean = false,
	val isStep1Complete: Boolean = false,
	val isStep2Complete: Boolean = false,
	val isStep3Complete: Boolean = false,
	val autoAdvanceCountdown: Int = 0    // Auto-advance countdown (seconds remaining, 0 = ready to advance or not counting)
)

/**
 * ViewModel for the setup flow, scoped to the setup navigation graph.
 */
class SetupViewModel(
	private val application: Application,
	private val userPreferencesRepository: UserPreferencesRepository,
	private val navigationManager: NavigationManager,
	private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
	private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

	private val _uiState = MutableStateFlow(SetupUiState())
	val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

	private var registeredShizukuListener: Shizuku.OnRequestPermissionResultListener? = null
	private var usbReceiver: BroadcastReceiver? = null
	private val setupCompletionHandled = AtomicBoolean(false)
	private var isMonitoring = false

	// Track which steps have triggered auto-advance to prevent repeated triggers
	private var step1AutoAdvanceTriggered = false
	private var step2AutoAdvanceTriggered = false
	private var step3AutoAdvanceTriggered = false

	private var autoAdvanceJob: kotlinx.coroutines.Job? = null

	init {
		initializeState()
		registerShizukuListener()
		logSetupStarted()
		startEnvironmentMonitoring()
		determineInitialStep()
	}

	private fun initializeState() {
		val context = application.applicationContext
		val hasShizuku = ShizukuAvailability.isShizukuInstalled(context)
		val packageName = context.packageName

		_uiState.update { current ->
			current.copy(
				isShizukuInstalled = hasShizuku,
				pendingAdbCommand = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS",
				isDeveloperOptionsEnabled = checkDeveloperOptionsEnabled(context),
				isUsbDebuggingEnabled = checkUsbDebuggingEnabled(context),
				hasWriteSecureSettings = checkWriteSecureSettingsPermission(context)
			)
		}
	}

	private fun determineInitialStep() {
		val context = application.applicationContext
		val step1Done = checkDeveloperOptionsEnabled(context) && checkUsbDebuggingEnabled(context)
		// We don't skip step 2 based on USB connection alone, as the user might need to see the instructions.
		val step3Done = checkWriteSecureSettingsPermission(context)

		if (step3Done) {
			_uiState.update {
				it.copy(
					isStep1Complete = true,
					isStep2Complete = true,
					isStep3Complete = true
				)
			}
			step1AutoAdvanceTriggered = true
			step2AutoAdvanceTriggered = true
			Logger.logSetupStepOneCompleted(context)
			Logger.logSetupStepTwoCompleted(context)
			navigateToStep(SetupRoute.GrantPermission)
		} else if (step1Done) {
			_uiState.update { it.copy(isStep1Complete = true) }
			step1AutoAdvanceTriggered = true
			Logger.logSetupStepOneCompleted(context)
			navigateToStep(SetupRoute.ConnectUsb)
		}
	}

	private fun registerShizukuListener() {
		val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
			val granted = grantResult == PackageManager.PERMISSION_GRANTED
			if (granted && requestCode == REQUEST_CODE_SHIZUKU) {
				Logger.logServiceEnabled(
					application.applicationContext,
					source = "shizuku_permission_granted"
				)
				onGrantViaShizukuRequested()
			} else if (!granted && requestCode == REQUEST_CODE_SHIZUKU) {
				Toast.makeText(
					application.applicationContext,
					application.getString(R.string.shizuku_denied),
					Toast.LENGTH_LONG
				).show()
				openShizukuAppIfInstalled()
			}
		}
		registeredShizukuListener = listener
		Shizuku.addRequestPermissionResultListener(listener)
	}

	private fun logSetupStarted() {
		val hasShizuku = _uiState.value.isShizukuInstalled
		Logger.logSetupStarted(
			context = application.applicationContext,
			hasShizuku = hasShizuku
		)
	}

	/**
	 * Starts monitoring environment state (developer options, USB, permission).
	 */
	fun startEnvironmentMonitoring() {
		if (isMonitoring) return
		isMonitoring = true

		val context = application.applicationContext

		// Register USB broadcast receiver
		val usbFilter = IntentFilter("android.hardware.usb.action.USB_STATE")
		usbReceiver = object : BroadcastReceiver() {
			override fun onReceive(ctx: Context?, intent: Intent?) {
				val nowConnected = parseUsbIntent(intent)
				_uiState.update { it.copy(isUsbConnected = nowConnected) }
			}
		}
		context.registerReceiver(usbReceiver, usbFilter)

		// Check initial USB state from sticky broadcast
		val sticky = context.registerReceiver(null, usbFilter)
		_uiState.update { it.copy(isUsbConnected = parseUsbIntent(sticky)) }

		// Start periodic polling for settings and permission
		viewModelScope.launch {
			while (isMonitoring) {
				updateEnvironmentState()
				checkAndTriggerStepCompletion()
				delay(1000)
			}
		}
	}

	/**
	 * Check if current step conditions are met and trigger auto-advance countdown if needed.
	 */
	private fun checkAndTriggerStepCompletion() {
		when (_uiState.value.currentStep) {
			SetupRoute.DeveloperMode -> checkStep1Completion()
			SetupRoute.ConnectUsb -> checkStep2Completion()
			SetupRoute.GrantPermission -> checkStep3Completion()
		}
	}

	private fun checkStep1Completion() {
		val state = _uiState.value
		val stepComplete = state.isDeveloperOptionsEnabled && state.isUsbDebuggingEnabled
		if (stepComplete != state.isStep1Complete) {
			_uiState.update { it.copy(isStep1Complete = stepComplete) }
		}
		if (stepComplete && !step1AutoAdvanceTriggered) {
			step1AutoAdvanceTriggered = true
			Logger.logSetupStepOneCompleted(application.applicationContext)
			startAutoAdvanceCountdown { navigateToStep(SetupRoute.ConnectUsb) }
		}
	}

	private fun checkStep2Completion() {
		val state = _uiState.value
		val stepComplete = state.isUsbConnected
		if (stepComplete != state.isStep2Complete) {
			_uiState.update { it.copy(isStep2Complete = stepComplete) }
		}
		if (stepComplete && !step2AutoAdvanceTriggered) {
			step2AutoAdvanceTriggered = true
			Logger.logSetupStepTwoCompleted(application.applicationContext)
			startAutoAdvanceCountdown { navigateToStep(SetupRoute.GrantPermission) }
		}
	}

	private fun checkStep3Completion() {
		val state = _uiState.value
		val stepComplete = state.hasWriteSecureSettings
		if (stepComplete != state.isStep3Complete) {
			_uiState.update { it.copy(isStep3Complete = stepComplete) }
		}
		if (stepComplete && !step3AutoAdvanceTriggered) {
			step3AutoAdvanceTriggered = true
			completeSetup(source = if (state.isUsbConnected) "usb" else "auto")
		}
	}

	/**
	 * Start a countdown before auto-advancing to next step.
	 * User can still manually navigate during countdown.
	 */
	private fun startAutoAdvanceCountdown(onComplete: () -> Unit) {
		autoAdvanceJob?.cancel()
		autoAdvanceJob = viewModelScope.launch {
			for (i in AUTO_ADVANCE_DELAY downTo 1) {
				_uiState.update { it.copy(autoAdvanceCountdown = i) }
				delay(1000)
			}
			_uiState.update { it.copy(autoAdvanceCountdown = 0) }
			onComplete()
		}
	}

	private fun cancelAutoAdvanceCountdown() {
		autoAdvanceJob?.cancel()
		autoAdvanceJob = null
		_uiState.update { it.copy(autoAdvanceCountdown = 0) }
	}

	/**
	 * Stops monitoring environment state (developer options, USB, permission).
	 */
	private fun stopEnvironmentMonitoring() {
		isMonitoring = false
		cancelAutoAdvanceCountdown()
		usbReceiver?.let {
			try {
				application.applicationContext.unregisterReceiver(it)
			} catch (_: IllegalArgumentException) {
				// Already unregistered
			}
		}
		usbReceiver = null
	}

	private fun updateEnvironmentState() {
		val context = application.applicationContext
		_uiState.update { current ->
			current.copy(
				isDeveloperOptionsEnabled = checkDeveloperOptionsEnabled(context),
				isUsbDebuggingEnabled = checkUsbDebuggingEnabled(context),
				hasWriteSecureSettings = checkWriteSecureSettingsPermission(context),
				isUsbConnected = current.isUsbConnected || checkUsbConnectedViaManager(context)
			)
		}
	}

	private fun parseUsbIntent(intent: Intent?): Boolean {
		if (intent == null) return false
		val extras = intent.extras ?: return false
		val connected = extras.getBoolean("connected", false)
		val configured = extras.getBoolean("configured", false)
		val dataConnected = extras.getBoolean("data_connected", false)
		val adb = extras.getBoolean("adb", false)
		val hostConnected = extras.getBoolean("host_connected", false)
		return connected && (configured || dataConnected || adb || hostConnected)
	}

	private fun checkDeveloperOptionsEnabled(context: Context): Boolean {
		return try {
			Settings.Global.getInt(
				context.contentResolver,
				Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
				0
			) == 1
		} catch (_: Exception) {
			false
		}
	}

	private fun checkUsbDebuggingEnabled(context: Context): Boolean {
		return try {
			Settings.Global.getInt(
				context.contentResolver,
				Settings.Global.ADB_ENABLED,
				0
			) == 1
		} catch (_: Exception) {
			false
		}
	}

	private fun checkWriteSecureSettingsPermission(context: Context): Boolean {
		return ContextCompat.checkSelfPermission(
			context, Manifest.permission.WRITE_SECURE_SETTINGS
		) == PackageManager.PERMISSION_GRANTED
	}

	private fun checkUsbConnectedViaManager(context: Context): Boolean {
		return try {
			val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
			usbManager?.deviceList?.isNotEmpty() == true
		} catch (_: Exception) {
			false
		}
	}

	/**
	 * Navigate to next step (Next/Continue buttons).
	 */
	fun navigateToNextStep() {
		cancelAutoAdvanceCountdown()
		val nextStep = when (_uiState.value.currentStep) {
			SetupRoute.DeveloperMode -> SetupRoute.ConnectUsb
			SetupRoute.ConnectUsb -> SetupRoute.GrantPermission
			SetupRoute.GrantPermission -> SetupRoute.GrantPermission // Already at last step
		}
		navigateToStep(nextStep)
	}

	/**
	 * Navigate to a specific step.
	 */
	fun navigateToStep(step: SetupRoute) {
		cancelAutoAdvanceCountdown()
		_uiState.update { it.copy(currentStep = step) }
		navigationManager.tryNavigate(NavigationEvent.ToSetupStep(step))
	}

	/**
	 * Navigate back to previous step, or exit setup if on first step.
	 */
	fun navigateBack() {
		cancelAutoAdvanceCountdown()
		when (_uiState.value.currentStep) {
			SetupRoute.DeveloperMode -> {
				// On first step, exit setup entirely
				exitSetup()
			}

			SetupRoute.ConnectUsb -> {
				// Go back to step 1
				_uiState.update { it.copy(currentStep = SetupRoute.DeveloperMode) }
				navigationManager.tryNavigate(NavigationEvent.Back)
			}

			SetupRoute.GrantPermission -> {
				// Go back to step 2
				_uiState.update { it.copy(currentStep = SetupRoute.ConnectUsb) }
				navigationManager.tryNavigate(NavigationEvent.Back)
			}
		}
	}

	/**
	 * Exit setup flow and return to main screen.
	 */
	fun exitSetup() {
		cancelAutoAdvanceCountdown()
		stopEnvironmentMonitoring()
		navigationManager.tryNavigate(NavigationEvent.ToMainClearingSetup)
	}

	fun completeSetup(source: String? = null) {
		viewModelScope.launch {
			if (setupCompletionHandled.getAndSet(true)) return@launch
			_uiState.update { it.copy(isSetupCompleted = true) }
			stopEnvironmentMonitoring()

			val context = application.applicationContext
			Logger.logSetupComplete(context, source)

			// Activate Adaptive Theme
			withContext(ioDispatcher) {
				userPreferencesRepository.updateSetupCompleted(true)
				userPreferencesRepository.ensureAdaptiveThemeThresholdDefault()
				userPreferencesRepository.updateAdaptiveThemeEnabled(true)

				// Start Service
				val intent =
					Intent(application.applicationContext, BroadcastReceiverService::class.java)
				try {
					ContextCompat.startForegroundService(application.applicationContext, intent)
				} catch (e: Exception) {
					Logger.logException(e)
				}

				Logger.logServiceEnabled(
					application.applicationContext,
					source = "setup_complete"
				)
			}

			// Close Setup
			navigationManager.tryNavigate(NavigationEvent.ToMainClearingSetup)
		}
	}

	fun openDeviceInfoSettings() {
		val context = application.applicationContext
		val intent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		try {
			context.startActivity(intent)
		} catch (_: Exception) {
			context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			})
		}
	}

	fun openDeveloperSettings() {
		val context = application.applicationContext
		val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		try {
			context.startActivity(intent)
		} catch (_: Exception) {
			context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			})
		}
	}

	fun shareAdbCommand() {
		val context = application.applicationContext
		val sendIntent = Intent().apply {
			action = Intent.ACTION_SEND
			putExtra(Intent.EXTRA_TEXT, _uiState.value.pendingAdbCommand)
			type = "text/plain"
		}
		val shareIntent = Intent.createChooser(sendIntent, null).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		context.startActivity(shareIntent)
	}

	fun checkPermissionAndComplete() {
		val context = application.applicationContext
		val nowGranted = checkWriteSecureSettingsPermission(context)
		_uiState.update { it.copy(hasWriteSecureSettings = nowGranted) }

		if (nowGranted) {
			completeSetup(source = if (_uiState.value.isUsbConnected) "usb" else "manual_check")
		}
	}

	fun installShizuku() {
		val context = application.applicationContext
		exitSetup()
		val intent = Intent(
			Intent.ACTION_VIEW,
			"https://play.google.com/store/apps/details?id=$SHIZUKU_PACKAGE".toUri()
		).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		context.startActivity(intent)
	}

	fun onGrantViaShizukuRequested() {
		val context = application.applicationContext
		val packageName = application.packageName

		if (!ShizukuManager.isBinderReady()) {
			Toast.makeText(
				context,
				context.getString(R.string.shizuku_not_ready),
				Toast.LENGTH_LONG
			).show()
			openShizukuAppIfInstalled()
			return
		}

		if (!ShizukuManager.hasPermission(context)) {
			Toast.makeText(
				context,
				context.getString(R.string.shizuku_request_permission),
				Toast.LENGTH_SHORT
			).show()
			ShizukuManager.requestPermission()
			return
		}

		viewModelScope.launch(ioDispatcher) {
			val result = ShizukuManager.executeGrantViaShizuku(context, packageName)
			Logger.logShizukuGrantResult(context, result, packageName)
			withContext(mainDispatcher) {
				when (result) {
					is ShizukuManager.GrantResult.Success -> {
						completeSetup(source = "shizuku")
					}

					is ShizukuManager.GrantResult.ServiceNotRunning -> {
						Toast.makeText(
							context,
							context.getString(R.string.shizuku_not_ready),
							Toast.LENGTH_LONG
						).show()
						openShizukuAppIfInstalled()
					}

					is ShizukuManager.GrantResult.NotAuthorized -> {
						Toast.makeText(
							context,
							context.getString(R.string.shizuku_not_ready),
							Toast.LENGTH_LONG
						).show()
					}

					is ShizukuManager.GrantResult.ShellCommandFailed -> {
						Toast.makeText(
							context,
							context.getString(R.string.shizuku_grant_shell_failed),
							Toast.LENGTH_LONG
						).show()
					}

					is ShizukuManager.GrantResult.Unexpected -> {
						Toast.makeText(
							context,
							context.getString(R.string.shizuku_grant_unexpected),
							Toast.LENGTH_LONG
						).show()
					}
				}
			}
		}
	}

	fun onGrantViaRootRequested() {
		val context = application.applicationContext
		Toast.makeText(context, R.string.setup_root_grant_starting, Toast.LENGTH_SHORT).show()

		viewModelScope.launch(ioDispatcher) {
			val result = tryGrantViaRoot()
			withContext(mainDispatcher) {
				when (result) {
					RootGrantResult.Success -> {
						completeSetup(source = "root")
						Toast.makeText(
							context,
							R.string.setup_permission_granted,
							Toast.LENGTH_SHORT
						).show()
					}

					is RootGrantResult.Failure -> {
						Toast.makeText(
							context,
							R.string.setup_root_grant_failed,
							Toast.LENGTH_SHORT
						).show()
					}
				}
			}
		}
	}

	private sealed interface RootGrantResult {
		data object Success : RootGrantResult
		data class Failure(val reason: String) : RootGrantResult
	}

	private fun tryGrantViaRoot(): RootGrantResult {
		return try {
			val process = Runtime.getRuntime().exec("su")
			val os = java.io.DataOutputStream(process.outputStream)
			os.writeBytes("pm grant dev.lexip.hecate android.permission.WRITE_SECURE_SETTINGS\n")
			os.writeBytes("exit\n")
			os.flush()
			os.close()

			val exitCode = process.waitFor()
			if (exitCode == 0) {
				RootGrantResult.Success
			} else {
				RootGrantResult.Failure("exit_code_$exitCode")
			}
		} catch (e: Exception) {
			RootGrantResult.Failure(e.javaClass.simpleName)
		}
	}

	private fun openShizukuAppIfInstalled() {
		val context = application.applicationContext
		val pm = context.packageManager
		try {
			pm.getPackageInfo(SHIZUKU_PACKAGE, 0)
			val launchIntent = pm.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
			if (launchIntent != null) {
				launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				context.startActivity(launchIntent)
			}
		} catch (_: PackageManager.NameNotFoundException) {
			// Shizuku not installed
		} catch (_: ActivityNotFoundException) {
			// No launchable activity
		}
	}

	override fun onCleared() {
		registeredShizukuListener?.let { Shizuku.removeRequestPermissionResultListener(it) }
		stopEnvironmentMonitoring()
		super.onCleared()
	}
}

class SetupViewModelFactory(
	private val application: Application,
	private val userPreferencesRepository: UserPreferencesRepository,
	private val navigationManager: NavigationManager
) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(SetupViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return SetupViewModel(
				application,
				userPreferencesRepository,
				navigationManager
			) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}
}

