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

package dev.lexip.hecate.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeDecisionPolicyTest {
	@Test
	fun lightBelowThresholdUsesDarkTheme() {
		assertTrue(decide(light = 99f, threshold = 100f))
	}

	@Test
	fun lightAtOrAboveThresholdUsesLightTheme() {
		assertFalse(decide(light = 100f, threshold = 100f))
		assertFalse(decide(light = 101f, threshold = 100f))
	}

	@Test
	fun activeNightWindowForcesDarkTheme() {
		assertTrue(
			ThemeDecisionPolicy.shouldUseDarkTheme(
				lightValue = 10_000f,
				thresholdLux = 100f,
				stayDarkAtNightEnabled = true,
				nightStartMinutes = 21 * 60,
				nightEndMinutes = 6 * 60,
				nowMinutes = 23 * 60
			)
		)
	}

	private fun decide(light: Float, threshold: Float) =
		ThemeDecisionPolicy.shouldUseDarkTheme(
			lightValue = light,
			thresholdLux = threshold,
			stayDarkAtNightEnabled = false,
			nightStartMinutes = 21 * 60,
			nightEndMinutes = 6 * 60,
			nowMinutes = 12 * 60
		)
}
