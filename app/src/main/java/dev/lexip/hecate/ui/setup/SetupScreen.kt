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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.ui.components.SegmentedProgressIndicator
import dev.lexip.hecate.ui.setup.steps.A_DeveloperModeStep
import dev.lexip.hecate.ui.setup.steps.B_ConnectUsbStep
import dev.lexip.hecate.ui.setup.steps.C_GrantPermissionStep
import dev.lexip.hecate.ui.setup.steps.DeveloperModeActions

enum class SetupStep {
	ENABLE_DEVELOPER_MODE,
	CONNECT_USB,
	GRANT_PERMISSION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
	step: SetupStep,
	isUsbConnected: Boolean,
	hasWriteSecureSettings: Boolean,
	isDeveloperOptionsEnabled: Boolean,
	isUsbDebuggingEnabled: Boolean,
	isShizukuInstalled: Boolean,
	onGrantViaShizuku: () -> Unit,
	onNext: () -> Unit,
	onExit: () -> Unit,
	onOpenSettings: () -> Unit,
	onOpenDeveloperSettings: () -> Unit,
	onShareSetupUrl: () -> Unit,
	onShareExpertCommand: () -> Unit,
	onCheckPermission: () -> Unit,
	onUseRoot: () -> Unit,
	onInstallShizuku: () -> Unit,
) {
	val totalSteps = SetupStep.entries.size
	val currentStepIndex = step.ordinal + 1

	Scaffold(
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
	) { paddingValues ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
		) {
			// Progress indicator section
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 24.dp)
					.padding(top = 16.dp)
			) {
				// Segmented progress matches the multi-step setup flow visually
				SegmentedProgressIndicator(
					segments = totalSteps,
					activeIndex = currentStepIndex - 1,
					enabled = true
				)

			}

			Spacer(modifier = Modifier.height(16.dp))

			// Main content
			Column(
				modifier = Modifier
					.weight(1f)
					.fillMaxWidth()
					.padding(horizontal = 24.dp),
				verticalArrangement = Arrangement.SpaceBetween
			) {
				when (step) {
					SetupStep.ENABLE_DEVELOPER_MODE -> A_DeveloperModeStep(
						isDeveloperOptionsEnabled = isDeveloperOptionsEnabled,
						isUsbDebuggingEnabled = isUsbDebuggingEnabled,
						isShizukuInstalled = isShizukuInstalled,
						onGrantViaShizuku = onGrantViaShizuku,
						onNext = onNext,
						onExit = onExit,
						actions = DeveloperModeActions(
							onOpenSettings = onOpenSettings,
							onOpenDeveloperSettings = onOpenDeveloperSettings
						)
					)

					SetupStep.CONNECT_USB -> B_ConnectUsbStep(
						isUsbConnected = isUsbConnected,
						isShizukuInstalled = isShizukuInstalled,
						onGrantViaShizuku = onGrantViaShizuku,
						onNext = onNext,
						onExit = onExit,
						onShareExpertCommand = onShareExpertCommand,
						onUseRoot = onUseRoot,
						onInstallShizuku = onInstallShizuku
					)

					SetupStep.GRANT_PERMISSION -> C_GrantPermissionStep(
						hasWriteSecureSettings = hasWriteSecureSettings,
						onShareSetupUrl = onShareSetupUrl,
						onShareExpertCommand = onShareExpertCommand,
						onCheckPermission = onCheckPermission,
						onExit = onExit,
						onUseRoot = onUseRoot,
						isUsbConnected = isUsbConnected,
						isShizukuInstalled = isShizukuInstalled,
						onInstallShizuku = onInstallShizuku
					)
				}
			}
			Spacer(modifier = Modifier.height(8.dp))
		}
	}
}
