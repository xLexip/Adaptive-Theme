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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * Navigation row for setup steps with left and right buttons.
 *
 * @param leftTextRes String resource for left button text
 * @param onLeft Callback for left button click
 * @param rightTextRes String resource for right button text
 * @param onRight Callback for right button click
 * @param rightEnabled Whether the right button is enabled
 * @param rightIsPrimary Whether the right button should be a filled/primary button
 */
@Composable
internal fun StepNavigationRow(
	leftTextRes: Int,
	onLeft: () -> Unit,
	rightTextRes: Int,
	onRight: () -> Unit,
	rightEnabled: Boolean = true,
	rightIsPrimary: Boolean = true
) {
	val haptic = LocalHapticFeedback.current

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 4.dp)
			.padding(top = 8.dp),
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		OutlinedButton(onClick = onLeft) {
			Text(text = stringResource(id = leftTextRes))
		}

		if (rightIsPrimary) {
			Button(
				onClick = {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					onRight()
				},
				enabled = rightEnabled
			) {
				Text(text = stringResource(id = rightTextRes))
			}
		} else {
			OutlinedButton(
				onClick = {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					onRight()
				},
				enabled = rightEnabled
			) {
				Text(text = stringResource(id = rightTextRes))
			}
		}
	}
}

