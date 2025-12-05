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

package dev.lexip.hecate.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Extension to format lux values with locale-aware thousands separators.
 * Usage: val s = 10000.formatLux()
 */
fun Int.formatLux(): String {
	val nf = NumberFormat.getIntegerInstance(Locale.getDefault())
	return nf.format(this)
}
