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

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import dev.lexip.hecate.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionMissingDialog(
	show: Boolean,
	adbCommand: String,
	onCopy: (String) -> Unit,
	onCloseApp: () -> Unit,
	onDismiss: () -> Unit
) {
	if (!show) return

	val haptic = LocalHapticFeedback.current

	LaunchedEffect(show) {
		if (show) haptic.performHapticFeedback(HapticFeedbackType.Reject)
	}

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(text = stringResource(id = R.string.title_missing_permission)) },
		text = {
			Text(
				text = stringResource(
					id = R.string.description_missing_secure_settings_permission,
					adbCommand
				)
			)
		},
		confirmButton = {
			TextButton(onClick = {
				onCopy(adbCommand)
				haptic.performHapticFeedback(HapticFeedbackType.Confirm)
			}) {
				Text(text = stringResource(id = R.string.action_copy_adb_command))
			}
		},
		dismissButton = {
			TextButton(onClick = onCloseApp) {
				Text(text = stringResource(id = R.string.action_close_app))
			}
		}
	)
}
