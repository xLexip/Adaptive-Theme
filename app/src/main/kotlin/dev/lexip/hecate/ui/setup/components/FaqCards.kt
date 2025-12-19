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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.R

@Composable
internal fun SetupFAQCards() {
	WhyOtherDeviceCard()
	IsThisSafeCard()
}

@Composable
internal fun WhyOtherDeviceCard() {
	var expanded by remember { mutableStateOf(false) }

	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surface
		)
	) {
		Column(modifier = Modifier
			.padding(16.dp)
			.animateContentSize()) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.fillMaxWidth()
					.clickable { expanded = !expanded }
			) {
				Text(
					text = stringResource(id = R.string.setup_why_other_device_title),
					style = MaterialTheme.typography.bodyMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.weight(1f)
				)
				val rotation by animateFloatAsState(
					targetValue = if (expanded) 180f else 0f,
					animationSpec = tween(durationMillis = 300)
				)
				IconButton(onClick = { expanded = !expanded }) {
					Icon(
						imageVector = Icons.Outlined.ExpandMore,
						contentDescription = null,
						modifier = Modifier.rotate(rotation)
					)
				}
			}

			AnimatedVisibility(
				visible = expanded,
				enter = fadeIn(animationSpec = tween(200)) + expandVertically(
					animationSpec = tween(
						300
					)
				),
				exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(
					animationSpec = tween(
						200
					)
				)
			) {
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = stringResource(id = R.string.setup_why_other_device),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}
}

@Composable
internal fun IsThisSafeCard() {
	var expanded by remember { mutableStateOf(false) }

	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surface
		)
	) {
		Column(modifier = Modifier
			.padding(16.dp)
			.animateContentSize()) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.fillMaxWidth()
					.clickable { expanded = !expanded }
			) {
				Text(
					text = stringResource(id = R.string.setup_is_this_safe_title),
					style = MaterialTheme.typography.bodyMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.weight(1f)
				)
				val rotation by animateFloatAsState(
					targetValue = if (expanded) 180f else 0f,
					animationSpec = tween(durationMillis = 300)
				)
				IconButton(onClick = { expanded = !expanded }) {
					Icon(
						imageVector = Icons.Outlined.ExpandMore,
						contentDescription = null,
						modifier = Modifier.rotate(rotation)
					)
				}
			}

			AnimatedVisibility(
				visible = expanded,
				enter = fadeIn(animationSpec = tween(200)) + expandVertically(
					animationSpec = tween(
						300
					)
				),
				exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(
					animationSpec = tween(
						200
					)
				)
			) {
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = stringResource(id = R.string.setup_is_this_safe_body),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}
}
