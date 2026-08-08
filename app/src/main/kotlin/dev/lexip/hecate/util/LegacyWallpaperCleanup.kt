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

internal const val CURRENT_WALLPAPER_STORAGE_VERSION = 1

internal enum class LegacyWallpaperCleanupAction {
	NONE,
	MARK_CURRENT,
	RESET_LEGACY_SELECTION
}

/**
 * Detects selections made before wallpapers were copied into app-private storage.
 * New installations are marked current before users can pick a wallpaper.
 */
internal fun legacyWallpaperCleanupAction(
	storageVersion: Int,
	dayWallpaperUri: String?,
	nightWallpaperUri: String?
): LegacyWallpaperCleanupAction {
	if (storageVersion >= CURRENT_WALLPAPER_STORAGE_VERSION) {
		return LegacyWallpaperCleanupAction.NONE
	}
	val hasLegacyUri = listOf(dayWallpaperUri, nightWallpaperUri)
		.any { uri -> !uri.isNullOrBlank() && !uri.startsWith("file:", ignoreCase = true) }
	return if (hasLegacyUri) {
		LegacyWallpaperCleanupAction.RESET_LEGACY_SELECTION
	} else {
		LegacyWallpaperCleanupAction.MARK_CURRENT
	}
}
