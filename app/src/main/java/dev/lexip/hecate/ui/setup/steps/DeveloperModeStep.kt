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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.R
import dev.lexip.hecate.ui.setup.components.ShizukuOptionCard
import dev.lexip.hecate.ui.setup.components.StepNavigationRow

@Composable
internal fun DeveloperModeStep(
	isDeveloperOptionsEnabled: Boolean,
	isUsbDebuggingEnabled: Boolean,
	isShizukuInstalled: Boolean,
	onGrantViaShizuku: () -> Unit,
	onNext: () -> Unit,
	onExit: () -> Unit,
	onOpenSettings: () -> Unit,
	onOpenDeveloperSettings: () -> Unit,
) {
	val haptic = LocalHapticFeedback.current
	val bothEnabled = isDeveloperOptionsEnabled && isUsbDebuggingEnabled

	LaunchedEffect(isDeveloperOptionsEnabled) {
		if (isDeveloperOptionsEnabled) {
			haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
		}
	}

	LaunchedEffect(isUsbDebuggingEnabled) {
		if (isUsbDebuggingEnabled) {
			haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
		}
	}

	LaunchedEffect(bothEnabled) {
		if (bothEnabled) {
			onNext()
		}
	}

	Column(modifier = Modifier.fillMaxSize()) {
		Column(
			modifier = Modifier
				.weight(1f)
				.verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(24.dp)
		) {
			Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				Text(
					text = stringResource(id = R.string.permission_wizard_developer_mode_title),
					style = MaterialTheme.typography.headlineMedium,
					fontWeight = FontWeight.Bold
				)
				Text(
					text = stringResource(id = R.string.permission_wizard_developer_mode_body),
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}

			DeveloperOptionsCard(
				isEnabled = isDeveloperOptionsEnabled,
				onOpenSettings = onOpenSettings
			)

			UsbDebuggingCard(
				isEnabled = isUsbDebuggingEnabled,
				isDeveloperOptionsEnabled = isDeveloperOptionsEnabled,
				onOpenDeveloperSettings = onOpenDeveloperSettings
			)

			ShizukuOptionCard(
				isVisible = isShizukuInstalled,
				onClick = onGrantViaShizuku
			)
		}

		StepNavigationRow(
			leftTextRes = R.string.action_close,
			onLeft = onExit,
			rightTextRes = R.string.action_continue,
			onRight = {
				haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
				onNext()
			},
			rightEnabled = bothEnabled,
			rightIsPrimary = true
		)
	}
}

@Composable
private fun DeveloperOptionsCard(
	isEnabled: Boolean,
	onOpenSettings: () -> Unit,
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
						id = if (isEnabled)
							R.string.permission_wizard_developer_options_enabled
						else
							R.string.permission_wizard_developer_options_title
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
			if (!isEnabled) {
				Spacer(modifier = Modifier.height(12.dp))
				val toastText = stringResource(R.string.permission_wizard_dev_options_toast)
				Button(
					onClick = {
						onOpenSettings()
						Toast.makeText(
							context,
							toastText,
							Toast.LENGTH_LONG
						).show()
					},
					modifier = Modifier.fillMaxWidth()
				) {
					Text(text = stringResource(id = R.string.permission_wizard_action_open_settings))
				}
			}
		}
	}
}

@Composable
private fun UsbDebuggingCard(
	isEnabled: Boolean,
	isDeveloperOptionsEnabled: Boolean,
	onOpenDeveloperSettings: () -> Unit,
) {
	val context = LocalContext.current
	val usbDebuggingToastText = stringResource(R.string.permission_wizard_usb_debugging_toast)

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
						id = if (isEnabled)
							R.string.permission_wizard_usb_debugging_enabled
						else
							R.string.permission_wizard_usb_debugging_disabled
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
			if (!isEnabled && isDeveloperOptionsEnabled) {
				Spacer(modifier = Modifier.height(12.dp))
				Button(
					onClick = {
						onOpenDeveloperSettings()
						Toast.makeText(
							context,
							usbDebuggingToastText,
							Toast.LENGTH_LONG
						).show()
					},
					modifier = Modifier.fillMaxWidth()
				) {
					Text(text = stringResource(id = R.string.permission_wizard_action_open_developer_settings))
				}
			}
		}
	}
}
