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
import android.content.pm.PackageManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import dev.lexip.hecate.Application
import dev.lexip.hecate.data.UserPreferencesRepository
import dev.lexip.hecate.logging.Logger
import dev.lexip.hecate.util.DefaultDispatcherProvider
import dev.lexip.hecate.util.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val serviceScope = CoroutineScope(SupervisorJob() + DefaultDispatcherProvider.main)
private const val TAG = "QuickSettingsTileService"

class QuickSettingsTileService : TileService() {

	private val dispatchers: DispatcherProvider = DefaultDispatcherProvider

	private var listeningJob: Job? = null
	private var toggleJob: Job? = null

	private fun hasWriteSecureSettingsPermission(): Boolean {
		return packageManager.checkPermission(
			Manifest.permission.WRITE_SECURE_SETTINGS,
			packageName
		) == PackageManager.PERMISSION_GRANTED
	}

	override fun onTileAdded() {
		super.onTileAdded()
		Logger.logQuickSettingsTileAdded(applicationContext)
	}

	override fun onStartListening() {
		super.onStartListening()
		val tile = qsTile ?: return

		if (!hasWriteSecureSettingsPermission()) {
			tile.state = QuickSettingsStatePolicy.from(
				hasPermission = false,
				adaptiveThemeEnabled = false
			).toTileState()
			tile.updateTile()
			return
		}

		listeningJob?.cancel()
		listeningJob = serviceScope.launch {
			// Load user preference and set tile state
			val dataStore = (applicationContext as Application).userPreferencesDataStore
			val repo = UserPreferencesRepository(dataStore)
			val prefs = repo.fetchInitialPreferences()
			tile.state = QuickSettingsStatePolicy.from(
				hasPermission = true,
				adaptiveThemeEnabled = prefs.adaptiveThemeEnabled
			).toTileState()
			tile.updateTile()
		}
	}

	override fun onStopListening() {
		super.onStopListening()
		listeningJob?.cancel()
	}

	override fun onClick() {
		super.onClick()
		val tile = qsTile ?: return

		listeningJob?.cancel()
		toggleJob?.cancel()

		// Toggle adaptive theme
		val isEnabled = tile.state == Tile.STATE_ACTIVE
		val newEnabled = !isEnabled

		// Update tile UI immediately
		tile.state = if (newEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
		tile.updateTile()

		toggleJob = serviceScope.launch(dispatchers.io) {
			val dataStore = (applicationContext as Application).userPreferencesDataStore
			val repo = UserPreferencesRepository(dataStore)
			val coordinator = QuickSettingsToggleCoordinator(
				preferences = repo,
				serviceController = AndroidAdaptiveThemeServiceController(applicationContext)
			)

			when (val result = coordinator.toggle(currentlyEnabled = isEnabled)) {
				QuickSettingsToggleResult.Enabled -> {
					Logger.logServiceEnabled(
						applicationContext,
						source = "quick_settings_tile"
					)
				}

				QuickSettingsToggleResult.Disabled -> {
					Logger.logServiceDisabled(
						applicationContext,
						source = "quick_settings_tile"
					)
				}

				is QuickSettingsToggleResult.StartFailed -> {
					Log.e(TAG, "Failed to start service", result.cause)
					launch(dispatchers.main) {
						tile.state = Tile.STATE_INACTIVE
						tile.updateTile()
					}
					return@launch
				}
			}
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		listeningJob?.cancel()
		toggleJob?.cancel()
	}

}

private fun QuickSettingsState.toTileState(): Int = when (this) {
	QuickSettingsState.UNAVAILABLE -> Tile.STATE_UNAVAILABLE
	QuickSettingsState.INACTIVE -> Tile.STATE_INACTIVE
	QuickSettingsState.ACTIVE -> Tile.STATE_ACTIVE
}
