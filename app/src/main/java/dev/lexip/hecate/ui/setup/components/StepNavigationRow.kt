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

@Composable
internal fun StepNavigationRow(
	leftTextRes: Int,
	onLeft: () -> Unit,
	rightTextRes: Int,
	onRight: () -> Unit,
	rightEnabled: Boolean = true,
	rightIsPrimary: Boolean = true,
) {
	val haptic = LocalHapticFeedback.current

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 12.dp)
			.padding(top = 12.dp),
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
				}
			) {
				Text(text = stringResource(id = rightTextRes))
			}
		}
	}
}

