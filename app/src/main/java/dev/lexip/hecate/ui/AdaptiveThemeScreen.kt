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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.lexip.hecate.R
import dev.lexip.hecate.data.AdaptiveThreshold
import dev.lexip.hecate.ui.components.MainSwitchPreferenceCard
import dev.lexip.hecate.ui.components.SetupRequiredCard
import dev.lexip.hecate.ui.components.ThreeDotMenu
import dev.lexip.hecate.ui.components.preferences.CustomThresholdDialog
import dev.lexip.hecate.ui.components.preferences.ProgressDetailCard
import dev.lexip.hecate.ui.components.preferences.SliderDetailCard
import dev.lexip.hecate.ui.setup.PermissionSetupHost
import dev.lexip.hecate.ui.theme.hecateTopAppBarColors
import dev.lexip.hecate.util.shizuku.ShizukuAvailability

private val ScreenHorizontalMargin = 20.dp
private val horizontalOffsetPadding = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveThemeScreen(
	uiState: AdaptiveThemeUiState,
	onAboutClick: () -> Unit = {}
) {
	// Enable top-app-bar collapsing on small devices
	val windowInfo = LocalWindowInfo.current
	val density = LocalDensity.current
	val screenHeightDp = with(density) { windowInfo.containerSize.height.toDp().value }
	val enableCollapsing = screenHeightDp < 700f
	val scrollBehavior = if (enableCollapsing) {
		TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
	} else null

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

	LaunchedEffect(Unit) {
		val installed = ShizukuAvailability.isShizukuInstalled(context)
		adaptiveThemeViewModel.setShizukuInstalled(installed)
	}

	val showCustomDialog = remember { mutableStateOf(false) }
	val setupShakeKey = remember { mutableIntStateOf(0) }

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
						onShowCustomThresholdDialog = { showCustomDialog.value = true },
						onAboutClick = onAboutClick
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
			verticalArrangement = Arrangement.spacedBy(24.dp)

		) {
			Text(
				modifier = Modifier.padding(horizontal = horizontalOffsetPadding),
				text = stringResource(id = R.string.description_adaptive_theme),
				style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 21.sp)
			)

			// Setup card shown when the required permission has not been granted yet
			if (!hasWriteSecureSettingsPermission) {
				SetupRequiredCard(
					modifier = Modifier.fillMaxWidth(),
					title = stringResource(id = R.string.setup_required_title),
					message = stringResource(
						id = R.string.setup_required_message,
						stringResource(id = R.string.app_name)
					),
					onFinishSetupRequested = { adaptiveThemeViewModel.onSetupRequested(packageName) },
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
						adaptiveThemeViewModel.onServiceToggleRequested(
							checked,
							hasWriteSecureSettingsPermission,
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

			// Device-covered warning when the proximity sensor reports covered
			if (internalUiState.isDeviceCovered && uiState.adaptiveThemeEnabled) {
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

	// Show permission wizard if needed
	if (internalUiState.showPermissionWizard) {
		PermissionSetupHost(viewModel = adaptiveThemeViewModel)
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
