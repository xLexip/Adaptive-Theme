/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import dev.lexip.hecate.R

@Composable
fun BackgroundActivityHint(
	onOpenSettings: () -> Unit,
	onOpenGuide: () -> Unit,
	onDismiss: () -> Unit,
	onImpression: () -> Unit = {}
) {
	var isHalfVisible by remember { mutableStateOf(false) }
	var impressionReported by rememberSaveable { mutableStateOf(false) }
	val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
	val currentOnImpression by rememberUpdatedState(onImpression)
	val reportImpression = {
		if (!impressionReported) {
			impressionReported = true
			currentOnImpression()
		}
	}
	LaunchedEffect(isHalfVisible, lifecycleState) {
		if (isHalfVisible && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) reportImpression()
	}

	Surface(
		modifier = Modifier
			.fillMaxWidth()
			.onGloballyPositioned { coordinates ->
				val bounds = coordinates.boundsInWindow()
				val area = coordinates.size.width.toFloat() * coordinates.size.height
				isHalfVisible = area > 0 && bounds.width * bounds.height >= area * 0.5f
			},
		shape = RoundedCornerShape(24.dp),
		color = MaterialTheme.colorScheme.surfaceContainerLow
	) {
		Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					text = stringResource(R.string.background_help_title),
					style = MaterialTheme.typography.titleMedium,
					modifier = Modifier.weight(1f)
				)
				IconButton(onClick = {
					reportImpression()
					onDismiss()
				}) {
					Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.background_help_hide))
				}
			}
			Text(
				text = stringResource(R.string.background_help_generic),
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			BackgroundActivityHelpActions(
				onOpenSettings = {
					reportImpression()
					onOpenSettings()
				},
				onOpenGuide = {
					reportImpression()
					onOpenGuide()
				}
			)
		}
	}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackgroundActivityHelpActions(onOpenSettings: () -> Unit, onOpenGuide: () -> Unit) {
	FlowRow(
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalArrangement = Arrangement.spacedBy(4.dp)
	) {
		Button(onClick = onOpenGuide) {
			Text(stringResource(R.string.background_help_website))
		}
		TextButton(onClick = onOpenSettings) {
			Text(stringResource(R.string.background_help_open_settings))
		}
	}
}
