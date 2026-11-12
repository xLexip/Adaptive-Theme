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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WallpaperUpdateSchedulerTest {
	@Test
	fun `coalesces pending wallpaper updates to the latest target`() = runTest {
		val appliedRequests = mutableListOf<Pair<Boolean, Boolean>>()
		val lockScreenDestinations = mutableListOf<Boolean>()
		val scheduler = WallpaperUpdateScheduler(
			scope = this,
			dispatcher = StandardTestDispatcher(testScheduler),
			applyWallpaper = { isDark, _, _, lockScreenBlurEnabled, lockSyncEnabled ->
				appliedRequests += isDark to lockScreenBlurEnabled
				lockScreenDestinations += lockSyncEnabled
				true
			}
		)

		scheduler.schedule(isDark = false, dayUri = "day-1", nightUri = "night-1")
		scheduler.schedule(
			isDark = true,
			dayUri = "day-2",
			nightUri = "night-2",
			lockScreenWallpaperBlurEnabled = true
		)
		advanceUntilIdle()

		assertEquals(listOf(true to true), appliedRequests)
		assertEquals(listOf(false), lockScreenDestinations)
	}

	@Test
	fun `starts a new worker after the previous update completed`() = runTest {
		val appliedThemes = mutableListOf<Boolean>()
		val scheduler = WallpaperUpdateScheduler(
			scope = this,
			dispatcher = StandardTestDispatcher(testScheduler),
			applyWallpaper = { isDark, _, _, _, _ ->
				appliedThemes += isDark
				true
			}
		)

		scheduler.schedule(isDark = false, dayUri = "day", nightUri = "night")
		advanceUntilIdle()
		scheduler.schedule(isDark = true, dayUri = "day", nightUri = "night")
		advanceUntilIdle()

		assertEquals(listOf(false, true), appliedThemes)
	}

	@Test
	fun `does not repeat the last successful wallpaper update`() = runTest {
		var applyCalls = 0
		val scheduler = WallpaperUpdateScheduler(
			scope = this,
			dispatcher = StandardTestDispatcher(testScheduler),
			applyWallpaper = { _, _, _, _, _ ->
				applyCalls++
				true
			}
		)

		scheduler.schedule(isDark = true, dayUri = "day", nightUri = "night")
		advanceUntilIdle()
		scheduler.schedule(isDark = true, dayUri = "day", nightUri = "night")
		advanceUntilIdle()

		assertEquals(1, applyCalls)
	}

	@Test
	fun `changing lock screen destination is not deduplicated`() = runTest {
		val destinations = mutableListOf<Boolean>()
		val scheduler = WallpaperUpdateScheduler(
			scope = this,
			dispatcher = StandardTestDispatcher(testScheduler),
			applyWallpaper = { _, _, _, _, lockSyncEnabled ->
				destinations += lockSyncEnabled
				true
			}
		)

		for (lockSyncEnabled in listOf(true, false, true)) {
			scheduler.schedule(false, "day", "night", lockScreenWallpaperSyncEnabled = lockSyncEnabled)
			advanceUntilIdle()
		}
		assertEquals(listOf(true, false, true), destinations)
	}

	@Test
	fun `pending destination changes coalesce to home only`() = runTest {
		val destinations = mutableListOf<Boolean>()
		val scheduler = WallpaperUpdateScheduler(
			scope = this,
			dispatcher = StandardTestDispatcher(testScheduler),
			applyWallpaper = { _, _, _, _, lockSyncEnabled ->
				destinations += lockSyncEnabled
				true
			}
		)

		scheduler.schedule(false, "day", "night", lockScreenWallpaperSyncEnabled = true)
		scheduler.schedule(false, "day", "night", lockScreenWallpaperSyncEnabled = false)
		advanceUntilIdle()
		assertEquals(listOf(false), destinations)
	}

	@Test
	fun `returning to the previous destination replaces a different pending request`() = runTest {
		val destinations = mutableListOf<Boolean>()
		val scheduler = WallpaperUpdateScheduler(
			scope = this,
			dispatcher = StandardTestDispatcher(testScheduler),
			applyWallpaper = { _, _, _, _, lockSyncEnabled ->
				destinations += lockSyncEnabled
				true
			}
		)
		scheduler.schedule(false, "day", "night", lockScreenWallpaperSyncEnabled = false)
		advanceUntilIdle()
		scheduler.schedule(false, "day", "night", lockScreenWallpaperSyncEnabled = true)
		scheduler.schedule(false, "day", "night", lockScreenWallpaperSyncEnabled = false)
		advanceUntilIdle()
		assertEquals(listOf(false, false), destinations)
	}

	@Test
	fun `excluding lock screen updates an already queued wallpaper change`() = runTest {
		val destinations = mutableListOf<Boolean>()
		val scheduler = WallpaperUpdateScheduler(
			scope = this,
			dispatcher = StandardTestDispatcher(testScheduler),
			applyWallpaper = { _, _, _, _, lockSyncEnabled ->
				destinations += lockSyncEnabled
				true
			}
		)
		scheduler.schedule(false, "day", "night", lockScreenWallpaperBlurEnabled = true, lockScreenWallpaperSyncEnabled = true)
		scheduler.excludeLockScreenFromPendingRequests()
		advanceUntilIdle()
		assertEquals(listOf(false), destinations)

		scheduler.excludeLockScreenFromPendingRequests()
		scheduler.schedule(false, "day", "night", lockScreenWallpaperBlurEnabled = true, lockScreenWallpaperSyncEnabled = true)
		advanceUntilIdle()
		assertEquals(listOf(false, true), destinations)
	}

	@Test
	fun `request matching active destination cancels a different pending destination`() = runTest {
		val destinations = mutableListOf<Boolean>()
		lateinit var scheduler: WallpaperUpdateScheduler
		scheduler = WallpaperUpdateScheduler(
			scope = this,
			dispatcher = StandardTestDispatcher(testScheduler),
			applyWallpaper = { _, _, _, _, lockSyncEnabled ->
				destinations += lockSyncEnabled
				scheduler.schedule(false, "day", "night", lockScreenWallpaperSyncEnabled = true)
				scheduler.schedule(false, "day", "night", lockScreenWallpaperSyncEnabled = false)
				true
			}
		)
		scheduler.schedule(false, "day", "night", lockScreenWallpaperSyncEnabled = false)
		advanceUntilIdle()
		assertEquals(listOf(false), destinations)
	}

	@Test
	fun `allows retry after a failed wallpaper update`() = runTest {
		var applyCalls = 0
		val scheduler = WallpaperUpdateScheduler(
			scope = this,
			dispatcher = StandardTestDispatcher(testScheduler),
			applyWallpaper = { _, _, _, _, _ ->
				applyCalls++
				applyCalls > 1
			}
		)

		scheduler.schedule(isDark = false, dayUri = "day", nightUri = "night")
		advanceUntilIdle()
		scheduler.schedule(isDark = false, dayUri = "day", nightUri = "night")
		advanceUntilIdle()

		assertEquals(2, applyCalls)
	}
}
