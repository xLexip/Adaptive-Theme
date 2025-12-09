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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.R
import dev.lexip.hecate.ui.setup.steps.ConnectUsbStep
import dev.lexip.hecate.ui.setup.steps.DeveloperModeActions
import dev.lexip.hecate.ui.setup.steps.DeveloperModeStep
import dev.lexip.hecate.ui.setup.steps.GrantPermissionStep

enum class PermissionWizardStep {
	ENABLE_DEVELOPER_MODE,
	CONNECT_USB,
	GRANT_PERMISSION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSetupWizardScreen(
	step: PermissionWizardStep,
	adbCommand: String,
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
	onCopyAdbCommand: () -> Unit,
	onShareExpertCommand: () -> Unit,
	onCheckPermission: () -> Unit,
	onUseRoot: () -> Unit,
) {
	val totalSteps = PermissionWizardStep.entries.size
	val currentStepIndex = step.ordinal + 1
	val progress = (currentStepIndex.toFloat() - (0.1).toFloat()) / totalSteps.toFloat()

	Scaffold(
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
		topBar = {
			TopAppBar(
				title = { Text(text = "Service Setup", fontWeight = FontWeight.Bold) },
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = MaterialTheme.colorScheme.surfaceContainer,
					titleContentColor = MaterialTheme.colorScheme.onSurface
				)
			)
		}
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
				LinearProgressIndicator(
					progress = { progress },
					modifier = Modifier
						.fillMaxWidth()
						.height(8.dp),
				)
				Spacer(modifier = Modifier.height(8.dp))
				Text(
					text = stringResource(
						id = R.string.permission_wizard_step_counter,
						currentStepIndex,
						totalSteps
					),
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.fillMaxWidth(),
					textAlign = TextAlign.Center
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
					PermissionWizardStep.ENABLE_DEVELOPER_MODE -> DeveloperModeStep(
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

					PermissionWizardStep.CONNECT_USB -> ConnectUsbStep(
						isUsbConnected = isUsbConnected,
						isShizukuInstalled = isShizukuInstalled,
						onGrantViaShizuku = onGrantViaShizuku,
						onNext = onNext,
						onExit = onExit,
						onShareExpertCommand = onShareExpertCommand,
						onUseRoot = onUseRoot
					)

					PermissionWizardStep.GRANT_PERMISSION -> GrantPermissionStep(
						hasWriteSecureSettings = hasWriteSecureSettings,
						onShareSetupUrl = onShareSetupUrl,
						onShareExpertCommand = onShareExpertCommand,
						onCheckPermission = onCheckPermission,
						onExit = onExit,
						onUseRoot = onUseRoot,
						isUsbConnected = isUsbConnected
					)
				}
			}
			Spacer(modifier = Modifier.height(8.dp))
		}
	}
}
