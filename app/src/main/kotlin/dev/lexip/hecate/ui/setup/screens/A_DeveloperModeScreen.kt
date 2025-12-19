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

package dev.lexip.hecate.ui.setup.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.R
import dev.lexip.hecate.ui.setup.SetupUiState
import dev.lexip.hecate.ui.setup.components.ShizukuOptionCard
import dev.lexip.hecate.ui.setup.components.StepNavigationRow

data class DeveloperModeActions(
	val onOpenSettings: () -> Unit,
	val onOpenDeveloperSettings: () -> Unit,
)

data class ActionConfig(
	val labelRes: Int,
	val toastRes: Int,
	val onAction: () -> Unit,
	val enabled: Boolean = true,
)

@Composable
fun A_DeveloperModeScreen(
	uiState: SetupUiState,
	onGrantViaShizuku: () -> Unit,
	onNext: () -> Unit,
	onExit: () -> Unit,
	onOpenSettings: () -> Unit,
	onOpenDeveloperSettings: () -> Unit,
) {
	val haptic = LocalHapticFeedback.current
	val stepComplete = uiState.isDeveloperOptionsEnabled && uiState.isUsbDebuggingEnabled


	// Haptic feedback when developer options get enabled
	val prevDevOptionsEnabled =
		remember { androidx.compose.runtime.mutableStateOf(uiState.isDeveloperOptionsEnabled) }
	LaunchedEffect(uiState.isDeveloperOptionsEnabled) {
		if (uiState.isDeveloperOptionsEnabled && !prevDevOptionsEnabled.value) {
			haptic.performHapticFeedback(HapticFeedbackType.Confirm)
		}
		prevDevOptionsEnabled.value = uiState.isDeveloperOptionsEnabled
	}

	// Haptic feedback when usb debugging gets enabled
	val prevUsbDebuggingEnabled =
		remember { androidx.compose.runtime.mutableStateOf(uiState.isUsbDebuggingEnabled) }
	LaunchedEffect(uiState.isUsbDebuggingEnabled) {
		if (uiState.isUsbDebuggingEnabled && !prevUsbDebuggingEnabled.value) {
			haptic.performHapticFeedback(HapticFeedbackType.Confirm)
		}
		prevUsbDebuggingEnabled.value = uiState.isUsbDebuggingEnabled
	}

	SetupScreenScaffold(
		currentStepIndex = 0,
		totalSteps = 3
	) {
		Column(modifier = Modifier.fillMaxSize()) {
			Column(
				modifier = Modifier
					.weight(1f)
					.verticalScroll(rememberScrollState()),
				verticalArrangement = Arrangement.spacedBy(24.dp)
			) {
				Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
					Text(
						text = stringResource(id = R.string.setup_developer_mode_title),
						style = MaterialTheme.typography.headlineMedium,
						fontWeight = FontWeight.Bold
					)
					Text(
						text = stringResource(id = R.string.setup_developer_mode_body),
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}

				DeveloperOptionsCard(
					isEnabled = uiState.isDeveloperOptionsEnabled,
					onOpenSettings = {
						haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
						onOpenSettings()
					}
				)

				UsbDebuggingCard(
					isEnabled = uiState.isUsbDebuggingEnabled,
					isDeveloperOptionsEnabled = uiState.isDeveloperOptionsEnabled,
					onOpenDeveloperSettings = {
						haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
						onOpenDeveloperSettings()
					}
				)
				if (!uiState.isDeveloperOptionsEnabled || !uiState.isUsbDebuggingEnabled)
					ShizukuOptionCard(
						isVisible = uiState.isShizukuInstalled,
						onClick = onGrantViaShizuku
					)
			}

			// Step 1: Close button (left) + Next button (right, enabled when step complete)
			StepNavigationRow(
				leftTextRes = R.string.action_close,
				onLeft = onExit,
				rightTextRes = R.string.action_continue,
				onRight = {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					onNext()
				},
				rightEnabled = stepComplete,
				rightIsPrimary = true
			)
		}
	}
}

@Composable
private fun StatusCard(
	isEnabled: Boolean,
	titleResIfEnabled: Int,
	titleResIfDisabled: Int,
	showAction: Boolean,
	actionConfig: ActionConfig? = null,
) {
	val context = LocalContext.current

	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = if (isEnabled)
				MaterialTheme.colorScheme.primaryContainer
			else
				MaterialTheme.colorScheme.surface
		)
	) {
		Column(modifier = Modifier.padding(20.dp)) {
			StatusCardHeader(isEnabled, titleResIfEnabled, titleResIfDisabled)

			if (showAction && actionConfig != null) {
				Spacer(modifier = Modifier.height(12.dp))
				val toastText = stringResource(actionConfig.toastRes)
				Button(
					onClick = {
						actionConfig.onAction()
						Toast.makeText(
							context,
							toastText,
							Toast.LENGTH_LONG
						).show()
					},
					modifier = Modifier.fillMaxWidth(),
					enabled = actionConfig.enabled
				) {
					Text(text = stringResource(id = actionConfig.labelRes))
				}
			}
		}
	}
}

@Composable
private fun StatusCardHeader(
	isEnabled: Boolean,
	titleResIfEnabled: Int,
	titleResIfDisabled: Int,
) {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = Modifier.fillMaxWidth()
	) {
		if (isEnabled) {
			Icon(
				imageVector = Icons.Filled.CheckCircle,
				contentDescription = null,
				modifier = Modifier.size(32.dp),
				tint = MaterialTheme.colorScheme.primary
			)
			Spacer(modifier = Modifier.width(16.dp))
		}
		Text(
			text = stringResource(
				id = if (isEnabled) titleResIfEnabled else titleResIfDisabled
			),
			style = MaterialTheme.typography.titleMedium,
			fontWeight = FontWeight.Bold,
			color = if (isEnabled)
				MaterialTheme.colorScheme.onPrimaryContainer
			else
				MaterialTheme.colorScheme.onSurface,
			modifier = Modifier.weight(1f)
		)
	}
}

@Composable
private fun DeveloperOptionsCard(
	isEnabled: Boolean,
	onOpenSettings: () -> Unit,
) {
	StatusCard(
		isEnabled = isEnabled,
		titleResIfEnabled = R.string.setup_developer_options_enabled,
		titleResIfDisabled = R.string.setup_developer_options_unlock,
		showAction = !isEnabled,
		actionConfig = ActionConfig(
			labelRes = R.string.setup_action_open_settings,
			toastRes = R.string.setup_dev_options_toast,
			onAction = onOpenSettings,
			enabled = true
		)
	)
}

@Composable
private fun UsbDebuggingCard(
	isEnabled: Boolean,
	isDeveloperOptionsEnabled: Boolean,
	onOpenDeveloperSettings: () -> Unit,
) {
	StatusCard(
		isEnabled = isEnabled,
		titleResIfEnabled = R.string.setup_usb_debugging_enabled,
		titleResIfDisabled = R.string.setup_usb_debugging_disabled,
		showAction = !isEnabled,
		actionConfig = ActionConfig(
			labelRes = R.string.setup_action_open_developer_settings,
			toastRes = R.string.setup_usb_debugging_toast,
			onAction = onOpenDeveloperSettings,
			enabled = isDeveloperOptionsEnabled
		)
	)
}

