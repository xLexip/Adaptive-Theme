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

package dev.lexip.hecate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SliderPreferenceCard(
	title: String,
	valueIndex: Int,
	steps: Int,
	labels: List<String>,
	lux: List<Float>? = null,
	onValueChange: (Int) -> Unit,
	enabled: Boolean = true,
	topRounded: Boolean = false,
	bottomRounded: Boolean = false,
) {
	val largeRadius = 20.dp
	val smallRadius = 4.dp
	val shape = RoundedCornerShape(
		topStart = if (topRounded) largeRadius else smallRadius,
		topEnd = if (topRounded) largeRadius else smallRadius,
		bottomStart = if (bottomRounded) largeRadius else smallRadius,
		bottomEnd = if (bottomRounded) largeRadius else smallRadius,
	)

	Card(
		shape = shape,
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
	) {
		Column(
			modifier = Modifier.padding(14.dp),
			verticalArrangement = Arrangement.spacedBy(0.dp)
		) {
			Text(
				modifier = Modifier.alpha(if (enabled) 1f else 0.38f),
				text = title,
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurface,
			)
			LabeledSlider(
				valueIndex = valueIndex,
				steps = steps,
				labels = labels,
				lux = lux,
				onValueChange = onValueChange,
				enabled = enabled
			)
		}
	}
}

@Composable
private fun LabeledSlider(
	valueIndex: Int,
	steps: Int,
	labels: List<String>,
	lux: List<Float>? = null,
	onValueChange: (Int) -> Unit,
	enabled: Boolean = true
) {
	val haptic = LocalHapticFeedback.current
	var sliderPosition by remember { mutableFloatStateOf(valueIndex.toFloat()) }
	var lastLiveIndex by remember { mutableIntStateOf(valueIndex) }

	LaunchedEffect(valueIndex) { sliderPosition = valueIndex.toFloat() }

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 12.dp)
	) {
		Slider(
			value = sliderPosition,
			onValueChange = { new ->
				sliderPosition = new
				val liveIndex = sliderPosition.roundToInt().coerceIn(0, steps - 1)
				if (liveIndex != lastLiveIndex) {
					haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
					lastLiveIndex = liveIndex
				}
			},
			onValueChangeFinished = { onValueChange(sliderPosition.toInt()) },
			valueRange = 0f..(steps - 1).toFloat(),
			steps = (steps - 2).coerceAtLeast(0),
			enabled = enabled,
			colors = SliderDefaults.colors()
		)

		if (enabled) {
			val liveIndex = sliderPosition.roundToInt().coerceIn(0, steps - 1)
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text(
					text = labels.getOrNull(liveIndex) ?: liveIndex.toString(),
					fontSize = MaterialTheme.typography.bodySmall.fontSize,
					color = MaterialTheme.colorScheme.primary,
					modifier = Modifier.weight(1f)
				)
				Text(
					text = lux?.getOrNull(liveIndex)?.toInt()?.let { "$it lux" } ?: "",
					fontSize = MaterialTheme.typography.bodySmall.fontSize,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					textAlign = TextAlign.End
				)
			}
		}
	}
}
