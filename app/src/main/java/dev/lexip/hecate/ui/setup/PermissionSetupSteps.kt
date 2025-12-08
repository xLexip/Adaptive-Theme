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

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.R

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

	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState())
	) {
		Text(
			text = stringResource(id = R.string.permission_wizard_developer_mode_title),
			style = MaterialTheme.typography.headlineMedium,
			fontWeight = FontWeight.Bold
		)
		Spacer(modifier = Modifier.height(12.dp))
		Text(
			text = stringResource(id = R.string.permission_wizard_developer_mode_body),
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)

		Spacer(modifier = Modifier.height(24.dp))

		DeveloperOptionsCard(
			isEnabled = isDeveloperOptionsEnabled,
			onOpenSettings = onOpenSettings
		)

		Spacer(modifier = Modifier.height(16.dp))

		UsbDebuggingCard(
			isEnabled = isUsbDebuggingEnabled,
			isDeveloperOptionsEnabled = isDeveloperOptionsEnabled,
			onOpenDeveloperSettings = onOpenDeveloperSettings
		)

		Spacer(modifier = Modifier.height(16.dp))

		// Shizuku suggestion card, visible in all setup steps when installed
		ShizukuOptionCard(
			isVisible = isShizukuInstalled,
			onClick = onGrantViaShizuku
		)

		Spacer(modifier = Modifier.weight(1f))

		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			OutlinedButton(onClick = onExit) {
				Text(text = stringResource(id = R.string.action_close))
			}
			Button(
				onClick = {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					onNext()
				},
				enabled = bothEnabled
			) {
				Text(text = stringResource(id = R.string.action_continue))
			}
		}
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

@Composable
internal fun ConnectUsbStep(
	isUsbConnected: Boolean,
	isShizukuInstalled: Boolean,
	onGrantViaShizuku: () -> Unit,
	onNext: () -> Unit,
	onExit: () -> Unit,
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

	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState())
	) {
		Text(
			text = stringResource(id = R.string.permission_wizard_connect_title),
			style = MaterialTheme.typography.headlineMedium,
			fontWeight = FontWeight.Bold
		)
		Spacer(modifier = Modifier.height(12.dp))
		Text(
			text = stringResource(id = R.string.permission_wizard_connect_body),
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)

		Spacer(modifier = Modifier.height(24.dp))

		StatusCard(
			isCompleted = isUsbConnected,
			title = if (isUsbConnected)
				stringResource(id = R.string.permission_wizard_usb_connected)
			else
				stringResource(id = R.string.permission_wizard_usb_not_connected),
			isWaiting = !isUsbConnected
		)
		Spacer(modifier = Modifier.height(24.dp))

		ShizukuOptionCard(
			isVisible = isShizukuInstalled,
			onClick = onGrantViaShizuku
		)

		Spacer(modifier = Modifier.height(12.dp))

		ConnectionWhySection()

		Spacer(modifier = Modifier.height(12.dp))

		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			OutlinedButton(onClick = onExit) {
				Text(text = stringResource(id = R.string.action_close))
			}
			OutlinedButton(
				onClick = {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					onNext()
				}
			) {
				Text(text = stringResource(id = R.string.action_skip))
			}
		}
	}
}

@Composable
private fun ConnectionWhySection() {
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surface
		)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Text(
				text = stringResource(id = R.string.permission_wizard_why_other_device_title),
				style = MaterialTheme.typography.bodyMedium,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			Spacer(modifier = Modifier.height(8.dp))
			Text(
				text = stringResource(id = R.string.permission_wizard_why_other_device),
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}

	Spacer(modifier = Modifier.height(16.dp))

	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surface
		)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Text(
				text = stringResource(id = R.string.permission_wizard_is_this_safe_title),
				style = MaterialTheme.typography.bodyMedium,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			Spacer(modifier = Modifier.height(8.dp))
			Text(
				text = stringResource(id = R.string.permission_wizard_is_this_safe_body),
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

@Composable
internal fun GrantPermissionStep(
	adbCommand: String,
	hasWriteSecureSettings: Boolean,
	isShizukuInstalled: Boolean,
	onGrantViaShizuku: () -> Unit,
	onCopyAdbCommand: () -> Unit,
	onShareSetupUrl: () -> Unit,
	onShareExpertCommand: () -> Unit,
	onCheckPermission: () -> Unit,
	onExit: () -> Unit,
) {
	val haptic = LocalHapticFeedback.current

	val pulseScale = remember { Animatable(0.8f) }

	LaunchedEffect(hasWriteSecureSettings) {
		if (!hasWriteSecureSettings) {
			pulseScale.animateTo(
				targetValue = 1.2f,
				animationSpec = infiniteRepeatable(
					animation = tween(durationMillis = 750, easing = LinearOutSlowInEasing),
					repeatMode = RepeatMode.Reverse
				)
			)
		} else {
			pulseScale.snapTo(1.0f)
		}
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState())
	) {
		Text(
			text = stringResource(id = R.string.permission_wizard_grant_title),
			style = MaterialTheme.typography.headlineMedium,
			fontWeight = FontWeight.Bold
		)
		Spacer(modifier = Modifier.height(12.dp))
		Text(
			text = stringResource(id = R.string.permission_wizard_grant_body),
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)

		Spacer(modifier = Modifier.height(24.dp))

		WebsiteShareCard(onShareSetupUrl = onShareSetupUrl)

		Spacer(modifier = Modifier.height(20.dp))

		PermissionStatusSection(
			hasWriteSecureSettings = hasWriteSecureSettings,
			pulseScale = pulseScale
		)

		Spacer(modifier = Modifier.height(16.dp))

		ShizukuOptionCard(
			isVisible = isShizukuInstalled,
			onClick = onGrantViaShizuku
		)

		Spacer(modifier = Modifier.height(16.dp))

		ForExpertsSection(
			adbCommand = adbCommand,
			onCopyAdbCommand = onCopyAdbCommand,
			onShareExpertCommand = onShareExpertCommand
		)

		Spacer(modifier = Modifier.height(24.dp))

		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			OutlinedButton(onClick = onExit) {
				Text(text = stringResource(id = R.string.action_close))
			}
			Button(
				onClick = {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					onCheckPermission()
				},
				enabled = hasWriteSecureSettings
			) {
				Text(text = stringResource(id = R.string.action_finish))
			}
		}
	}
}

@Composable
private fun WebsiteShareCard(
	onShareSetupUrl: () -> Unit,
) {
	val haptic = LocalHapticFeedback.current

	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.primaryContainer
		)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(24.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				text = stringResource(id = R.string.permission_wizard_website_url),
				style = MaterialTheme.typography.displaySmall,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.primary,
				textAlign = androidx.compose.ui.text.style.TextAlign.Center
			)
			Spacer(modifier = Modifier.height(16.dp))
			OutlinedButton(
				onClick = {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					onShareSetupUrl()
				},
				modifier = Modifier.wrapContentWidth(),
				contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
			) {
				Text(text = stringResource(id = R.string.action_share_url))
			}
		}
	}
}

@Composable
private fun PermissionStatusSection(
	hasWriteSecureSettings: Boolean,
	pulseScale: Animatable<Float, *>,
) {
	if (hasWriteSecureSettings) {
		StatusCard(
			isCompleted = true,
			title = stringResource(id = R.string.permission_wizard_permission_granted)
		)
	} else {
		Card(
			modifier = Modifier.fillMaxWidth(),
			colors = CardDefaults.cardColors(
				containerColor = MaterialTheme.colorScheme.surface
			)
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Icon(
					imageVector = Icons.Outlined.Circle,
					contentDescription = null,
					modifier = Modifier
						.size(24.dp)
						.scale(pulseScale.value),
					tint = MaterialTheme.colorScheme.onSurfaceVariant
				)
				Spacer(modifier = Modifier.width(12.dp))
				Text(
					text = stringResource(id = R.string.permission_wizard_permission_not_granted),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}
}
