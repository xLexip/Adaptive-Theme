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

package dev.lexip.hecate.ui.setup.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.R
import dev.lexip.hecate.ui.setup.components.ForExpertsSectionCard
import dev.lexip.hecate.ui.setup.components.SetupFAQCards
import dev.lexip.hecate.ui.setup.components.SetupWaitingCard
import dev.lexip.hecate.ui.setup.components.ShizukuOptionCard
import dev.lexip.hecate.ui.setup.components.StepNavigationRow
import dev.lexip.hecate.ui.setup.components.rememberPulseScale

@Composable
internal fun B_ConnectUsbStep(
	isUsbConnected: Boolean,
	isShizukuInstalled: Boolean,
	onGrantViaShizuku: () -> Unit,
	onNext: () -> Unit,
	onExit: () -> Unit,
	onShareExpertCommand: (() -> Unit)? = null,
	onUseRoot: (() -> Unit)? = null,
	onInstallShizuku: (() -> Unit)? = null,
) {
	val haptic = LocalHapticFeedback.current

	LaunchedEffect(isUsbConnected) {
		if (isUsbConnected) {
			haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
		}
	}

	LaunchedEffect(isUsbConnected) {
		if (isUsbConnected) {
			onNext()
		}
	}

	val pulseScale = rememberPulseScale(isActive = !isUsbConnected)

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
				title = stringResource(id = R.string.setup_usb_not_connected),
				pulseScale = pulseScale
			)

			ShizukuOptionCard(
				isVisible = isShizukuInstalled,
				onClick = onGrantViaShizuku
			)

			SetupFAQCards()

			ForExpertsSectionCard(
				onUseRoot = onUseRoot,
				onShareADBCommand = onShareExpertCommand,
				isShizukuInstalled = isShizukuInstalled,
				onInstallShizuku = onInstallShizuku
			)
		}

		StepNavigationRow(
			leftTextRes = R.string.action_close,
			onLeft = onExit,
			rightTextRes = R.string.action_skip,
			onRight = {
				haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
				onNext()
			},
			rightEnabled = true,
			rightIsPrimary = false
		)
	}
}
