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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
