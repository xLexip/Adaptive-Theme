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

package dev.lexip.hecate.broadcasts

import dev.lexip.hecate.util.MinuteProvider
import dev.lexip.hecate.util.ProximitySensorReader
import dev.lexip.hecate.util.SensorReader
import dev.lexip.hecate.util.ThemeController
import dev.lexip.hecate.util.ThemeDecisionPolicy

internal const val COVERED_DISTANCE_CENTIMETERS = 5f

class ScreenOnCoordinator(
	private val proximitySensor: ProximitySensorReader,
	private val lightSensor: SensorReader,
	private val themeController: ThemeController,
	private val minuteProvider: MinuteProvider,
	var adaptiveThemeThresholdLux: Float,
	var stayDarkAtNightEnabled: Boolean,
	var nightStartMinutes: Int,
	var nightEndMinutes: Int
) {
	private var proximityReadingPending = false
	private var lightReadingPending = false

	fun onScreenOn() {
		if (proximityReadingPending || lightReadingPending) return

		if (!proximitySensor.hasProximitySensor) {
			readLightAndApplyTheme()
			return
		}

		proximityReadingPending = true
		proximitySensor.startListening(
			callback = { distance ->
				if (!proximityReadingPending) return@startListening
				proximityReadingPending = false
				proximitySensor.stopListening()
				if (distance >= COVERED_DISTANCE_CENTIMETERS) {
					readLightAndApplyTheme()
				}
			}
		)
	}

	private fun readLightAndApplyTheme() {
		if (lightReadingPending) return
		lightReadingPending = true
		lightSensor.startListening(
			callback = { lightValue ->
				if (!lightReadingPending) return@startListening
				lightReadingPending = false
				lightSensor.stopListening()
				themeController.setDarkTheme(
					ThemeDecisionPolicy.shouldUseDarkTheme(
						lightValue = lightValue,
						thresholdLux = adaptiveThemeThresholdLux,
						stayDarkAtNightEnabled = stayDarkAtNightEnabled,
						nightStartMinutes = nightStartMinutes,
						nightEndMinutes = nightEndMinutes,
						nowMinutes = minuteProvider.currentMinutes()
					)
				)
			}
		)
	}
}
