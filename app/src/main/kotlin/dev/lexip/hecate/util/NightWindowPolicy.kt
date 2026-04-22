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

import java.time.LocalTime

object NightWindowPolicy {
	fun isInNightWindow(nowMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
		if (!isValidMinute(nowMinutes) || !isValidMinute(startMinutes) || !isValidMinute(endMinutes)) {
			return false
		}
		if (startMinutes == endMinutes) return false

		return if (startMinutes < endMinutes) {
			nowMinutes in startMinutes until endMinutes
		} else {
			nowMinutes >= startMinutes || nowMinutes < endMinutes
		}
	}

	fun currentMinutes(): Int {
		val now = LocalTime.now()
		return now.hour * 60 + now.minute
	}

	private fun isValidMinute(value: Int): Boolean = value in 0 until 24 * 60
}

