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

package dev.lexip.hecate.data

import dev.lexip.hecate.R

enum class AdaptiveThreshold(val labelRes: Int, val lux: Float) {
	DARK(R.string.adaptive_threshold_dark, 0f),
	DIM(R.string.adaptive_threshold_dim, 1f),
	SOFT(R.string.adaptive_threshold_soft, 10f),
	BRIGHT(R.string.adaptive_threshold_bright, 100f),
	DAYLIGHT(R.string.adaptive_threshold_daylight, 1_000f),
	SUNLIGHT(R.string.adaptive_threshold_sunlight, 10_000f);

	companion object {
		fun fromIndex(index: Int): AdaptiveThreshold {
			val i = index.coerceIn(0, entries.size - 1)
			return entries[i]
		}

		fun fromLux(lux: Float): AdaptiveThreshold {
			val exact = entries.firstOrNull { it.lux == lux }
			if (exact != null) return exact
			return entries.minByOrNull { kotlin.math.abs(it.lux - lux) } ?: SOFT
		}
	}
}
