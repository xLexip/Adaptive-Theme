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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.lexip.hecate.R
import dev.lexip.hecate.data.AdaptiveThreshold
import dev.lexip.hecate.ui.components.MainSwitchPreferenceCard
import dev.lexip.hecate.ui.components.PermissionMissingDialog
import dev.lexip.hecate.ui.components.preferences.ProgressDetailCard
import dev.lexip.hecate.ui.components.preferences.SliderDetailCard
import dev.lexip.hecate.ui.theme.hecateTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveThemeScreen(
	uiState: AdaptiveThemeUiState
) {
	val scrollBehavior =
		TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
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

	val showMissingPermissionDialog by adaptiveThemeViewModel.showMissingPermissionDialog.collectAsState()
	val pendingAdbCommand by adaptiveThemeViewModel.pendingAdbCommand.collectAsState()

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
			.nestedScroll(scrollBehavior.nestedScrollConnection),
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
		topBar = {
			LargeTopAppBar(
				modifier = Modifier
					.padding(horizontal = horizontalOffsetPadding)
					.padding(top = 22.dp, bottom = 12.dp),
				colors = hecateTopAppBarColors(),
				title = {
					Text(
						text = stringResource(id = R.string.app_name),
						style = MaterialTheme.typography.displaySmall,
						fontWeight = FontWeight.Medium
					)
				},
				scrollBehavior = scrollBehavior
			)
		}
	) { innerPadding ->
		Column(
			modifier = Modifier
				.windowInsetsPadding(WindowInsets.systemGestures.only(WindowInsetsSides.Horizontal))
				.fillMaxSize()
				.padding(innerPadding)
				.verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(32.dp)

		) {
			Text(
				modifier = Modifier.padding(horizontal = horizontalOffsetPadding),
				text = stringResource(id = R.string.description_adaptive_theme),
				style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp)
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

			val currentIndex = adaptiveThemeViewModel.getIndexForCurrentLux()
			val labels = AdaptiveThreshold.entries.map { stringResource(id = it.labelRes) }
			val lux = AdaptiveThreshold.entries.map { it.lux }
			val currentLux by adaptiveThemeViewModel.currentSensorLuxFlow.collectAsState(initial = adaptiveThemeViewModel.currentSensorLux)

			Column(
				verticalArrangement = Arrangement.spacedBy(2.dp)
			) {
				SliderDetailCard(
					title = stringResource(id = R.string.title_brightness_threshold),
					valueIndex = currentIndex,
					steps = labels.size,
					labels = labels,
					lux = lux,
					onValueChange = { index ->
						adaptiveThemeViewModel.updateAdaptiveThemeThresholdByIndex(
							index
						)
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

	PermissionMissingDialog(
		show = showMissingPermissionDialog,
		adbCommand = pendingAdbCommand,
		onCopy = { adaptiveThemeViewModel.requestCopyAdbCommand() },
		onDismiss = { adaptiveThemeViewModel.dismissDialog() }
	)
}