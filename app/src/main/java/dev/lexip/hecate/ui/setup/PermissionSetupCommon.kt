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
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
internal fun StatusCard(
	isCompleted: Boolean,
	title: String,
	onClick: (() -> Unit)? = null,
	isWaiting: Boolean = false
) {
	val cardColors = if (isCompleted) {
		CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.primaryContainer,
		)
	} else {
		CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surface,
		)
	}

	val pulseScale = remember { Animatable(0.8f) }

	LaunchedEffect(isWaiting) {
		if (isWaiting) {
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

	Card(
		onClick = onClick ?: {},
		enabled = onClick != null,
		modifier = Modifier
			.fillMaxWidth()
			.height(80.dp),
		colors = cardColors
	) {
		Row(
			modifier = Modifier
				.fillMaxSize()
				.padding(20.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier.weight(1f)
			) {
				val icon = if (isCompleted && !isWaiting) {
					Icons.Filled.CheckCircle
				} else {
					Icons.Outlined.Circle
				}
				Icon(
					imageVector = icon,
					contentDescription = null,
					modifier = Modifier
						.size(32.dp)
						.scale(pulseScale.value),
					tint = if (isCompleted && !isWaiting)
						MaterialTheme.colorScheme.primary
					else
						MaterialTheme.colorScheme.onSurfaceVariant
				)
				Spacer(modifier = Modifier.width(16.dp))
				Text(
					text = title,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = if (isCompleted) FontWeight.Medium else FontWeight.Normal,
					color = if (isCompleted)
						MaterialTheme.colorScheme.onPrimaryContainer
					else
						MaterialTheme.colorScheme.onSurface
				)
			}
		}
	}
}

@Composable
internal fun ForExpertsSection(
	adbCommand: String?,
	onCopyAdbCommand: (() -> Unit)? = null,
	onShareExpertCommand: (() -> Unit)? = null,
) {
	val context = LocalContext.current
	val haptic = LocalHapticFeedback.current
	var expanded by remember { mutableStateOf(false) }

	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surface
		)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.clickable { expanded = !expanded },
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = stringResource(id = R.string.permission_wizard_for_experts),
					style = MaterialTheme.typography.labelLarge,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.weight(1f)
				)
				IconButton(onClick = { expanded = !expanded }) {
					Icon(
						imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
						contentDescription = null
					)
				}
			}

			if (expanded) {
				Spacer(modifier = Modifier.height(8.dp))
				Text(
					text = stringResource(id = R.string.permission_wizard_manual_command),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)

				Spacer(modifier = Modifier.height(12.dp))
				Surface(
					modifier = Modifier.fillMaxWidth(),
					color = MaterialTheme.colorScheme.surface,
					shape = MaterialTheme.shapes.small
				) {
					Text(
						text = adbCommand ?: "",
						style = MaterialTheme.typography.bodySmall,
						modifier = Modifier.padding(12.dp),
						fontWeight = FontWeight.Medium
					)
				}

				Spacer(modifier = Modifier.height(8.dp))
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(8.dp)
				) {
					OutlinedButton(
						onClick = {
							haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
							onCopyAdbCommand?.invoke()
							Toast.makeText(
								context,
								R.string.permission_wizard_copied,
								Toast.LENGTH_SHORT
							).show()
						},
						modifier = Modifier.weight(1f)
					) {
						Text(text = stringResource(id = R.string.action_copy))
					}

					Button(
						onClick = {
							haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
							onShareExpertCommand?.invoke()
						},
						modifier = Modifier.weight(1f)
					) {
						Text(text = stringResource(id = R.string.action_share))
					}
				}
			}
		}
	}
}
