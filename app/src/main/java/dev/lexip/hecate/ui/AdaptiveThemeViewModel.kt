/*
 * Copyright (C) 2024-2025 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0
 *
 * Please see the License for specific terms regarding permissions and limitations.
 */

package dev.lexip.hecate.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.lexip.hecate.HecateApplication
import dev.lexip.hecate.R
import dev.lexip.hecate.analytics.AnalyticsLogger
import dev.lexip.hecate.data.AdaptiveThreshold
import dev.lexip.hecate.data.UserPreferencesRepository
import dev.lexip.hecate.services.BroadcastReceiverService
import dev.lexip.hecate.ui.setup.PermissionWizardStep
import dev.lexip.hecate.util.DarkThemeHandler
import dev.lexip.hecate.util.LightSensorManager
import dev.lexip.hecate.util.ProximitySensorManager
import dev.lexip.hecate.util.shizuku.ShizukuAvailability
import dev.lexip.hecate.util.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.util.concurrent.atomic.AtomicBoolean

const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

sealed interface UiEvent {
	data class CopyToClipboard(val text: String) : UiEvent
}

data class AdaptiveThemeUiState(
	val adaptiveThemeEnabled: Boolean = false,
	val adaptiveThemeThresholdLux: Float = 1000f,
	val customAdaptiveThemeThresholdLux: Float? = null,
	val showPermissionWizard: Boolean = false,
	val permissionWizardStep: PermissionWizardStep = PermissionWizardStep.ENABLE_DEVELOPER_MODE,
	val permissionWizardCompleted: Boolean = false,
	val hasAutoAdvancedFromDeveloperMode: Boolean = false,
	val hasAutoAdvancedFromConnectUsb: Boolean = false,
	val isDeviceCovered: Boolean = false,
	val isShizukuInstalled: Boolean = false
)

class AdaptiveThemeViewModel(
	private val application: HecateApplication,
	private val userPreferencesRepository: UserPreferencesRepository,
	@Suppress("unused")
	private var _darkThemeHandler: DarkThemeHandler,
	private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
	private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

	private val _uiState = MutableStateFlow(AdaptiveThemeUiState())
	val uiState: StateFlow<AdaptiveThemeUiState> = _uiState.asStateFlow()

	fun setShizukuInstalled(installed: Boolean) {
		if (_uiState.value.isShizukuInstalled == installed) return
		_uiState.value = _uiState.value.copy(isShizukuInstalled = installed)
	}

	fun isAdaptiveThemeEnabled(): Boolean = _uiState.value.adaptiveThemeEnabled

	// One-shot UI events
	private val _uiEvents = MutableSharedFlow<UiEvent>(
		replay = 0,
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	)
	val uiEvents = _uiEvents.asSharedFlow()

	// Wizard + pending ADB command
	private val _pendingAdbCommand = MutableStateFlow("")
	val pendingAdbCommand: StateFlow<String> = _pendingAdbCommand.asStateFlow()

	// Light Sensor
	private val lightSensorManager = LightSensorManager(application.applicationContext)
	private var isListeningToSensor = false

	private val _currentSensorLux = MutableStateFlow(0f)
	val currentSensorLuxFlow: StateFlow<Float> = _currentSensorLux.asStateFlow()
	val currentSensorLux: Float get() = _currentSensorLux.value

	fun updateCurrentSensorLux(lux: Float) {
		_currentSensorLux.value = lux
	}

	// Proximity Sensor
	private val proximitySensorManager = ProximitySensorManager(application.applicationContext)
	private var isListeningToProximity = false

	fun onSetupRequested(packageName: String) {
		onServiceToggleRequested(
			checked = true,
			hasPermission = false,
			packageName = packageName
		)
	}

	private fun startProximityListening() {
		if (isListeningToProximity) return
		isListeningToProximity = true
		proximitySensorManager.startListening({ distance: Float ->
			val covered = distance < 5f
			if (covered != _uiState.value.isDeviceCovered) {
				if (covered) Thread.sleep(1000) // Prevents UI flickering
				_uiState.value = _uiState.value.copy(isDeviceCovered = covered)
			}
		}, sensorDelay = SensorManager.SENSOR_DELAY_UI)
	}

	private fun stopProximityListening() {
		if (!isListeningToProximity) return
		isListeningToProximity = false
		proximitySensorManager.stopListening()
		if (_uiState.value.isDeviceCovered) {
			_uiState.value = _uiState.value.copy(isDeviceCovered = false)
		}
	}

	fun startSensorsIfEnabled() {
		if (_uiState.value.adaptiveThemeEnabled) {
			startLightSensorListening()
			startProximityListening()
		}
	}

	fun stopSensors() {
		stopLightSensorListening()
		stopProximityListening()
	}

	// Temporary variable for custom threshold
	private var customThresholdTemp: Float? = null

	// Keep a reference to the registered Shizuku listener for removal in onCleared
	private var registeredShizukuListener: Shizuku.OnRequestPermissionResultListener? = null

	init {
		viewModelScope.launch {
			userPreferencesRepository.userPreferencesFlow.collect { userPreferences ->
				_uiState.value = _uiState.value.copy(
					adaptiveThemeEnabled = userPreferences.adaptiveThemeEnabled,
					adaptiveThemeThresholdLux = userPreferences.adaptiveThemeThresholdLux,
					customAdaptiveThemeThresholdLux = userPreferences.customAdaptiveThemeThresholdLux,
					permissionWizardCompleted = userPreferences.permissionWizardCompleted
				)

				if (userPreferences.adaptiveThemeEnabled) {
					startSensorsIfEnabled()
				} else {
					stopSensors()
				}
			}
		}

		// Log when Shizuku is present on the device
		val hasShizuku =
			ShizukuAvailability.isShizukuInstalled(application.applicationContext)
		if (hasShizuku) {
			AnalyticsLogger.logServiceEnabled(
				application.applicationContext,
				source = "shizuku_found"
			)
		}

		// Create and register the Shizuku permission listener here so it is fully initialized
		val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
			val granted = grantResult == PackageManager.PERMISSION_GRANTED
			if (granted && requestCode == REQUEST_CODE_SHIZUKU) {
				AnalyticsLogger.logServiceEnabled(
					application.applicationContext,
					source = "shizuku_permission_granted"
				)
				onGrantViaShizukuRequested(application.packageName)
			} else if (!granted && requestCode == REQUEST_CODE_SHIZUKU) {
				Toast.makeText(
					application.applicationContext,
					application.getString(R.string.shizuku_denied_rationale),
					Toast.LENGTH_LONG
				).show()
			}
		}
		registeredShizukuListener = listener
		Shizuku.addRequestPermissionResultListener(listener)
	}

	private fun startLightSensorListening() {
		if (isListeningToSensor) return
		isListeningToSensor = true
		lightSensorManager.startListening({ lux: Float ->
			viewModelScope.launch {
				updateCurrentSensorLux(lux)
			}
		}, sensorDelay = SensorManager.SENSOR_DELAY_UI)
	}

	private fun stopLightSensorListening() {
		if (!isListeningToSensor) return
		isListeningToSensor = false
		lightSensorManager.stopListening()
	}

	override fun onCleared() {
		registeredShizukuListener?.let { Shizuku.removeRequestPermissionResultListener(it) }
		stopLightSensorListening()
		stopProximityListening()
		super.onCleared()
	}

	/**
	 * Toggle adaptive theme service or show permission wizard.
	 * @return true if service was toggled, false if permission wizard is shown.
	 */
	fun onServiceToggleRequested(
		checked: Boolean,
		hasPermission: Boolean,
		packageName: String
	): Boolean {
		if (checked && !hasPermission) {
			startPermissionWizard(packageName)
			AnalyticsLogger.logPermissionErrorShown(
				application.applicationContext,
				reason = "missing_write_secure_settings",
				attemptedAction = "enable_adaptive_theme"
			)
			return false
		}
		updateAdaptiveThemeEnabled(checked)
		return true
	}

	private fun startPermissionWizard(packageName: String) {
		_pendingAdbCommand.value =
			"adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
		_uiState.value = _uiState.value.copy(
			showPermissionWizard = true,
			permissionWizardStep = PermissionWizardStep.ENABLE_DEVELOPER_MODE
		)
	}

	fun goToNextPermissionWizardStep() {
		val next = when (_uiState.value.permissionWizardStep) {
			PermissionWizardStep.ENABLE_DEVELOPER_MODE -> PermissionWizardStep.CONNECT_USB
			PermissionWizardStep.CONNECT_USB -> PermissionWizardStep.GRANT_PERMISSION
			PermissionWizardStep.GRANT_PERMISSION -> PermissionWizardStep.GRANT_PERMISSION
		}
		_uiState.value = _uiState.value.copy(permissionWizardStep = next)
	}

	fun goToPreviousPermissionWizardStep() {
		val prev = when (_uiState.value.permissionWizardStep) {
			PermissionWizardStep.ENABLE_DEVELOPER_MODE -> PermissionWizardStep.ENABLE_DEVELOPER_MODE
			PermissionWizardStep.CONNECT_USB -> PermissionWizardStep.ENABLE_DEVELOPER_MODE
			PermissionWizardStep.GRANT_PERMISSION -> PermissionWizardStep.CONNECT_USB
		}
		_uiState.value = _uiState.value.copy(permissionWizardStep = prev)
	}

	fun dismissPermissionWizard() {
		_uiState.value = _uiState.value.copy(showPermissionWizard = false)
	}

	fun recheckWriteSecureSettingsPermission(granted: Boolean) {
		if (granted) {
			_uiState.value =
				_uiState.value.copy(permissionWizardStep = PermissionWizardStep.GRANT_PERMISSION)
		}
	}

	private val permissionWizardCompletionHandled = AtomicBoolean(false)
	fun completePermissionWizard(
		context: android.content.Context,
		source: String? = null
	) {
		viewModelScope.launch {
			if (permissionWizardCompletionHandled.getAndSet(true)) return@launch
			if (source != null) AnalyticsLogger.logSetupComplete(context, source)
			userPreferencesRepository.updatePermissionWizardCompleted(true)
			dismissPermissionWizard()
			updateAdaptiveThemeEnabled(true)
		}
	}

	fun requestCopyAdbCommand() {
		val cmd = _pendingAdbCommand.value
		viewModelScope.launch {
			_uiEvents.emit(UiEvent.CopyToClipboard(cmd))
		}
	}

	private fun updateAdaptiveThemeEnabled(enable: Boolean) {
		val wasEnabled = _uiState.value.adaptiveThemeEnabled
		viewModelScope.launch {
			userPreferencesRepository.updateAdaptiveThemeEnabled(enable)
			if (enable) {
				startBroadcastReceiverService()
				userPreferencesRepository.ensureAdaptiveThemeThresholdDefault()
				AnalyticsLogger.logServiceEnabled(
					application.applicationContext,
					source = if (wasEnabled) "state_restore" else "ui_toggle"
				)
			} else {
				stopBroadcastReceiverService()
				AnalyticsLogger.logServiceDisabled(
					application.applicationContext,
					source = if (wasEnabled) "ui_toggle" else "state_restore"
				)
			}
		}
	}

	fun updateAdaptiveThemeThresholdByIndex(index: Int) {
		val threshold = AdaptiveThreshold.fromIndex(index)
		val oldLux = _uiState.value.adaptiveThemeThresholdLux
		viewModelScope.launch {
			userPreferencesRepository.updateAdaptiveThemeThresholdLux(threshold.lux)
			// Log threshold change
			AnalyticsLogger.logBrightnessThresholdChanged(
				application.applicationContext,
				oldLux = oldLux,
				newLux = threshold.lux
			)
		}
	}

	fun setCustomAdaptiveThemeThreshold(lux: Float) {
		val oldLux = _uiState.value.adaptiveThemeThresholdLux
		viewModelScope.launch {
			userPreferencesRepository.updateCustomAdaptiveThemeThresholdLux(lux)
			AnalyticsLogger.logBrightnessThresholdChanged(
				application.applicationContext,
				oldLux = oldLux,
				newLux = lux
			)
		}
	}

	val isUsingCustomThreshold: Boolean
		get() = _uiState.value.customAdaptiveThemeThresholdLux != null

	fun getDisplayLuxSteps(baseLux: List<Float>): List<Float> {
		val customLux = _uiState.value.customAdaptiveThemeThresholdLux ?: return baseLux
		val index = AdaptiveThreshold.fromLux(customLux).ordinal
		return baseLux.mapIndexed { i, value -> if (i == index) customLux else value }
	}

	fun getDisplayLabels(labels: List<String>, customLabel: String): List<String> {
		return if (isUsingCustomThreshold) {
			labels.mapIndexed { index, label ->
				if (index == getIndexForCurrentLux()) customLabel else label
			}
		} else labels
	}

	fun onSliderValueCommitted(index: Int) {
		if (isUsingCustomThreshold) {
			customThresholdTemp = null
		}
		updateAdaptiveThemeThresholdByIndex(index)
	}

	fun getIndexForCurrentLux(): Int {
		val lux = customThresholdTemp ?: _uiState.value.adaptiveThemeThresholdLux
		return AdaptiveThreshold.fromLux(lux).ordinal
	}

	fun setPendingCustomSliderLux(lux: Float) {
		customThresholdTemp = lux
	}

	private fun startBroadcastReceiverService() {
		val intent = Intent(application.applicationContext, BroadcastReceiverService::class.java)
		ContextCompat.startForegroundService(application.applicationContext, intent)
	}

	private fun stopBroadcastReceiverService() {
		val intent = Intent(application.applicationContext, BroadcastReceiverService::class.java)
		application.applicationContext.stopService(intent)
	}

	fun onGrantViaShizukuRequested(packageName: String) {
		val context = application.applicationContext

		if (!ShizukuManager.isBinderReady()) {
			Toast.makeText(
				context,
				context.getString(R.string.shizuku_not_ready),
				Toast.LENGTH_LONG
			).show()
			openShizukuAppIfInstalled(context)
			return
		}

		if (!ShizukuManager.hasPermission()) {
			Toast.makeText(
				context,
				context.getString(R.string.shizuku_request_permission),
				Toast.LENGTH_LONG
			).show()
			ShizukuManager.requestPermission(context)
			return
		}

		viewModelScope.launch(ioDispatcher) {
			val result = ShizukuManager.executeGrantViaShizuku(packageName)
			withContext(mainDispatcher) {
				when (result) {
					is ShizukuManager.GrantResult.Success -> {
						// Setup using Shizuku complete
						completePermissionWizard(
							context,
							source = "shizuku"
						)
					}

					is ShizukuManager.GrantResult.ServiceNotRunning -> {
						Toast.makeText(
							context,
							context.getString(R.string.shizuku_not_ready),
							Toast.LENGTH_LONG
						).show()
						openShizukuAppIfInstalled(context)
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

	fun onGrantViaRootRequested(context: android.content.Context, packageName: String) {
		viewModelScope.launch(ioDispatcher) {
			val result = tryGrantViaRoot(packageName)
			withContext(mainDispatcher) {
				when (result) {
					RootGrantResult.Success -> {
						completePermissionWizard(
							context,
							source = "root"
						)
						Toast.makeText(
							context,
							R.string.permission_wizard_permission_granted,
							Toast.LENGTH_SHORT
						).show()
					}

					is RootGrantResult.Failure -> {
						Toast.makeText(
							context,
							R.string.permission_wizard_root_grant_failed,
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

	private fun tryGrantViaRoot(packageName: String): RootGrantResult {
		return try {
			val command = "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
			val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
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

	private fun openShizukuAppIfInstalled(context: android.content.Context) {
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

	companion object {
		private const val REQUEST_CODE_SHIZUKU = 1001
	}
}

class AdaptiveThemeViewModelFactory(
	private val application: HecateApplication,
	private val userPreferencesRepository: UserPreferencesRepository,
	private val darkThemeHandler: DarkThemeHandler
) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(AdaptiveThemeViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return AdaptiveThemeViewModel(
				application,
				userPreferencesRepository,
				darkThemeHandler
			) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}
}