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

package dev.lexip.hecate.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveThresholdTest {
	@Test
	fun fromIndex_clampsOutsideRange() {
		assertEquals(AdaptiveThreshold.DARK, AdaptiveThreshold.fromIndex(-1))
		assertEquals(AdaptiveThreshold.SUNLIGHT, AdaptiveThreshold.fromIndex(Int.MAX_VALUE))
	}

	@Test
	fun fromLux_returnsExactAndNearestThresholds() {
		assertEquals(AdaptiveThreshold.BRIGHT, AdaptiveThreshold.fromLux(100f))
		assertEquals(AdaptiveThreshold.SOFT, AdaptiveThreshold.fromLux(40f))
		assertEquals(AdaptiveThreshold.BRIGHT, AdaptiveThreshold.fromLux(60f))
	}

	@Test
	fun fromLux_usesFirstThresholdWhenDistanceIsEqual() {
		assertEquals(AdaptiveThreshold.DARK, AdaptiveThreshold.fromLux(0.5f))
	}
}
