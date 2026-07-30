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

class NightWindowPolicyTest {
	@Test
	fun sameDayWindow_includesStartAndExcludesEnd() {
		assertTrue(NightWindowPolicy.isInNightWindow(60, 60, 120))
		assertTrue(NightWindowPolicy.isInNightWindow(119, 60, 120))
		assertFalse(NightWindowPolicy.isInNightWindow(120, 60, 120))
	}

	@Test
	fun overnightWindow_wrapsAcrossMidnight() {
		assertTrue(NightWindowPolicy.isInNightWindow(23 * 60, 21 * 60, 6 * 60))
		assertTrue(NightWindowPolicy.isInNightWindow(0, 21 * 60, 6 * 60))
		assertFalse(NightWindowPolicy.isInNightWindow(12 * 60, 21 * 60, 6 * 60))
	}

	@Test
	fun equalOrInvalidMinutes_areNeverInWindow() {
		assertFalse(NightWindowPolicy.isInNightWindow(60, 60, 60))
		assertFalse(NightWindowPolicy.isInNightWindow(-1, 60, 120))
		assertFalse(NightWindowPolicy.isInNightWindow(60, -1, 120))
		assertFalse(NightWindowPolicy.isInNightWindow(60, 60, 1440))
	}
}
