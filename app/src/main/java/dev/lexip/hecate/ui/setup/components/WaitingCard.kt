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

package dev.lexip.hecate.ui.setup.components

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
internal fun rememberPulseScale(isActive: Boolean): Float {
	val pulseScale = remember { Animatable(0.8f) }

	LaunchedEffect(isActive) {
		if (isActive) {
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

	return pulseScale.value
}

@Composable
internal fun WaitingCircle(
	modifier: Modifier = Modifier,
	isWaiting: Boolean,
) {
	val pulseScale = if (isWaiting) rememberPulseScale(isActive = true) else 1f

	if (isWaiting) {
		Icon(
			imageVector = Icons.Outlined.Circle,
			contentDescription = null,
			modifier = modifier
				.size(32.dp)
				.scale(pulseScale),
			tint = MaterialTheme.colorScheme.onSurfaceVariant
		)
	} else {
		Icon(
			imageVector = Icons.Filled.CheckCircle,
			contentDescription = null,
			modifier = modifier.size(32.dp),
			tint = MaterialTheme.colorScheme.primary
		)
	}
}

@Composable
internal fun SetupWaitingCard(
	title: String,
	isWaiting: Boolean,
	onClick: (() -> Unit)? = null,
) {
	CardDefaults.cardColors(
		containerColor = MaterialTheme.colorScheme.primaryContainer,
	)

	Card(
		onClick = onClick ?: {},
		enabled = onClick != null,
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
				WaitingCircle(
					isWaiting = isWaiting,
				)
				Spacer(modifier = Modifier.width(16.dp))
				Text(
					text = title,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Normal,
					color = MaterialTheme.colorScheme.onSurface
				)
			}
		}
	}
}
