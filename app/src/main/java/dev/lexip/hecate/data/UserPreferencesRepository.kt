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

package dev.lexip.hecate.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val TAG = "UserPreferencesRepository"

data class UserPreferences(
	val adaptiveThemeEnabled: Boolean,
	val adaptiveThemeThresholdLux: Float,
	val customAdaptiveThemeThresholdLux: Float? = null,
	val permissionWizardCompleted: Boolean = false
)

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

	private object PreferencesKeys {
		val ADAPTIVE_THEME_ENABLED = booleanPreferencesKey("adaptive_theme_enabled")
		val ADAPTIVE_THEME_THRESHOLD_LUX = floatPreferencesKey("adaptive_theme_threshold_lux")
		val CUSTOM_ADAPTIVE_THEME_THRESHOLD_LUX =
			floatPreferencesKey("custom_adaptive_theme_threshold_lux")
		val PERMISSION_WIZARD_COMPLETED = booleanPreferencesKey("permission_wizard_completed")
	}

	val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
		.catch { exception ->
			// dataStore.data throws an IOException when an error is encountered when reading data
			if (exception is IOException) {
				Log.e(TAG, "Error reading user preferences.", exception)
				emit(emptyPreferences())
			} else {
				throw exception
			}
		}.map { preferences ->
			mapUserPreferences(preferences)
		}

	suspend fun fetchInitialPreferences() =
		mapUserPreferences(dataStore.data.first().toPreferences())


	suspend fun ensureAdaptiveThemeThresholdDefault(default: Float = AdaptiveThreshold.DAYLIGHT.lux) {
		dataStore.edit { preferences ->
			if (preferences[PreferencesKeys.ADAPTIVE_THEME_THRESHOLD_LUX] == null) {
				preferences[PreferencesKeys.ADAPTIVE_THEME_THRESHOLD_LUX] = default
			}
		}
	}

	private fun mapUserPreferences(preferences: Preferences): UserPreferences {
		val adaptiveThemeEnabled = preferences[PreferencesKeys.ADAPTIVE_THEME_ENABLED] == true
		val adaptiveThemeThresholdLux =
			preferences[PreferencesKeys.ADAPTIVE_THEME_THRESHOLD_LUX]
				?: AdaptiveThreshold.DAYLIGHT.lux
		val customAdaptiveThemeThresholdLux =
			preferences[PreferencesKeys.CUSTOM_ADAPTIVE_THEME_THRESHOLD_LUX]
		val permissionWizardCompleted =
			preferences[PreferencesKeys.PERMISSION_WIZARD_COMPLETED] == true
		return UserPreferences(
			adaptiveThemeEnabled = adaptiveThemeEnabled,
			adaptiveThemeThresholdLux = adaptiveThemeThresholdLux,
			customAdaptiveThemeThresholdLux = customAdaptiveThemeThresholdLux,
			permissionWizardCompleted = permissionWizardCompleted
		)
	}

	suspend fun updateAdaptiveThemeEnabled(enabled: Boolean) {
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.ADAPTIVE_THEME_ENABLED] = enabled
		}
	}

	suspend fun updateAdaptiveThemeThresholdLux(lux: Float) {
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.ADAPTIVE_THEME_THRESHOLD_LUX] = lux
			preferences.remove(PreferencesKeys.CUSTOM_ADAPTIVE_THEME_THRESHOLD_LUX)
		}
	}

	suspend fun updateCustomAdaptiveThemeThresholdLux(lux: Float) {
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.ADAPTIVE_THEME_THRESHOLD_LUX] = lux
			preferences[PreferencesKeys.CUSTOM_ADAPTIVE_THEME_THRESHOLD_LUX] = lux
		}
	}

	suspend fun clearCustomAdaptiveThemeThreshold() {
		dataStore.edit { preferences ->
			preferences.remove(PreferencesKeys.CUSTOM_ADAPTIVE_THEME_THRESHOLD_LUX)
		}
	}

	suspend fun updatePermissionWizardCompleted(completed: Boolean) {
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.PERMISSION_WIZARD_COMPLETED] = completed
		}
	}

}