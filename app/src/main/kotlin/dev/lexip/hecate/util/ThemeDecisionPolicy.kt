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

fun interface MinuteProvider {
	fun currentMinutes(): Int
}

object SystemMinuteProvider : MinuteProvider {
	override fun currentMinutes(): Int = NightWindowPolicy.currentMinutes()
}

object ThemeDecisionPolicy {
	fun shouldUseDarkTheme(
		lightValue: Float,
		thresholdLux: Float,
		stayDarkAtNightEnabled: Boolean,
		nightStartMinutes: Int,
		nightEndMinutes: Int,
		nowMinutes: Int
	): Boolean {
		if (
			stayDarkAtNightEnabled &&
			NightWindowPolicy.isInNightWindow(
				nowMinutes = nowMinutes,
				startMinutes = nightStartMinutes,
				endMinutes = nightEndMinutes
			)
		) {
			return true
		}

		return lightValue < thresholdLux
	}
}
