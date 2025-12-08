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
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import dev.lexip.hecate.analytics.AnalyticsLogger
import dev.lexip.hecate.ui.AdaptiveThemeViewModel
import dev.lexip.hecate.util.shizuku.ShizukuAvailability

@Composable
fun PermissionSetupHost(
	viewModel: AdaptiveThemeViewModel,
) {
	val context = LocalContext.current
	val haptic = LocalHapticFeedback.current
	val internalUiState by viewModel.uiState.collectAsState()

	// Track setup-related environment state locally in this host
	var isDeveloperOptionsEnabled by remember { mutableStateOf(false) }
	var isUsbDebuggingEnabled by remember { mutableStateOf(false) }
	var isUsbConnected by remember { mutableStateOf(false) }
	var hasPermission by remember { mutableStateOf(false) }

	val isShizukuInstalled = remember { ShizukuAvailability.isShizukuInstalled(context) }

	SideEffect {
		AnalyticsLogger.logSetupStarted(context)
	}

	// Periodically check developer settings, USB and permission status
	LaunchedEffect(Unit) {
		var previousDevOptionsState = try {
			Settings.Global.getInt(
				context.contentResolver,
				Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
				0
			) == 1
		} catch (_: Exception) {
			false
		}

		var previousUsbDebuggingState = try {
			Settings.Global.getInt(
				context.contentResolver,
				Settings.Global.ADB_ENABLED,
				0
			) == 1
		} catch (_: Exception) {
			false
		}

		// Observe USB state via sticky broadcast and runtime receiver
		val usbFilter =
			android.content.IntentFilter("android.hardware.usb.action.USB_STATE")
		val sticky = context.registerReceiver(null, usbFilter)
		fun parseUsbIntent(intent: Intent?): Boolean {
			if (intent == null) return false
			val extras = intent.extras ?: return false
			val connected = extras.getBoolean("connected", false)
			val configured = extras.getBoolean("configured", false)
			val dataConnected = extras.getBoolean("data_connected", false)
			val adb = extras.getBoolean("adb", false)
			val hostConnected = extras.getBoolean("host_connected", false)
			return connected && (configured || dataConnected || adb || hostConnected)
		}
		isUsbConnected = parseUsbIntent(sticky)
		var previousUsbConnected = isUsbConnected

		val runtimeReceiver = object : android.content.BroadcastReceiver() {
			override fun onReceive(
				ctx: android.content.Context?,
				intent: Intent?
			) {
				val nowConnected = parseUsbIntent(intent)
				if (!previousUsbConnected && nowConnected) {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
				}
				isUsbConnected = nowConnected
				previousUsbConnected = nowConnected
			}
		}
		context.registerReceiver(runtimeReceiver, usbFilter)

		try {
			// Fallback: check attached USB devices via UsbManager
			val usbManager =
				context.getSystemService(android.content.Context.USB_SERVICE) as? android.hardware.usb.UsbManager
			val nowConnected = (usbManager?.deviceList?.isNotEmpty() == true)
			if (!previousUsbConnected && nowConnected) {
				haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
			}
			isUsbConnected = isUsbConnected || nowConnected
			previousUsbConnected = isUsbConnected
		} catch (_: Exception) {
			// ignore
		}

		try {
			while (true) {
				isDeveloperOptionsEnabled = try {
					Settings.Global.getInt(
						context.contentResolver,
						Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
						0
					) == 1
				} catch (_: Exception) {
					false
				}

				isUsbDebuggingEnabled = try {
					Settings.Global.getInt(
						context.contentResolver,
						Settings.Global.ADB_ENABLED,
						0
					) == 1
				} catch (_: Exception) {
					false
				}

				hasPermission = ContextCompat.checkSelfPermission(
					context, Manifest.permission.WRITE_SECURE_SETTINGS
				) == PackageManager.PERMISSION_GRANTED

				if (!previousDevOptionsState && isDeveloperOptionsEnabled) {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
				}

				if (!previousUsbDebuggingState && isUsbDebuggingEnabled) {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
				}

				previousDevOptionsState = isDeveloperOptionsEnabled
				previousUsbDebuggingState = isUsbDebuggingEnabled

				// Fallback refresh: if sticky broadcast wasn’t conclusive, re-check UsbManager
				if (!isUsbConnected) {
					val usbManager =
						context.getSystemService(android.content.Context.USB_SERVICE) as? android.hardware.usb.UsbManager
					val nowConnected = usbManager?.deviceList?.isNotEmpty() == true
					if (!previousUsbConnected && nowConnected) {
						haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					}
					isUsbConnected = nowConnected
					previousUsbConnected = nowConnected
				}

				// If permission becomes granted, auto-complete wizard and enable service
				if (hasPermission) {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					viewModel.completePermissionWizardAndEnableService()
					break
				}

				// Check every second
				kotlinx.coroutines.delay(1000)
			}
		} finally {
			context.unregisterReceiver(runtimeReceiver)
		}
	}

	val adbCommand by viewModel.pendingAdbCommand.collectAsState()

	PermissionSetupWizardScreen(
		step = internalUiState.permissionWizardStep,
		adbCommand = adbCommand,
		isUsbConnected = isUsbConnected,
		hasWriteSecureSettings = hasPermission,
		isDeveloperOptionsEnabled = isDeveloperOptionsEnabled,
		isUsbDebuggingEnabled = isUsbDebuggingEnabled,
		isShizukuInstalled = isShizukuInstalled,
		onGrantViaShizuku = {
			// Trigger the ViewModel’s Shizuku-based grant flow
			viewModel.onGrantViaShizukuRequested(context.packageName)
		},
		onNext = {
			haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
			when (internalUiState.permissionWizardStep) {
				PermissionWizardStep.ENABLE_DEVELOPER_MODE -> {
					AnalyticsLogger.logSetupStepOneCompleted(context)
					viewModel.goToNextPermissionWizardStep()
				}

				PermissionWizardStep.CONNECT_USB -> {
					AnalyticsLogger.logSetupStepTwoCompleted(context)
					viewModel.goToNextPermissionWizardStep()
				}

				PermissionWizardStep.GRANT_PERMISSION -> {
					if (hasPermission) {
						val source =
							if (isUsbConnected) "permission_wizard_complete_usb" else "permission_wizard_complete"
						AnalyticsLogger.logSetupFinished(context, source = source)
						viewModel.completePermissionWizardAndEnableService()
					} else {
						viewModel.goToNextPermissionWizardStep()
					}
				}
			}
		},
		onExit = {
			viewModel.dismissPermissionWizard()
		},
		onOpenSettings = {
			val intent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
			try {
				context.startActivity(intent)
			} catch (_: Exception) {
				context.startActivity(Intent(Settings.ACTION_SETTINGS))
			}
		},
		onOpenDeveloperSettings = {
			val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
			try {
				context.startActivity(intent)
			} catch (_: Exception) {
				context.startActivity(Intent(Settings.ACTION_SETTINGS))
			}
		},
		onShareSetupUrl = {
			AnalyticsLogger.logShareLinkClicked(context, "permission_wizard")
			context.shareSetupUrl("https://lexip.dev/setup")
		},
		onCopyAdbCommand = { viewModel.requestCopyAdbCommand() },
		onShareExpertCommand = {
			context.shareSetupUrl(adbCommand)
		},
		onCheckPermission = {
			val nowGranted =
				ContextCompat.checkSelfPermission(
					context, Manifest.permission.WRITE_SECURE_SETTINGS
				) == PackageManager.PERMISSION_GRANTED
			viewModel.recheckWriteSecureSettingsPermission(nowGranted)
			if (nowGranted) {
				haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
				val source =
					if (isUsbConnected) "permission_wizard_check_now_granted_usb" else "permission_wizard_check_now_granted"
				AnalyticsLogger.logSetupFinished(context, source = source)
				viewModel.completePermissionWizardAndEnableService()
			}
		}
	)
}
