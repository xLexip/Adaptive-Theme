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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.R

@Composable
fun CustomThresholdDialog(
	show: Boolean,
	currentLux: Float,
	onConfirm: (Float) -> Unit,
	onDismiss: () -> Unit
) {
	if (!show) return

	var text by remember(currentLux) {
		mutableStateOf(
			if (currentLux >= 0) currentLux.toInt().toString() else ""
		)
	}
	var error by remember { mutableStateOf<Int?>(null) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(text = stringResource(id = R.string.title_custom_threshold)) },
		text = {
			Column(modifier = Modifier.fillMaxWidth()) {
				OutlinedTextField(
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 4.dp),
					value = text,
					onValueChange = {
						text = it
						error = null
					},
					label = { Text(text = stringResource(id = R.string.hint_custom_threshold_value)) },
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
					singleLine = true,
					isError = error != null,
					supportingText = error?.let { errorRes ->
						{ Text(text = stringResource(id = errorRes)) }
					}
				)
			}
		},
		confirmButton = {
			TextButton(onClick = {
				val value = text.toFloatOrNull()
				if (value == null) {
					error = R.string.error_invalid_lux_value
					return@TextButton
				}

				if (value < 0f) {
					error = R.string.error_negative_lux_value
					return@TextButton
				}

				onConfirm(value)
			}) {
				Text(text = stringResource(id = R.string.action_set))
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(text = stringResource(id = R.string.action_cancel))
			}
		}
	)
}
