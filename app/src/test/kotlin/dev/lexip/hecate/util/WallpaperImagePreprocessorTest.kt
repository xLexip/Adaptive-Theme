/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package dev.lexip.hecate.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperImagePreprocessorTest {

	@Test
	fun usesTargetDimensionsWhenTheyRequireMoreDownsampling() {
		assertEquals(
			8,
			calculateDecodeSampleSize(
				sourceWidth = 8_000,
				sourceHeight = 6_000,
				targetWidth = 1_000,
				targetHeight = 750,
				maxDecodePixels = 16_000_000L
			)
		)
	}

	@Test
	fun capsOversizedDecodeEvenWhenTargetWouldUseFullResolution() {
		assertEquals(
			2,
			calculateDecodeSampleSize(
				sourceWidth = 12_000,
				sourceHeight = 4_000,
				targetWidth = 12_000,
				targetHeight = 4_000,
				maxDecodePixels = 16_000_000L
			)
		)
	}

	@Test
	fun keepsFullResolutionAtTheDecodePixelBudget() {
		assertEquals(
			1,
			calculateDecodeSampleSize(
				sourceWidth = 8_000,
				sourceHeight = 2_000,
				targetWidth = 8_000,
				targetHeight = 2_000,
				maxDecodePixels = 16_000_000L
			)
		)
	}
}
