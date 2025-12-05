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

package dev.lexip.hecate.ui.components.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

@Composable
fun DetailPreferenceCard(
	title: String,
	enabled: Boolean = true,
	firstCard: Boolean = false,
	lastCard: Boolean = false,
	content: @Composable () -> Unit
) {
	val largeRadius = 20.dp
	val smallRadius = 4.dp
	val shape = RoundedCornerShape(
		topStart = if (firstCard) largeRadius else smallRadius,
		topEnd = if (firstCard) largeRadius else smallRadius,
		bottomStart = if (lastCard) largeRadius else smallRadius,
		bottomEnd = if (lastCard) largeRadius else smallRadius,
	)

	Card(
		shape = shape,
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(14.dp)
				.alpha(if (enabled) 1f else 0.38f)
		) {
			Text(
				text = title,
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurface
			)

			content()
		}
	}
}
