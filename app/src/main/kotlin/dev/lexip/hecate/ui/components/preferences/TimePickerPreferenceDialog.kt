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

package dev.lexip.hecate.ui.components.preferences

import android.text.format.DateFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.lexip.hecate.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerPreferenceDialog(
	show: Boolean,
	title: String,
	initialMinutes: Int,
	onConfirm: (Int) -> Unit,
	onDismiss: () -> Unit
) {
	if (!show) return

	val context = LocalContext.current
	val is24Hour = DateFormat.is24HourFormat(context)
	val state = rememberTimePickerState(
		initialHour = initialMinutes / 60,
		initialMinute = initialMinutes % 60,
		is24Hour = is24Hour
	)

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(text = title) },
		text = {
			TimePicker(state = state)
		},
		confirmButton = {
			Button(
				onClick = {
					onConfirm(state.hour * 60 + state.minute)
				}
			) {
				Text(text = stringResource(id = R.string.action_set))
			}
		},
		dismissButton = {
			OutlinedButton(onClick = onDismiss) {
				Text(text = stringResource(id = R.string.action_cancel))
			}
		}
	)
}

