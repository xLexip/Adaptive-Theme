/*
 * Copyright (C) 2024-2025 xLexip <https://lexip.dev>
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

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import android.util.Log
import java.lang.ref.WeakReference

import dev.lexip.hecate.logging.Logger

private const val TAG = "WallpaperHandler"

/**
 * Handler for theme-synchronized wallpaper swapping.
 */
class WallpaperHandler(context: Context) {
	private val contextRef = WeakReference(context.applicationContext)

	private val wallpaperManager: WallpaperManager?
		get() = contextRef.get()?.let { WallpaperManager.getInstance(it) }

	/**
	 * Checks whether a live wallpaper is currently active.
	 */
	fun isLiveWallpaperActive(): Boolean {
		val wm = wallpaperManager ?: return false
		val isLive = wm.wallpaperInfo != null
		Log.d(TAG, "Live wallpaper active: $isLive")
		return isLive
	}

	/**
	 * Applies the corresponding theme wallpaper based on dark theme status.
	 * @param isDark True if target theme is dark, false for light.
	 * @param dayUriStr Content URI string for day wallpaper.
	 * @param nightUriStr Content URI string for night wallpaper.
	 * @return true if wallpaper was successfully applied, false otherwise.
	 */
	fun applyWallpaperForTheme(isDark: Boolean, dayUriStr: String?, nightUriStr: String?): Boolean {
		val context = contextRef.get() ?: return false
		val wm = wallpaperManager ?: return false

		val targetUriStr = if (isDark) nightUriStr else dayUriStr
		if (targetUriStr.isNullOrEmpty()) {
			Log.w(TAG, "Target wallpaper URI is missing (isDark=$isDark). Skipping wallpaper swap.")
			return false
		}

		val succeeded: Boolean = try {
			val uri = Uri.parse(targetUriStr)
			val stream = context.contentResolver.openInputStream(uri)
			if (stream != null) {
				stream.use { s ->
					// Future addition: allow separate home screen and lock screen wallpaper configuration
					wm.setStream(
						s,
						null,
						true,
						WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
					)
				}
				true
			} else {
				Log.w(TAG, "Could not open InputStream for URI: $targetUriStr")
				false
			}
		} catch (e: Exception) {
			Log.w(TAG, "Failed to set wallpaper from URI: $targetUriStr", e)
			false
		}

		if (succeeded) {
			Log.i(TAG, "Successfully set ${if (isDark) "night" else "day"} wallpaper from URI.")
		}
		Logger.logWallpaperSwitched(context, isDark = isDark, succeeded = succeeded)
		return succeeded
	}
}
