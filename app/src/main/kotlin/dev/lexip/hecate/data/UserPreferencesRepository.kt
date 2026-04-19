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
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val TAG = "UserPreferencesRepository"
private const val DEFAULT_NIGHT_START_MINUTES = 21 * 60
private const val DEFAULT_NIGHT_END_MINUTES = 6 * 60

data class UserPreferences(
	val adaptiveThemeEnabled: Boolean,
	val adaptiveThemeThresholdLux: Float,
	val customAdaptiveThemeThresholdLux: Float? = null,
	val hasSetupCompleted: Boolean = false,
	val stayDarkAtNightEnabled: Boolean = false,
	val nightStartMinutes: Int = DEFAULT_NIGHT_START_MINUTES,
	val nightEndMinutes: Int = DEFAULT_NIGHT_END_MINUTES
)

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

	private object PreferencesKeys {
		val ADAPTIVE_THEME_ENABLED = booleanPreferencesKey("adaptive_theme_enabled")
		val ADAPTIVE_THEME_THRESHOLD_LUX = floatPreferencesKey("adaptive_theme_threshold_lux")
		val CUSTOM_ADAPTIVE_THEME_THRESHOLD_LUX =
			floatPreferencesKey("custom_adaptive_theme_threshold_lux")
		val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
		val STAY_DARK_AT_NIGHT_ENABLED = booleanPreferencesKey("stay_dark_at_night_enabled")
		val NIGHT_START_MINUTES = intPreferencesKey("night_start_minutes")
		val NIGHT_END_MINUTES = intPreferencesKey("night_end_minutes")
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

	suspend fun ensureNightDefaults(
		defaultStartMinutes: Int = DEFAULT_NIGHT_START_MINUTES,
		defaultEndMinutes: Int = DEFAULT_NIGHT_END_MINUTES
	) {
		dataStore.edit { preferences ->
			if (preferences[PreferencesKeys.NIGHT_START_MINUTES] == null) {
				preferences[PreferencesKeys.NIGHT_START_MINUTES] = defaultStartMinutes
			}
			if (preferences[PreferencesKeys.NIGHT_END_MINUTES] == null) {
				preferences[PreferencesKeys.NIGHT_END_MINUTES] = defaultEndMinutes
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
		val hasSetupCompleted =
			preferences[PreferencesKeys.SETUP_COMPLETED] == true
		val stayDarkAtNightEnabled = preferences[PreferencesKeys.STAY_DARK_AT_NIGHT_ENABLED] == true
		val nightStartMinutes =
			preferences[PreferencesKeys.NIGHT_START_MINUTES] ?: DEFAULT_NIGHT_START_MINUTES
		val nightEndMinutes =
			preferences[PreferencesKeys.NIGHT_END_MINUTES] ?: DEFAULT_NIGHT_END_MINUTES
		return UserPreferences(
			adaptiveThemeEnabled = adaptiveThemeEnabled,
			adaptiveThemeThresholdLux = adaptiveThemeThresholdLux,
			customAdaptiveThemeThresholdLux = customAdaptiveThemeThresholdLux,
			hasSetupCompleted = hasSetupCompleted,
			stayDarkAtNightEnabled = stayDarkAtNightEnabled,
			nightStartMinutes = nightStartMinutes,
			nightEndMinutes = nightEndMinutes
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


	suspend fun updateSetupCompleted(completed: Boolean) {
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.SETUP_COMPLETED] = completed
		}
	}

	suspend fun updateStayDarkAtNightEnabled(enabled: Boolean) {
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.STAY_DARK_AT_NIGHT_ENABLED] = enabled
		}
	}

	/**
	 * @return true when values are valid and persisted, false when rejected.
	 */
	suspend fun updateNightWindow(startMinutes: Int, endMinutes: Int): Boolean {
		if (!isValidMinute(startMinutes) || !isValidMinute(endMinutes) || startMinutes == endMinutes) {
			Log.w(
				TAG,
				"Rejected invalid night window update: start=$startMinutes, end=$endMinutes"
			)
			return false
		}

		dataStore.edit { preferences ->
			preferences[PreferencesKeys.NIGHT_START_MINUTES] = startMinutes
			preferences[PreferencesKeys.NIGHT_END_MINUTES] = endMinutes
		}
		return true
	}

	private fun isValidMinute(value: Int): Boolean = value in 0 until 24 * 60

}