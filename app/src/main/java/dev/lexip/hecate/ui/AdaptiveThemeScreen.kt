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

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.lexip.hecate.BuildConfig
import dev.lexip.hecate.R
import dev.lexip.hecate.analytics.AnalyticsLogger
import dev.lexip.hecate.data.AdaptiveThreshold
import dev.lexip.hecate.ui.components.MainSwitchPreferenceCard
import dev.lexip.hecate.ui.components.preferences.CustomThresholdDialog
import dev.lexip.hecate.ui.components.preferences.ProgressDetailCard
import dev.lexip.hecate.ui.components.preferences.SliderDetailCard
import dev.lexip.hecate.ui.setup.PermissionSetupWizardScreen
import dev.lexip.hecate.ui.setup.PermissionWizardStep
import dev.lexip.hecate.ui.theme.hecateTopAppBarColors
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Helper to share via Android Sharesheet
private fun android.content.Context.shareSetupUrl(url: String) {
	if (url.isBlank()) return

	val sendIntent = Intent().apply {
		action = Intent.ACTION_SEND
		putExtra(Intent.EXTRA_TEXT, url)
		putExtra(Intent.EXTRA_TITLE, "Setup - Adaptive Theme")
		type = "text/plain"
	}

	val shareIntent = Intent.createChooser(sendIntent, null)
	startActivity(shareIntent)

}

private val ScreenHorizontalMargin = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveThemeScreen(
	uiState: AdaptiveThemeUiState,
	onAboutClick: () -> Unit = {}
) {
	// Enable top-app-bar collapsing on small devices
	val configuration = androidx.compose.ui.platform.LocalConfiguration.current
	val screenHeightDp = configuration.screenHeightDp
	val enableCollapsing = screenHeightDp < 700
	val scrollBehavior = if (enableCollapsing) {
		TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
	} else null

	val horizontalOffsetPadding = 8.dp

	val context = LocalContext.current
	val haptic = LocalHapticFeedback.current
	val packageName = context.packageName

	val adaptiveThemeViewModel: AdaptiveThemeViewModel = viewModel(
		factory = AdaptiveThemeViewModelFactory(
			context.applicationContext as dev.lexip.hecate.HecateApplication,
			dev.lexip.hecate.data.UserPreferencesRepository((context.applicationContext as dev.lexip.hecate.HecateApplication).userPreferencesDataStore),
			dev.lexip.hecate.util.DarkThemeHandler(context)
		)
	)

	val internalUiState by adaptiveThemeViewModel.uiState.collectAsState()

	val showCustomDialog = remember { mutableStateOf(false) }

	LaunchedEffect(adaptiveThemeViewModel) {
		adaptiveThemeViewModel.uiEvents.collect { event ->
			when (event) {
				is UiEvent.CopyToClipboard -> {
					val clipboard = context.getSystemService(ClipboardManager::class.java)
					val clip = ClipData.newPlainText("ADB Command", event.text)
					clipboard?.setPrimaryClip(clip)
				}
			}
		}
	}

	Scaffold(
		modifier = Modifier
			.fillMaxSize()
			.then(
				if (scrollBehavior != null) {
					Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
				} else {
					Modifier
				}
			),
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
		topBar = {
			val collapsedFraction = scrollBehavior?.state?.collapsedFraction ?: 0f
			LargeTopAppBar(
				modifier = Modifier
					.padding(start = ScreenHorizontalMargin - 8.dp)
					.padding(top = 22.dp, bottom = 12.dp),
				colors = hecateTopAppBarColors(),
				title = {
					Text(
						text = stringResource(id = R.string.app_name),
						style = if (collapsedFraction > 0.4f) {
							MaterialTheme.typography.titleLarge
						} else {
							MaterialTheme.typography.displaySmall
						}
					)
				},
				actions = {
					stringResource(id = R.string.error_no_email_client)
					var menuExpanded by remember { mutableStateOf(false) }
					androidx.compose.foundation.layout.Box {
						IconButton(onClick = { menuExpanded = true }) {
							Icon(
								imageVector = Icons.Filled.MoreVert,
								contentDescription = stringResource(id = R.string.title_more)
							)
						}
						DropdownMenu(
							expanded = menuExpanded,
							onDismissRequest = { menuExpanded = false }
						) {
							val feedbackSubject =
								"Adaptive Theme Feedback (v${BuildConfig.VERSION_NAME})"

							// 1) Custom Threshold
							DropdownMenuItem(
								text = { Text(text = stringResource(id = R.string.title_custom_threshold)) },
								enabled = uiState.adaptiveThemeEnabled,
								onClick = {
									menuExpanded = false
									AnalyticsLogger.logOverflowMenuItemClicked(
										context,
										"custom_threshold"
									)
									if (uiState.adaptiveThemeEnabled) {
										showCustomDialog.value = true
									}
								}
							)

							// 2) Change Language (Android 13+)
							if (android.os.Build.VERSION.SDK_INT >= 33) {
								DropdownMenuItem(
									text = { Text(text = stringResource(id = R.string.title_change_language)) },
									onClick = {
										menuExpanded = false
										AnalyticsLogger.logOverflowMenuItemClicked(
											context,
											"change_language"
										)
										val intent =
											Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
												data = "package:$packageName".toUri()
											}
										try {
											context.startActivity(intent)
										} catch (_: ActivityNotFoundException) {
											Toast.makeText(
												context,
												context.getString(R.string.error_no_app_language_settings),
												Toast.LENGTH_SHORT
											).show()
										}
									}
								)
							}

							// 3) Send Feedback
							DropdownMenuItem(
								text = { Text(text = stringResource(id = R.string.title_send_feedback)) },
								onClick = {
									menuExpanded = false
									AnalyticsLogger.logOverflowMenuItemClicked(
										context,
										"send_feedback"
									)
									val encodedSubject = URLEncoder.encode(
										feedbackSubject,
										StandardCharsets.UTF_8.toString()
									)
									val feedbackUri =
										"https://lexip.dev/hecate/feedback?subject=$encodedSubject".toUri()
									val feedbackIntent = Intent(Intent.ACTION_VIEW, feedbackUri)
									context.startActivity(feedbackIntent)

								}
							)

							// 3) Beta Feedback (only on beta builds)
							if (BuildConfig.VERSION_NAME.contains("-beta")) {
								DropdownMenuItem(
									text = { Text(text = "Beta Feedback") },
									onClick = {
										menuExpanded = false
										AnalyticsLogger.logOverflowMenuItemClicked(
											context,
											"beta_feedback"
										)
										val betaUri =
											"https://play.google.com/store/apps/details?id=dev.lexip.hecate".toUri()
										val betaIntent = Intent(Intent.ACTION_VIEW, betaUri)
										context.startActivity(betaIntent)
									}
								)
							}

							// 4) About
							DropdownMenuItem(
								text = { Text(stringResource(R.string.title_about)) },
								onClick = {
									menuExpanded = false
									AnalyticsLogger.logOverflowMenuItemClicked(context, "about")
									val aboutUri = "https://lexip.dev/hecate/about".toUri()
									val aboutIntent = Intent(Intent.ACTION_VIEW, aboutUri)
									Toast.makeText(
										context,
										"v${BuildConfig.VERSION_NAME}",
										Toast.LENGTH_SHORT
									).show()
									try {
										context.startActivity(aboutIntent)
									} catch (_: ActivityNotFoundException) {
										context.startActivity(Intent(Intent.ACTION_VIEW, aboutUri))
									}
									onAboutClick()
								}
							)
						}
					}
				},
				scrollBehavior = scrollBehavior
			)
		}
	) { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.padding(horizontal = ScreenHorizontalMargin)
				.verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(32.dp)

		) {
			Text(
				modifier = Modifier.padding(horizontal = horizontalOffsetPadding),
				text = stringResource(id = R.string.description_adaptive_theme),
				style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 21.sp)
			)
			MainSwitchPreferenceCard(
				text = stringResource(id = R.string.action_use_adaptive_theme),
				isChecked = uiState.adaptiveThemeEnabled,
				onCheckedChange = { checked ->
					val hasPermission = ContextCompat.checkSelfPermission(
						context, Manifest.permission.WRITE_SECURE_SETTINGS
					) == PackageManager.PERMISSION_GRANTED

					adaptiveThemeViewModel.onServiceToggleRequested(
						checked,
						hasPermission,
						packageName
					).also { wasToggled ->
						if (wasToggled)
							haptic.performHapticFeedback(
								if (checked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
							)
						else
							haptic.performHapticFeedback(HapticFeedbackType.Reject)
					}

				}
			)

			val customLabel = stringResource(id = R.string.adaptive_threshold_custom)
			val labels = adaptiveThemeViewModel.getDisplayLabels(
				AdaptiveThreshold.entries.map { stringResource(id = it.labelRes) },
				customLabel
			)
			val baseLux = AdaptiveThreshold.entries.map { it.lux }
			val lux = adaptiveThemeViewModel.getDisplayLuxSteps(baseLux)
			val currentLux by adaptiveThemeViewModel.currentSensorLuxFlow.collectAsState(initial = adaptiveThemeViewModel.currentSensorLux)

			Column(
				verticalArrangement = Arrangement.spacedBy(2.dp)
			) {
				SliderDetailCard(
					title = stringResource(id = R.string.title_brightness_threshold),
					valueIndex = adaptiveThemeViewModel.getIndexForCurrentLux(),
					steps = labels.size,
					labels = labels,
					lux = lux,
					onValueChange = { index ->
						adaptiveThemeViewModel.setPendingCustomSliderLux(lux[index])
						adaptiveThemeViewModel.onSliderValueCommitted(index)
					},
					enabled = uiState.adaptiveThemeEnabled,
					firstCard = true,
					lastCard = false
				)

				ProgressDetailCard(
					title = stringResource(id = R.string.title_current_brightness),
					currentLux = currentLux,
					luxSteps = lux,
					enabled = uiState.adaptiveThemeEnabled,
					firstCard = false,
					lastCard = true
				)

			}
		}
	}

	// Show permission wizard if needed
	if (internalUiState.showPermissionWizard) {
		var isDeveloperOptionsEnabled by remember { mutableStateOf(false) }
		var isUsbDebuggingEnabled by remember { mutableStateOf(false) }
		var isUsbConnected by remember { mutableStateOf(false) }
		var hasPermission by remember { mutableStateOf(false) }

		// Periodically check developer settings and permission status
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

			var previousUsbConnected = false

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
			previousUsbConnected = isUsbConnected

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
			} catch (_: Exception) { /* ignore */
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
						Toast.makeText(
							context,
							context.getString(R.string.permission_wizard_dev_options_enabled_toast),
							Toast.LENGTH_LONG
						).show()
						haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					}

					if (!previousUsbDebuggingState && isUsbDebuggingEnabled) {
						Toast.makeText(
							context,
							context.getString(R.string.permission_wizard_usb_debugging_enabled_toast),
							Toast.LENGTH_LONG
						).show()
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
						adaptiveThemeViewModel.completePermissionWizardAndEnableService()
						break
					}

					// Check every second
					kotlinx.coroutines.delay(1000)
				}
			} finally {
				context.unregisterReceiver(runtimeReceiver)
			}
		}

		val adbCommand by adaptiveThemeViewModel.pendingAdbCommand.collectAsState()

		PermissionSetupWizardScreen(
			step = internalUiState.permissionWizardStep,
			adbCommand = adbCommand,
			isUsbConnected = isUsbConnected,
			hasWriteSecureSettings = hasPermission,
			isDeveloperOptionsEnabled = isDeveloperOptionsEnabled,
			isUsbDebuggingEnabled = isUsbDebuggingEnabled,
			onNext = {
				haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
				if (internalUiState.permissionWizardStep == PermissionWizardStep.GRANT_PERMISSION && hasPermission) {
					adaptiveThemeViewModel.completePermissionWizardAndEnableService()
				} else {
					adaptiveThemeViewModel.goToNextPermissionWizardStep()
				}
			},
			onExit = { adaptiveThemeViewModel.dismissPermissionWizard() },
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
			onCopyAdbCommand = { adaptiveThemeViewModel.requestCopyAdbCommand() },
			onShareExpertCommand = {
				context.shareSetupUrl(adbCommand)
			},
			onCheckPermission = {
				val nowGranted =
					ContextCompat.checkSelfPermission(
						context, Manifest.permission.WRITE_SECURE_SETTINGS
					) == PackageManager.PERMISSION_GRANTED
				adaptiveThemeViewModel.recheckWriteSecureSettingsPermission(nowGranted)
				if (nowGranted) {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					adaptiveThemeViewModel.completePermissionWizardAndEnableService()
				}
			}
		)
		return
	}

	CustomThresholdDialog(
		show = showCustomDialog.value,
		currentLux = uiState.customAdaptiveThemeThresholdLux ?: uiState.adaptiveThemeThresholdLux,
		onConfirm = { luxValue: Float ->
			adaptiveThemeViewModel.setCustomAdaptiveThemeThreshold(luxValue)
			showCustomDialog.value = false
		},
		onDismiss = { showCustomDialog.value = false }
	)
}