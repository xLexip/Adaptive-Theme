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

import android.app.UiModeManager
import android.content.Context
import android.provider.Settings.Secure
import android.util.Log
import dev.lexip.hecate.analytics.AnalyticsLogger
import java.lang.ref.WeakReference

private const val TAG = "DarkThemeHandler"
private const val SECURE_SETTINGS_KEY = "ui_night_mode"

/**
 * Handler for managing the system dark theme.
 */
class DarkThemeHandler(context: Context) {
	private val contextRef = WeakReference(context)
	private val contentResolver = contextRef.get()?.contentResolver
	private val uiModeManager =
		contextRef.get()?.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

	/**
	 * @return True if the system dark theme is enabled, false otherwise.
	 */
	fun isDarkThemeEnabled(): Boolean {
		val enabled =
			Secure.getInt(contentResolver, SECURE_SETTINGS_KEY) == UiModeManager.MODE_NIGHT_YES
		Log.d(TAG, "Device dark theme enabled: $enabled")
		return enabled
	}

	/**
	 * Set the system dark theme based on the given parameter.
	 * @param enable True to enable dark theme, false to disable.
	 * @throws SecurityException if android.permission.WRITE_SECURE_SETTINGS is not granted.
	 */
	fun setDarkTheme(enable: Boolean) {
		val context = contextRef.get() ?: run {
			Log.w(TAG, "Context reference lost, cannot change dark theme")
			return
		}
		val resolver = contentResolver
		if (resolver == null) {
			Log.w(TAG, "ContentResolver is null, cannot change dark theme")
			return
		}

		val targetMode = if (enable) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO

		var succeeded = false
		try {
			Log.i(TAG, "Setting dark theme to target mode: $targetMode")
			succeeded = Secure.putInt(resolver, SECURE_SETTINGS_KEY, targetMode)
			if (succeeded) {
				refreshUi()
			} else {
				Log.w(TAG, "Secure.putInt reported failure when changing dark theme")
			}
		} catch (e: SecurityException) {
			Log.e(TAG, "SecurityException while changing dark theme", e)
			succeeded = false
		} catch (e: Exception) {
			Log.e(TAG, "Unexpected exception while changing dark theme", e)
			succeeded = false
		}

		AnalyticsLogger.logThemeSwitched(
			context = context,
			targetMode = targetMode,
			succeeded = succeeded
		)
	}

	/**
	 * Refreshes the Android UI by briefly enabling and disabling car mode.
	 * While this is not a good practice the is the the only way to ensure
	 * that the a theme change is applied to the entire system UI.
	 * @since API 29 (Android 10)
	 * @see <a href="https://developer.android.com/reference/android/app/UiModeManager#setNightMode(int)">UiModeManager.setNightMode(int)</a>
	 */
	private fun refreshUi() {
		Log.d(TAG, "Refreshing system UI after dark theme change...")
		uiModeManager.enableCarMode(0)
		uiModeManager.disableCarMode(UiModeManager.DISABLE_CAR_MODE_GO_HOME)
	}
}