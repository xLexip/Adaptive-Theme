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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.R
import dev.lexip.hecate.ui.setup.SetupUiState
import dev.lexip.hecate.ui.setup.components.ForExpertsSectionCard
import dev.lexip.hecate.ui.setup.components.SetupFAQCards
import dev.lexip.hecate.ui.setup.components.SetupWaitingCard
import dev.lexip.hecate.ui.setup.components.ShizukuOptionCard
import dev.lexip.hecate.ui.setup.components.StepNavigationRow


@Composable
fun B_ConnectUsbScreen(
	uiState: SetupUiState,
	onGrantViaShizuku: () -> Unit,
	onNext: () -> Unit,
	onBack: () -> Unit,
	onShareExpertCommand: () -> Unit,
	onUseRoot: () -> Unit,
	onInstallShizuku: () -> Unit,
) {
	val haptic = LocalHapticFeedback.current


	// Haptic feedback when USB connected
	val previousUsbConnected = remember { mutableStateOf(uiState.isUsbConnected) }
	LaunchedEffect(uiState.isUsbConnected) {
		if (uiState.isUsbConnected && !previousUsbConnected.value) {
			haptic.performHapticFeedback(HapticFeedbackType.Confirm)
		}
		previousUsbConnected.value = uiState.isUsbConnected
	}


	SetupScreenScaffold(
		currentStepIndex = 1,
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
						text = stringResource(id = R.string.setup_connect_title),
						style = MaterialTheme.typography.headlineMedium,
						fontWeight = FontWeight.Bold
					)
					Text(
						text = stringResource(id = R.string.setup_connect_body),
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}

				SetupWaitingCard(
					title =
						if (uiState.isUsbConnected) stringResource(id = R.string.setup_usb_connected)
						else stringResource(id = R.string.setup_usb_not_connected),
					isWaiting = !uiState.isUsbConnected
				)

				ShizukuOptionCard(
					isVisible = uiState.isShizukuInstalled,
					onClick = onGrantViaShizuku
				)

				SetupFAQCards()

				ForExpertsSectionCard(
					onUseRoot = onUseRoot,
					onShareADBCommand = onShareExpertCommand,
					isShizukuInstalled = uiState.isShizukuInstalled,
					onInstallShizuku = onInstallShizuku
				)
			}

			Spacer(modifier = Modifier.height(8.dp))

			// Step 2: Back button (left) + Skip/Continue button (right, outlined)
			StepNavigationRow(
				leftTextRes = R.string.action_back,
				onLeft = onBack,
				rightTextRes = if (uiState.isUsbConnected) R.string.action_continue else R.string.action_skip,
				onRight = {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					onNext()
				},
				rightEnabled = true,
				rightIsPrimary = uiState.isUsbConnected
			)
		}
	}
}
