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

import android.app.ActivityManager
import android.content.Context
import android.util.Log

private const val TAG = "AppTaskRecentsController"

/**
 * Toggles whether the app's own task is listed in the system Recents screen.
 *
 * Excluding the task affects only the overview entry: the process and any running
 * foreground service keep working in the background.
 */
class AppTaskRecentsController(
	private val context: Context
) {

	private var lastAppliedExcludeFromRecents: Boolean? = null

	/**
	 * Applies [excludeFromRecents] to the task that hosts this app.
	 *
	 * Safe to call repeatedly and from the main thread: an unchanged value is a no-op
	 * and platform failures are logged instead of propagated by this bypass path.
	 */
	fun applyExcludeFromRecents(excludeFromRecents: Boolean) {
		if (lastAppliedExcludeFromRecents == excludeFromRecents) return
		if (requestExcludeFromRecents(excludeFromRecents)) {
			lastAppliedExcludeFromRecents = excludeFromRecents
		}
	}

	/**
	 * @return true when every app task accepted the new value, so it is safe to cache it.
	 */
	private fun requestExcludeFromRecents(excludeFromRecents: Boolean): Boolean {
		val activityManager = context.getSystemService(ActivityManager::class.java)
		if (activityManager == null) {
			Log.w(TAG, "ActivityManager unavailable; cannot change Recents visibility.")
			return true
		}
		val appTasks = try {
			activityManager.appTasks
		} catch (e: Exception) {
			Log.w(TAG, "Failed to read app tasks; cannot change Recents visibility.", e)
			return true
		}
		var allSucceeded = true
		appTasks.forEach { appTask ->
			try {
				appTask.setExcludeFromRecents(excludeFromRecents)
			} catch (e: Exception) {
				allSucceeded = false
				Log.w(TAG, "Failed to update Recents visibility for an app task.", e)
			}
		}
		return allSucceeded
	}
}
