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

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.lexip.hecate.HecateApplication
import dev.lexip.hecate.data.UserPreferencesRepository
import dev.lexip.hecate.services.BroadcastReceiverService
import dev.lexip.hecate.ui.theme.HecateTheme
import dev.lexip.hecate.util.DarkThemeHandler
import dev.lexip.hecate.util.InstallSourceChecker

class MainActivity : ComponentActivity() {

	private var inAppUpdateManager: InAppUpdateManager? = null
	private lateinit var adaptiveThemeViewModel: AdaptiveThemeViewModel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		installSplashScreen()
		enableEdgeToEdge()

		// Catch mysterious unsupported SDK versions despite minSDK 31
		@SuppressLint("ObsoleteSdkInt")
		if (Build.VERSION.SDK_INT < 31) {
			Toast.makeText(
				this,
				"Unsupported Android version, please uninstall the app.",
				Toast.LENGTH_LONG
			).show()
			finish()
			return
		}


		val isPlayInstall = InstallSourceChecker.isInstalledFromPlayStore(this)
		if (isPlayInstall) {
			inAppUpdateManager = InAppUpdateManager(this).also { manager ->
				manager.registerUpdateLauncher(this)
			}
		}

		// Obtain a stable ViewModel instance
		val dataStore = (this.applicationContext as HecateApplication).userPreferencesDataStore
		adaptiveThemeViewModel = androidx.lifecycle.ViewModelProvider(
			this,
			AdaptiveThemeViewModelFactory(
				this.application as HecateApplication,
				UserPreferencesRepository(dataStore),
				DarkThemeHandler(applicationContext)
			)
		)[AdaptiveThemeViewModel::class.java]

		setContent {
			val state by adaptiveThemeViewModel.uiState.collectAsState()

			HecateTheme {
				AdaptiveThemeScreen(
					state
				)
			}
		}

		inAppUpdateManager?.checkForImmediateUpdate()
	}

	override fun onResume() {
		super.onResume()
		if (this::adaptiveThemeViewModel.isInitialized) {
			adaptiveThemeViewModel.startSensorsIfEnabled()

			// Always restart the service (it may have been paused in the meantime)
			if (adaptiveThemeViewModel.isAdaptiveThemeEnabled()) {
				val intent = android.content.Intent(this, BroadcastReceiverService::class.java)
				androidx.core.content.ContextCompat.startForegroundService(this, intent)
			}
		}
		inAppUpdateManager?.resumeImmediateUpdateIfNeeded()
	}

	override fun onPause() {
		if (this::adaptiveThemeViewModel.isInitialized) {
			adaptiveThemeViewModel.stopSensors()
		}
		super.onPause()
	}
}
