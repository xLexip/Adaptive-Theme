package dev.lexip.hecate.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.R

@Composable
fun SetupRequiredCard(
	modifier: Modifier = Modifier,
	title: String,
	message: String,
	onLaunchSetup: () -> Unit,
	shakeKey: Int = 0,
) {
	// Shake animation when user tries to enable service without permission
	val offsetAnim = remember { Animatable(0f) }

	val haptic = LocalHapticFeedback.current

	LaunchedEffect(shakeKey) {
		if (shakeKey > 0) {
			val offsets = listOf(-12f, 12f, -8f, 8f, -4f, 4f, 0f)
			for (o in offsets) {
				offsetAnim.animateTo(o, animationSpec = tween(durationMillis = 45))
			}
		}
	}

	Card(
		modifier = modifier.offset { IntOffset(offsetAnim.value.dp.roundToPx(), 0) },
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
			contentColor = MaterialTheme.colorScheme.onSurface
		),
		shape = RoundedCornerShape(24.dp)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Text(
				text = title,
				style = MaterialTheme.typography.titleMedium
			)
			Spacer(modifier = Modifier.padding(top = 4.dp))
			Text(
				text = message,
				style = MaterialTheme.typography.bodyMedium
			)
			Spacer(modifier = Modifier.padding(top = 12.dp))
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.Center
			) {
				Button(onClick = {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					onLaunchSetup()
				}) {
					Text(text = stringResource(id = R.string.action_start_setup))
				}
			}
		}
	}
}