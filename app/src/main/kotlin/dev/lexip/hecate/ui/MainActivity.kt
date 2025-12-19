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

package dev.lexip.hecate.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.lexip.hecate.Application
import dev.lexip.hecate.data.UserPreferencesRepository
import dev.lexip.hecate.services.BroadcastReceiverService
import dev.lexip.hecate.ui.navigation.NavigationManager
import dev.lexip.hecate.ui.theme.HecateTheme
import dev.lexip.hecate.util.DarkThemeHandler
import dev.lexip.hecate.util.InAppUpdateManager
import dev.lexip.hecate.util.InstallSourceChecker

class MainActivity : ComponentActivity() {

	private var inAppUpdateManager: InAppUpdateManager? = null
	private lateinit var mainViewModel: MainViewModel
	private val navigationManager = NavigationManager()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		installSplashScreen()
		enableEdgeToEdge()

		val isPlayInstall = InstallSourceChecker.fromPlayStore(this)
		if (isPlayInstall) {
			inAppUpdateManager = InAppUpdateManager(this).also { manager ->
				manager.registerUpdateLauncher(this)
			}
		}

		// Obtain a stable ViewModel instance
		val dataStore = (this.applicationContext as Application).userPreferencesDataStore
		mainViewModel = androidx.lifecycle.ViewModelProvider(
			this,
			MainViewModelFactory(
				this.application as Application,
				UserPreferencesRepository(dataStore),
				DarkThemeHandler(applicationContext)
			)
		)[MainViewModel::class.java]

		setContent {
			val state by mainViewModel.uiState.collectAsState()

			HecateTheme {
				AppNavHost(
					mainViewModel = mainViewModel,
					uiState = state,
					navigationManager = navigationManager
				)
			}
		}
		inAppUpdateManager?.checkAndLaunchUpdate()
	}

	override fun onResume() {
		super.onResume()

		inAppUpdateManager?.resumeImmediateUpdateIfNeeded()
		inAppUpdateManager?.resumeFlexibleUpdateIfNeeded()

		// Always restart the service (it may have been paused in the meantime)
		if (this::mainViewModel.isInitialized) {
			mainViewModel.startSensorsIfEnabled()
			if (mainViewModel.isAdaptiveThemeEnabled()) {
				val intent = android.content.Intent(this, BroadcastReceiverService::class.java)
				androidx.core.content.ContextCompat.startForegroundService(this, intent)
			}
		}
	}

	override fun onPause() {
		if (this::mainViewModel.isInitialized) {
			mainViewModel.stopSensors()
		}
		super.onPause()
	}
}
