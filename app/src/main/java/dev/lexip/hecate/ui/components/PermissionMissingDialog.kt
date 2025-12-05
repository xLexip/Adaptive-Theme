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

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import dev.lexip.hecate.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionMissingDialog(
	show: Boolean,
	setupUrl: String,
	onOpenSetup: () -> Unit,
	onShareSetupUrl: (String) -> Unit,
	onDismiss: () -> Unit
) {
	if (!show) return

	val haptic = LocalHapticFeedback.current

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(text = stringResource(id = R.string.title_missing_permission)) },
		text = {
			// Show description text and extra action buttons in a Column
			Column {
				Text(
					text = stringResource(
						id = R.string.description_missing_permission,
						setupUrl
					)
				)

				TextButton(onClick = {
					onOpenSetup()
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
				}, modifier = Modifier.align(Alignment.CenterHorizontally)) {
					Text(text = stringResource(id = R.string.action_view_website))
				}


			}
		},
		confirmButton = {
			Button(onClick = {
				haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
				onShareSetupUrl(setupUrl)
			}) {
				Text(text = stringResource(id = R.string.action_share_setup_url))
			}
		},
		dismissButton = {
			OutlinedButton(onClick = onDismiss) {
				Text(text = stringResource(id = R.string.action_close))
			}
		}
	)
}
