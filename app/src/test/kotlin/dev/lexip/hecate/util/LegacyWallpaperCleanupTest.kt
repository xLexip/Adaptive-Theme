/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0
 *
 * Please see the License for specific language governing permissions and
 * limitations under the License.
 */

package dev.lexip.hecate.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyWallpaperCleanupTest {
	@Test
	fun `resets old picker selections when storage has not been versioned`() {
		assertEquals(
			LegacyWallpaperCleanupAction.RESET_LEGACY_SELECTION,
			legacyWallpaperCleanupAction(
				storageVersion = 0,
				dayWallpaperUri = "content://media/picker/day",
				nightWallpaperUri = "content://media/picker/night"
			)
		)
	}

	@Test
	fun `marks a new user current without clearing an empty selection`() {
		assertEquals(
			LegacyWallpaperCleanupAction.MARK_CURRENT,
			legacyWallpaperCleanupAction(0, null, null)
		)
	}

	@Test
	fun `retains prepared wallpapers for users already on the new storage`() {
		assertEquals(
			LegacyWallpaperCleanupAction.MARK_CURRENT,
			legacyWallpaperCleanupAction(
				storageVersion = 0,
				dayWallpaperUri = "file:///data/user/0/dev.lexip.hecate/files/day_wallpaper.jpg",
				nightWallpaperUri = "file:///data/user/0/dev.lexip.hecate/files/night_wallpaper.jpg"
			)
		)
	}

	@Test
	fun `does not reset a user marked as current`() {
		assertEquals(
			LegacyWallpaperCleanupAction.NONE,
			legacyWallpaperCleanupAction(
				storageVersion = CURRENT_WALLPAPER_STORAGE_VERSION,
				dayWallpaperUri = "content://unexpected/source",
				nightWallpaperUri = null
			)
		)
	}
}
