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

/** Boundary around the privileged system-theme implementation. */
fun interface ThemeController {
	fun setDarkTheme(enabled: Boolean)
}

class DarkThemeController(
	private val handler: DarkThemeHandler
) : ThemeController {
	override fun setDarkTheme(enabled: Boolean) {
		handler.setDarkTheme(enabled)
	}
}

class AdaptiveAppearanceController(
	private val handler: AdaptiveAppearanceHandler
) : ThemeController {
	override fun setDarkTheme(enabled: Boolean) {
		handler.applyAppearance(enabled)
	}
}
