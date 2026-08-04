/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package dev.lexip.hecate.services

import dev.lexip.hecate.broadcasts.ScreenOnReceiverSettings
import dev.lexip.hecate.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringPreferencesCoordinatorTest {
	@Test
	fun appliesReceiverAndWallpaperPreferencesTogether() {
		val wallpaperConfigurations = mutableListOf<WallpaperConfiguration>()
		val coordinator = MonitoringPreferencesCoordinator { enabled, dayUri, nightUri ->
			wallpaperConfigurations += WallpaperConfiguration(enabled, dayUri, nightUri)
		}
		val receiver = FakeScreenOnReceiverSettings()
		val preferences = preferences(
			thresholdLux = 420f,
			stayDarkAtNight = true,
			nightStartMinutes = 22 * 60,
			nightEndMinutes = 5 * 60,
			wallpaperSyncEnabled = true,
			dayUri = "content://wallpaper/day",
			nightUri = "content://wallpaper/night"
		)

		coordinator.apply(preferences, receiver)

		assertEquals(420f, receiver.adaptiveThemeThresholdLux)
		assertTrue(receiver.stayDarkAtNightEnabled)
		assertEquals(22 * 60, receiver.nightStartMinutes)
		assertEquals(5 * 60, receiver.nightEndMinutes)
		assertEquals(
			listOf(
				WallpaperConfiguration(
					true,
					"content://wallpaper/day",
					"content://wallpaper/night"
				)
			),
			wallpaperConfigurations
		)
	}

	@Test
	fun appliesWallpaperConfigurationWithoutARegisteredReceiver() {
		var configuration: WallpaperConfiguration? = null
		val coordinator = MonitoringPreferencesCoordinator { enabled, dayUri, nightUri ->
			configuration = WallpaperConfiguration(enabled, dayUri, nightUri)
		}

		coordinator.apply(
			preferences(
				wallpaperSyncEnabled = false,
				dayUri = null,
				nightUri = null
			),
			receiver = null
		)

		assertEquals(WallpaperConfiguration(false, null, null), configuration)
	}

	private fun preferences(
		thresholdLux: Float = 1_000f,
		stayDarkAtNight: Boolean = false,
		nightStartMinutes: Int = 21 * 60,
		nightEndMinutes: Int = 6 * 60,
		wallpaperSyncEnabled: Boolean = false,
		dayUri: String? = null,
		nightUri: String? = null
	) = UserPreferences(
		adaptiveThemeEnabled = true,
		adaptiveThemeThresholdLux = thresholdLux,
		stayDarkAtNightEnabled = stayDarkAtNight,
		nightStartMinutes = nightStartMinutes,
		nightEndMinutes = nightEndMinutes,
		wallpaperSyncEnabled = wallpaperSyncEnabled,
		dayWallpaperUri = dayUri,
		nightWallpaperUri = nightUri
	)
}

private data class WallpaperConfiguration(
	val enabled: Boolean,
	val dayUri: String?,
	val nightUri: String?
)

private class FakeScreenOnReceiverSettings : ScreenOnReceiverSettings {
	override var adaptiveThemeThresholdLux: Float = 0f
	override var stayDarkAtNightEnabled: Boolean = false
	override var nightStartMinutes: Int = 0
	override var nightEndMinutes: Int = 0
}
