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
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.lexip.hecate.R
import dev.lexip.hecate.data.AdaptiveThreshold
import dev.lexip.hecate.ui.components.MainSwitchPreferenceCard
import dev.lexip.hecate.ui.components.SetupRequiredCard
import dev.lexip.hecate.ui.components.ThreeDotMenu
import dev.lexip.hecate.ui.components.preferences.CustomThresholdDialog
import dev.lexip.hecate.ui.components.preferences.DetailPreferenceCard
import dev.lexip.hecate.ui.components.preferences.ProgressDetailCard
import dev.lexip.hecate.ui.components.preferences.SliderDetailCard
import dev.lexip.hecate.ui.components.preferences.TimePickerPreferenceDialog
import dev.lexip.hecate.ui.theme.hecateTopAppBarColors
import dev.lexip.hecate.util.InAppReviewHandler
import dev.lexip.hecate.util.shizuku.ShizukuAvailability
import java.util.Calendar

private val ScreenHorizontalMargin = 20.dp
private val horizontalOffsetPadding = 8.dp

private fun formatMinutesAsLocalTime(context: android.content.Context, totalMinutes: Int): String {
	val formatter = DateFormat.getTimeFormat(context)
	val calendar = Calendar.getInstance().apply {
		set(Calendar.HOUR_OF_DAY, totalMinutes / 60)
		set(Calendar.MINUTE, totalMinutes % 60)
		set(Calendar.SECOND, 0)
		set(Calendar.MILLISECOND, 0)
	}
	return formatter.format(calendar.time)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
	uiState: MainUiState,
	mainViewModel: MainViewModel
) {
	val context = LocalContext.current
	val haptic = LocalHapticFeedback.current
	val packageName = context.packageName
	val contentScrollState = rememberScrollState()
	var isLargeTitleVisible by remember { mutableStateOf(true) }

	val internalUiState by mainViewModel.uiState.collectAsState()

	val isSystemDark = isSystemInDarkTheme()

	LaunchedEffect(Unit) {
		val installed = ShizukuAvailability.isShizukuInstalled(context)
		mainViewModel.setShizukuInstalled(installed)
	}

	val showCustomDialog = remember { mutableStateOf(false) }
	val showNightStartPicker = remember { mutableStateOf(false) }
	val showNightEndPicker = remember { mutableStateOf(false) }
	var isAdvancedSettingsExpanded by remember { mutableStateOf(uiState.stayDarkAtNightEnabled) }
	val setupShakeKey = remember { mutableIntStateOf(0) }
	val textShakeKey = remember { mutableIntStateOf(0) }

	// Text shake animation for the conditions text (shown when the threshold changes)
	val textOffsetAnim = remember { Animatable(0f) }
	LaunchedEffect(textShakeKey.intValue) {
		if (textShakeKey.intValue > 0) {
			val offsets = listOf(-3f, 3f, -2f, 2f, -1f, 1f, -0.5f, 0.5f, 0f)
			for (o in offsets) {
				textOffsetAnim.animateTo(o, animationSpec = tween(durationMillis = 80))
			}
		}
	}

	LaunchedEffect(mainViewModel) {
		mainViewModel.uiEvents.collect { event ->
			when (event) {
				is CopyToClipboard -> {
					val clipboard = context.getSystemService(ClipboardManager::class.java)
					val clip = ClipData.newPlainText("ADB Command", event.text)
					clipboard?.setPrimaryClip(clip)
				}

				is NavigateToSetup -> {
					// Handled by MainActivity
				}

				is RequestInAppReview -> {
					val activity = context as? Activity
					if (activity != null) {
						InAppReviewHandler.triggerReview(activity)
					}
				}
			}
		}
	}

	Scaffold(
		modifier = Modifier
			.fillMaxSize(),
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
		topBar = {
			val showCollapsedTitle = !isLargeTitleVisible
			TopAppBar(
				modifier = Modifier
					.padding(start = ScreenHorizontalMargin - 8.dp)
					.padding(top = 12.dp),
				colors = hecateTopAppBarColors(),
				title = {
					AnimatedVisibility(
						visible = showCollapsedTitle,
						enter = fadeIn(animationSpec = tween(180)) +
								slideInHorizontally(
									initialOffsetX = { fullWidth -> -fullWidth / 2 },
									animationSpec = tween(220)
								),
						exit = fadeOut(animationSpec = tween(120)) +
								slideOutHorizontally(
									targetOffsetX = { fullWidth -> -fullWidth / 3 },
									animationSpec = tween(160)
								)
					) {
						Text(
							text = stringResource(id = R.string.app_name),
							style = MaterialTheme.typography.titleLarge
						)
					}
				},
				actions = {
					ThreeDotMenu(
						isAdaptiveThemeEnabled = uiState.adaptiveThemeEnabled,
						packageName = packageName,
						isInstalledFromPlayStore = uiState.isInstalledFromPlayStore,
						onShowCustomThresholdDialog = { showCustomDialog.value = true }
					)
				}
			)
		}
	) { innerPadding ->
		val hasWriteSecureSettingsPermission = ContextCompat.checkSelfPermission(
			context,
			Manifest.permission.WRITE_SECURE_SETTINGS
		) == PackageManager.PERMISSION_GRANTED

		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.padding(horizontal = ScreenHorizontalMargin)
				.verticalScroll(contentScrollState),
			verticalArrangement = Arrangement.spacedBy(28.dp)

		) {
			val showBatterySaverWarning =
				internalUiState.isBatterySaverActive && uiState.adaptiveThemeEnabled

			Text(
				modifier = Modifier
					.padding(horizontal = horizontalOffsetPadding)
					.padding(top = 12.dp)
					.onGloballyPositioned { coordinates ->
						isLargeTitleVisible = coordinates.boundsInWindow().bottom > 0f
					},
				text = stringResource(id = R.string.app_name),
				style = MaterialTheme.typography.displaySmall
			)

			Column {
				Text(
					modifier = Modifier.padding(horizontal = horizontalOffsetPadding),
					text = stringResource(id = R.string.description_service_purpose),
					style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 21.sp)
				)
				Spacer(modifier = Modifier.padding(top = 8.dp))
				Text(
					modifier = Modifier
						.padding(horizontal = horizontalOffsetPadding)
						.offset { IntOffset(textOffsetAnim.value.dp.roundToPx(), 0) },
					text = stringResource(id = R.string.description_switching_conditions),
					style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 21.sp)
				)
			}

			// Battery saver warning
			AnimatedVisibility(
				visible = showBatterySaverWarning,
				enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
				exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
			) {
				Card(
					modifier = Modifier
						.fillMaxWidth(),
					colors = CardDefaults.cardColors(
						containerColor = MaterialTheme.colorScheme.tertiaryContainer,
						contentColor = MaterialTheme.colorScheme.onTertiaryContainer
					),
					shape = RoundedCornerShape(20.dp)
				) {
					Column(modifier = Modifier.padding(16.dp)) {
						Text(
							text = stringResource(id = R.string.battery_saver_title),
							style = MaterialTheme.typography.titleMedium
						)
						Spacer(modifier = Modifier.padding(top = 4.dp))
						Text(
							text = stringResource(id = R.string.battery_saver_message),
							style = MaterialTheme.typography.bodyMedium
						)
					}
				}
			}

			// Device-covered warning when the proximity sensor reports covered
			AnimatedVisibility(
				visible = internalUiState.isDeviceCovered && uiState.adaptiveThemeEnabled && !showBatterySaverWarning,
				enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
				exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
			) {
				Card(
					modifier = Modifier
						.fillMaxWidth(),
					colors = CardDefaults.cardColors(
						containerColor = MaterialTheme.colorScheme.errorContainer,
						contentColor = MaterialTheme.colorScheme.onErrorContainer
					),
					shape = RoundedCornerShape(20.dp)
				) {
					Column(modifier = Modifier.padding(16.dp)) {
						Text(
							text = stringResource(id = R.string.device_covered_title),
							style = MaterialTheme.typography.titleMedium
						)
						Spacer(modifier = Modifier.padding(top = 4.dp))
						Text(
							text = stringResource(id = R.string.device_covered_message),
							style = MaterialTheme.typography.bodyMedium
						)
					}
				}
			}

			// Setup card shown when the required permission has not been granted yet
			if (!hasWriteSecureSettingsPermission) {
				SetupRequiredCard(
					modifier = Modifier.fillMaxWidth(),
					title = stringResource(id = R.string.setup_required_title),
					message = stringResource(
						id = R.string.setup_required_message,
						stringResource(id = R.string.app_name)
					),
					onLaunchSetup = {
						mainViewModel.onServiceToggleRequested(
							checked = true,
							hasPermission = false
						)
					},
					shakeKey = setupShakeKey.intValue,
				)
			}

			MainSwitchPreferenceCard(
				text = stringResource(
					id = R.string.action_use_adaptive_theme,
					stringResource(id = R.string.app_name)
				),
				isChecked = uiState.adaptiveThemeEnabled,
				onCheckedChange = { checked ->
					// Shake animation when user tries to enable service without permission
					if (checked && !hasWriteSecureSettingsPermission) {
						setupShakeKey.intValue += 1
						haptic.performHapticFeedback(HapticFeedbackType.Reject)
					} else {
						mainViewModel.onServiceToggleRequested(
							checked,
							hasWriteSecureSettingsPermission
						).also { wasToggled ->
							if (wasToggled)
								haptic.performHapticFeedback(
									if (checked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
								)
							else
								haptic.performHapticFeedback(HapticFeedbackType.Reject)
						}
					}

				}
			)

			val customLabel = stringResource(id = R.string.adaptive_threshold_custom)
			val labels = mainViewModel.getDisplayLabels(
				AdaptiveThreshold.entries.map { stringResource(id = it.labelRes) },
				customLabel
			)
			val baseLux = AdaptiveThreshold.entries.map { it.lux }
			val lux = mainViewModel.getDisplayLuxSteps(baseLux)
			val currentLux by mainViewModel.currentSensorLuxFlow.collectAsState(initial = mainViewModel.currentSensorLux)

			Column(
				verticalArrangement = Arrangement.spacedBy(2.dp)
			) {
				SliderDetailCard(
					title = stringResource(id = R.string.title_brightness_threshold),
					valueIndex = mainViewModel.getIndexForCurrentLux(),
					steps = labels.size,
					labels = labels,
					lux = lux,
					onValueChange = { index ->
						mainViewModel.setPendingCustomSliderLux(lux[index])
						mainViewModel.onSliderValueCommitted(index)

						// Shake the description text when the user could expect an immediate theme switch
						if ((currentLux > lux[index]) == isSystemDark) {
							textShakeKey.intValue += 1
						}
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
					lastCard = !isAdvancedSettingsExpanded
				)

				AnimatedVisibility(
					visible = !isAdvancedSettingsExpanded,
					enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(220)),
					exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(180))
				) {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(top = 8.dp),
						horizontalArrangement = Arrangement.Center
					) {
						AssistChip(
							onClick = {
								isAdvancedSettingsExpanded = true
							},
							enabled = uiState.adaptiveThemeEnabled,
							shape = RoundedCornerShape(20.dp),
							label = {
								Text(text = stringResource(id = R.string.action_advanced_settings))
							},
							leadingIcon = {
								Icon(
									imageVector = Icons.Filled.KeyboardArrowDown,
									contentDescription = null
								)
							}
						)
					}
				}

				AnimatedVisibility(
					visible = isAdvancedSettingsExpanded,
					enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(260)),
					exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(200))
				) {
					Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
						DetailPreferenceCard(
							title = stringResource(id = R.string.title_night_dark_lock),
							enabled = uiState.adaptiveThemeEnabled,
							firstCard = false,
							lastCard = true,
							toggleableValue = uiState.stayDarkAtNightEnabled,
							onToggle = { enabled ->
								mainViewModel.updateStayDarkAtNightEnabled(enabled)
							}
						) {
							Row(
								modifier = Modifier
									.fillMaxWidth(),
								verticalAlignment = Alignment.Top,
								horizontalArrangement = Arrangement.SpaceBetween
							) {
								Text(
									text = stringResource(id = R.string.description_night_dark_lock),
									style = MaterialTheme.typography.bodyMedium,
									modifier = Modifier.weight(1f)
								)
								Switch(
									modifier = Modifier
										.padding(start = 14.dp, end = 4.dp)
										.offset(y = (-6).dp)
										.align(Alignment.Top),
									checked = uiState.stayDarkAtNightEnabled,
									enabled = uiState.adaptiveThemeEnabled,
									onCheckedChange = null,
									thumbContent = if (uiState.stayDarkAtNightEnabled) {
										{
											Icon(
												imageVector = Icons.Filled.Check,
												contentDescription = null,
												modifier = Modifier.size(SwitchDefaults.IconSize)
											)
										}
									} else {
										{
											Icon(
												imageVector = Icons.Filled.Clear,
												contentDescription = null,
												modifier = Modifier.size(SwitchDefaults.IconSize)
											)
										}
									}
								)
							}

							if (uiState.stayDarkAtNightEnabled && uiState.adaptiveThemeEnabled) {
								val startText = formatMinutesAsLocalTime(context, uiState.nightStartMinutes)
								val endText = formatMinutesAsLocalTime(context, uiState.nightEndMinutes)

								Row(
									modifier = Modifier.fillMaxWidth(),
									horizontalArrangement = Arrangement.spacedBy(8.dp)
								) {
									OutlinedButton(
										modifier = Modifier.weight(1f),
										onClick = { showNightStartPicker.value = true }
									) {
										Text(
											text = stringResource(
												id = R.string.action_night_from_time,
												startText
											)
										)
									}

									OutlinedButton(
										modifier = Modifier.weight(1f),
										onClick = { showNightEndPicker.value = true }
									) {
										Text(
											text = stringResource(
												id = R.string.action_night_to_time,
												endText
											)
										)
									}
								}
							}
						}

						AssistChip(
							modifier = Modifier.align(Alignment.CenterHorizontally),
							onClick = {
								isAdvancedSettingsExpanded = false
							},
							enabled = uiState.adaptiveThemeEnabled,
							shape = RoundedCornerShape(20.dp),
							label = {
								Text(text = stringResource(id = R.string.action_collapse))
							},
							leadingIcon = {
								Icon(
									imageVector = Icons.Filled.KeyboardArrowUp,
									contentDescription = null
								)
							}
						)
					}
				}

			}

			Spacer(modifier = Modifier.padding(bottom = 4.dp))
		}
	}


	CustomThresholdDialog(
		show = showCustomDialog.value,
		currentLux = uiState.customAdaptiveThemeThresholdLux ?: uiState.adaptiveThemeThresholdLux,
		onConfirm = { luxValue: Float ->
			mainViewModel.setCustomAdaptiveThemeThreshold(luxValue)
			showCustomDialog.value = false
			if (luxValue.toInt() == 42) {
				Toast.makeText(
					context,
					"The answer to the ultimate question of life, the universe, and everything.",
					Toast.LENGTH_LONG
				).show()
			}
		},
		onDismiss = { showCustomDialog.value = false }
	)

	TimePickerPreferenceDialog(
		show = showNightStartPicker.value,
		title = stringResource(id = R.string.title_night_start_time_picker),
		initialMinutes = uiState.nightStartMinutes,
		onConfirm = { selectedMinutes ->
			mainViewModel.updateNightWindow(
				startMinutes = selectedMinutes,
				endMinutes = uiState.nightEndMinutes,
				onRejected = {
					Toast.makeText(context, R.string.error_invalid_night_period, Toast.LENGTH_SHORT)
						.show()
				}
			)
			showNightStartPicker.value = false
		},
		onDismiss = { showNightStartPicker.value = false }
	)

	TimePickerPreferenceDialog(
		show = showNightEndPicker.value,
		title = stringResource(id = R.string.title_night_end_time_picker),
		initialMinutes = uiState.nightEndMinutes,
		onConfirm = { selectedMinutes ->
			mainViewModel.updateNightWindow(
				startMinutes = uiState.nightStartMinutes,
				endMinutes = selectedMinutes,
				onRejected = {
					Toast.makeText(context, R.string.error_invalid_night_period, Toast.LENGTH_SHORT)
						.show()
				}
			)
			showNightEndPicker.value = false
		},
		onDismiss = { showNightEndPicker.value = false }
	)
}
