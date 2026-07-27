/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package dev.lexip.hecate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveAppearanceHandlerTest {
	@Test
	fun `applies matching wallpaper after successful theme transition`() {
		var appliedTheme: Boolean? = null
		var appliedDayUri: String? = null
		var appliedNightUri: String? = null
		val handler = AdaptiveAppearanceHandler(
			setDarkTheme = {
				DarkThemeChangeResult(succeeded = true, changed = true)
			},
			applyWallpaperForTheme = { isDark, dayUri, nightUri ->
				appliedTheme = isDark
				appliedDayUri = dayUri
				appliedNightUri = nightUri
				true
			}
		)
		handler.configureWallpaperSync(
			enabled = true,
			dayWallpaperUri = "content://wallpaper/day",
			nightWallpaperUri = "content://wallpaper/night"
		)

		val result = handler.applyAppearance(useDarkTheme = true)

		assertTrue(result.succeeded)
		assertTrue(result.changed)
		assertEquals(true, appliedTheme)
		assertEquals("content://wallpaper/day", appliedDayUri)
		assertEquals("content://wallpaper/night", appliedNightUri)
	}

	@Test
	fun `does not apply wallpaper when theme is already correct`() {
		var wallpaperApplied = false
		val handler = createHandler(
			themeResult = DarkThemeChangeResult(succeeded = true, changed = false),
			onWallpaperApplied = { wallpaperApplied = true }
		)

		val result = handler.applyAppearance(useDarkTheme = false)

		assertTrue(result.succeeded)
		assertFalse(result.changed)
		assertFalse(wallpaperApplied)
	}

	@Test
	fun `does not apply wallpaper when theme change fails`() {
		var wallpaperApplied = false
		val handler = createHandler(
			themeResult = DarkThemeChangeResult(succeeded = false, changed = false),
			onWallpaperApplied = { wallpaperApplied = true }
		)

		val result = handler.applyAppearance(useDarkTheme = true)

		assertFalse(result.succeeded)
		assertFalse(wallpaperApplied)
	}

	@Test
	fun `does not apply wallpaper when sync is disabled`() {
		var wallpaperApplied = false
		val handler = AdaptiveAppearanceHandler(
			setDarkTheme = {
				DarkThemeChangeResult(succeeded = true, changed = true)
			},
			applyWallpaperForTheme = { _, _, _ ->
				wallpaperApplied = true
				true
			}
		)
		handler.configureWallpaperSync(
			enabled = false,
			dayWallpaperUri = "content://wallpaper/day",
			nightWallpaperUri = "content://wallpaper/night"
		)

		handler.applyAppearance(useDarkTheme = true)

		assertFalse(wallpaperApplied)
	}

	@Test
	fun `does not apply wallpaper when sync configuration is incomplete`() {
		var wallpaperApplied = false
		val handler = AdaptiveAppearanceHandler(
			setDarkTheme = {
				DarkThemeChangeResult(succeeded = true, changed = true)
			},
			applyWallpaperForTheme = { _, _, _ ->
				wallpaperApplied = true
				true
			}
		)
		handler.configureWallpaperSync(
			enabled = true,
			dayWallpaperUri = "content://wallpaper/day",
			nightWallpaperUri = null
		)

		handler.applyAppearance(useDarkTheme = true)

		assertFalse(wallpaperApplied)
	}

	private fun createHandler(
		themeResult: DarkThemeChangeResult,
		onWallpaperApplied: () -> Unit
	): AdaptiveAppearanceHandler {
		return AdaptiveAppearanceHandler(
			setDarkTheme = { themeResult },
			applyWallpaperForTheme = { _, _, _ ->
				onWallpaperApplied()
				true
			}
		).apply {
			configureWallpaperSync(
				enabled = true,
				dayWallpaperUri = "content://wallpaper/day",
				nightWallpaperUri = "content://wallpaper/night"
			)
		}
	}
}
