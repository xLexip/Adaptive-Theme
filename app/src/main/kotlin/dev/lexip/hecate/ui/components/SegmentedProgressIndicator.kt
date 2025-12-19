package dev.lexip.hecate.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun SegmentedProgressIndicator(segments: Int, activeIndex: Int, enabled: Boolean) {
	val sliderColors = androidx.compose.material3.SliderDefaults.colors()
	val activeColor = sliderColors.activeTrackColor
	val inactiveColor = sliderColors.inactiveTrackColor

	Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
		for (i in 0 until segments) {
			val isActive = i <= activeIndex
			val shape = RoundedCornerShape(8.dp)

			// Interpolate between colors for a smooth fade
			val targetColor = if (enabled && isActive) activeColor else inactiveColor
			val animatedColor by animateColorAsState(
				targetColor,
				animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
			)

			Box(
				modifier = Modifier
					.weight(1f)
					.height(16.dp)
					.clip(shape)
					.background(animatedColor)
			)
		}
	}
}