/*
 * Copyright (C) 2025 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0
 *
 * Please see the License for specific terms regarding permissions and limitations.
 */

package dev.lexip.hecate.services

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import dev.lexip.hecate.Application
import dev.lexip.hecate.analytics.AnalyticsLogger
import dev.lexip.hecate.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

class QuickSettingsTileService : TileService() {

	private fun hasWriteSecureSettingsPermission(): Boolean {
		return packageManager.checkPermission(
			Manifest.permission.WRITE_SECURE_SETTINGS,
			packageName
		) == PackageManager.PERMISSION_GRANTED
	}

	override fun onTileAdded() {
		super.onTileAdded()
		AnalyticsLogger.logQuickSettingsTileAdded(applicationContext)
	}

	override fun onStartListening() {
		super.onStartListening()
		val tile = qsTile ?: return

		// No permission => tile unavailable
		if (!hasWriteSecureSettingsPermission()) {
			tile.state = Tile.STATE_UNAVAILABLE
			tile.updateTile()
			return
		}

		// Load user preference and set tile state
		val dataStore = (applicationContext as Application).userPreferencesDataStore
		val repo = UserPreferencesRepository(dataStore)

		serviceScope.launch {
			val prefs = repo.fetchInitialPreferences()
			tile.state = if (prefs.adaptiveThemeEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
			tile.updateTile()
		}
	}

	override fun onClick() {
		super.onClick()
		val tile = qsTile ?: return

		// No permission => tile unavailable
		if (!hasWriteSecureSettingsPermission()) {
			tile.state = Tile.STATE_UNAVAILABLE
			tile.updateTile()
			return
		}

		val dataStore = (applicationContext as Application).userPreferencesDataStore
		val repo = UserPreferencesRepository(dataStore)

		// Toggle adaptive theme
		serviceScope.launch {
			val prefs = repo.fetchInitialPreferences()
			val newEnabled = !prefs.adaptiveThemeEnabled

			repo.updateAdaptiveThemeEnabled(newEnabled)

			// Start/stop the service
			val intent = Intent(applicationContext, BroadcastReceiverService::class.java)
			if (newEnabled) {
				repo.ensureAdaptiveThemeThresholdDefault()
				ContextCompat.startForegroundService(applicationContext, intent)
				AnalyticsLogger.logServiceEnabled(
					applicationContext,
					source = "quick_settings_tile"
				)
			} else {
				applicationContext.stopService(intent)
				AnalyticsLogger.logServiceDisabled(
					applicationContext,
					source = "quick_settings_tile"
				)
			}

			// Update tile UI
			tile.state = if (newEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
			tile.updateTile()
		}
	}

}
