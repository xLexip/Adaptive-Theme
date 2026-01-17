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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
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
import dev.lexip.hecate.ui.components.preferences.ProgressDetailCard
import dev.lexip.hecate.ui.components.preferences.SliderDetailCard
import dev.lexip.hecate.ui.theme.hecateTopAppBarColors
import dev.lexip.hecate.util.shizuku.ShizukuAvailability

private val ScreenHorizontalMargin = 20.dp
private val horizontalOffsetPadding = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
	uiState: MainUiState,
	mainViewModel: MainViewModel
) {
	// Enable top-app-bar collapsing on small devices
	val windowInfo = LocalWindowInfo.current
	val density = LocalDensity.current
	val screenHeightDp = with(density) { windowInfo.containerSize.height.toDp().value }
	val enableCollapsing = screenHeightDp < 650f
	val scrollBehavior = if (enableCollapsing) {
		TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
	} else null

	val context = LocalContext.current
	val haptic = LocalHapticFeedback.current
	val packageName = context.packageName

	val internalUiState by mainViewModel.uiState.collectAsState()

	val isSystemDark = isSystemInDarkTheme()

	LaunchedEffect(Unit) {
		val installed = ShizukuAvailability.isShizukuInstalled(context)
		mainViewModel.setShizukuInstalled(installed)
	}

	val showCustomDialog = remember { mutableStateOf(false) }
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
					.padding(top = 12.dp, bottom = 12.dp),
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
					ThreeDotMenu(
						isAdaptiveThemeEnabled = uiState.adaptiveThemeEnabled,
						packageName = packageName,
						isInstalledFromPlayStore = uiState.isInstalledFromPlayStore,
						onShowCustomThresholdDialog = { showCustomDialog.value = true }
					)
				},
				scrollBehavior = scrollBehavior
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
				.verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(28.dp)

		) {
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
					lastCard = true
				)

			}

			// Device-covered warning when the proximity sensor reports covered
			AnimatedVisibility(
				visible = internalUiState.isDeviceCovered && uiState.adaptiveThemeEnabled,
				enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
				exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
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
}
