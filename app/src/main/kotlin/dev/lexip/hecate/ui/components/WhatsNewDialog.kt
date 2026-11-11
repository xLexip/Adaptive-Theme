/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 * Licensed under the GNU General Public License, Version 3.0.
 * https://www.gnu.org/licenses/gpl-3.0
 */
package dev.lexip.hecate.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import dev.lexip.hecate.R

@Composable
fun WhatsNewDialog(
	release: String,
	onDismiss: () -> Unit,
	onOpenChangelog: () -> Unit,
	onImpression: () -> Unit
) {
	val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
	LaunchedEffect(release, lifecycleState) {
		if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) onImpression()
	}
	AlertDialog(
		onDismissRequest = {
			onImpression()
			onDismiss()
		},
		title = { Text(stringResource(R.string.whats_new_title, release)) },
		text = {
			Text(
				text = stringResource(R.string.whats_new_message),
				modifier = Modifier.verticalScroll(rememberScrollState())
			)
		},
		confirmButton = {
			Button(onClick = {
				onImpression()
				onOpenChangelog()
			}) {
				Text(stringResource(R.string.whats_new_view_changelog))
			}
		},
		dismissButton = {
			TextButton(onClick = {
				onImpression()
				onDismiss()
			}) {
				Text(stringResource(R.string.action_close))
			}
		}
	)
}
