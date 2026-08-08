/*
 * Copyright (C) 2024-2026 xLexip <https://lexip.dev>
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import dev.lexip.hecate.R
import dev.lexip.hecate.util.InAppReviewHandler
import dev.lexip.hecate.util.shizuku.ShizukuAvailability

data class MainScreenCallbacks(
	val onServiceToggleRequested: (checked: Boolean, hasPermission: Boolean) -> Boolean,
	val onThresholdSelected: (index: Int, lux: Float) -> Unit,
	val onCheckReviewPrompt: () -> Unit,
	val onStayDarkAtNightChanged: (Boolean) -> Unit,
	val onWallpaperSyncToggleRequested: (Boolean) -> Unit = {},
	val onSelectDayWallpaper: () -> Unit = {},
	val onSelectNightWallpaper: () -> Unit = {},
	val onConfirmLiveWallpaper: () -> Unit = {},
	val onDismissLiveWallpaperWarning: () -> Unit = {},
	val onCustomThresholdConfirmed: (Float) -> Unit,
	val onNightWindowChanged: (
		startMinutes: Int,
		endMinutes: Int,
		onRejected: (() -> Unit)?
	) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
	uiState: MainUiState,
	mainViewModel: MainViewModel
) {
	val context = LocalContext.current
	val adbCommandLabel = stringResource(R.string.setup_action_adb_command)
	val internalUiState by mainViewModel.uiState.collectAsState()
	val currentLux by mainViewModel.currentSensorLuxFlow.collectAsState(
		initial = mainViewModel.currentSensorLux
	)
	val hasWriteSecureSettingsPermission = ContextCompat.checkSelfPermission(
		context,
		Manifest.permission.WRITE_SECURE_SETTINGS
	) == PackageManager.PERMISSION_GRANTED

	LaunchedEffect(Unit) {
		mainViewModel.setShizukuInstalled(ShizukuAvailability.isShizukuInstalled(context))
	}

	val dayWallpaperPicker = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.PickVisualMedia()
	) { uri ->
		if (uri != null) {
			mainViewModel.onDayWallpaperPicked(uri)
		}
	}

	val nightWallpaperPicker = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.PickVisualMedia()
	) { uri ->
		if (uri != null) {
			mainViewModel.onNightWallpaperPicked(uri)
		}
	}

	LaunchedEffect(mainViewModel, adbCommandLabel) {
		mainViewModel.uiEvents.collect { event ->
			when (event) {
				is CopyToClipboard -> {
					val clipboard = context.getSystemService(ClipboardManager::class.java)
					clipboard?.setPrimaryClip(
						ClipData.newPlainText(
							adbCommandLabel,
							event.text
						)
					)
				}

				is NavigateToSetup -> Unit

				is RequestInAppReview -> {
					(context as? Activity)?.let(InAppReviewHandler::triggerReview)
				}
			}
		}
	}

	MainScreenContent(
		uiState = uiState,
		currentSensorLux = currentLux,
		isDeviceCovered = internalUiState.isDeviceCovered,
		isBatterySaverActive = internalUiState.isBatterySaverActive,
		hasWriteSecureSettingsPermission = hasWriteSecureSettingsPermission,
		packageName = context.packageName,
		callbacks = MainScreenCallbacks(
			onServiceToggleRequested = mainViewModel::onServiceToggleRequested,
			onThresholdSelected = { index, lux ->
				mainViewModel.setPendingCustomSliderLux(lux)
				mainViewModel.onSliderValueCommitted(index)
			},
			onCheckReviewPrompt = mainViewModel::checkReviewPrompt,
			onStayDarkAtNightChanged = mainViewModel::updateStayDarkAtNightEnabled,
			onWallpaperSyncToggleRequested = mainViewModel::onWallpaperSyncToggleRequested,
			onSelectDayWallpaper = {
				dayWallpaperPicker.launch(
					PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
				)
			},
			onSelectNightWallpaper = {
				nightWallpaperPicker.launch(
					PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
				)
			},
			onConfirmLiveWallpaper = mainViewModel::confirmEnableWithLiveWallpaper,
			onDismissLiveWallpaperWarning = mainViewModel::dismissLiveWallpaperWarningDialog,
			onCustomThresholdConfirmed = mainViewModel::setCustomAdaptiveThemeThreshold,
			onNightWindowChanged = mainViewModel::updateNightWindow
		)
	)
}
