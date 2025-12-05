/*
 * Copyright (C) 2024-2025 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0
 *
 * Please see the License for specific terms regarding permissions and limitations.
 */

package dev.lexip.hecate.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.lexip.hecate.util.DarkThemeHandler
import dev.lexip.hecate.util.LightSensorManager
import dev.lexip.hecate.util.ProximitySensorManager
import kotlin.math.max

private const val TAG = "ScreenOnReceiver"
private const val HYSTERESIS_RATIO = 0.10f // 10%

/**
 * Adaptive theme switching logic. Executes when the screen is turned on.
 * The theme is switched based on the environment brightness and proximity sensor values.
 */
class ScreenOnReceiver(
	private val proximitySensorManager: ProximitySensorManager,
	private val lightSensorManager: LightSensorManager,
	private val darkThemeHandler: DarkThemeHandler,
	var adaptiveThemeThresholdLux: Float
) : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action == Intent.ACTION_SCREEN_ON) {
			Log.d(TAG, "Screen turned on, checking adaptive theme conditions...")

			// Check if the device is covered using the proximity sensor
			proximitySensorManager.startListening { distance ->
				proximitySensorManager.stopListening()

				// If the device is not covered, change the device theme based on the environment
				if (distance >= 5f) {
					lightSensorManager.startListening { lightValue ->
						lightSensorManager.stopListening()

						// Hysteresis around the configured threshold to avoid rapid toggles
						val threshold = adaptiveThemeThresholdLux
						val clampedThreshold = max(0f, threshold)
						val lower = clampedThreshold * (1f - HYSTERESIS_RATIO)
						val upper = clampedThreshold * (1f + HYSTERESIS_RATIO)
						val isCurrentlyDark = darkThemeHandler.isDarkThemeEnabled()

						val shouldBeDark =
							if (isCurrentlyDark) lightValue <= upper
							else lightValue < lower

						darkThemeHandler.setDarkTheme(shouldBeDark)
					}
				} else Log.d(TAG, "Device is covered, skipping adaptive theme checks.")

			}
		}
	}
}