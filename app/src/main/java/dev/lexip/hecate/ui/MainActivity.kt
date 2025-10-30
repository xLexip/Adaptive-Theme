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

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.lexip.hecate.HecateApplication
import dev.lexip.hecate.data.UserPreferencesRepository
import dev.lexip.hecate.ui.theme.HecateTheme
import dev.lexip.hecate.util.DarkThemeHandler

class MainActivity : ComponentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		installSplashScreen()
		enableEdgeToEdge()

		setContent {
			val dataStore = (this.applicationContext as HecateApplication).userPreferencesDataStore
			val adaptiveThemeViewModel: AdaptiveThemeViewModel = viewModel(
				factory = AdaptiveThemeViewModelFactory(
					this.application as HecateApplication,
					UserPreferencesRepository(dataStore),
					DarkThemeHandler(applicationContext)
				)
			)
			val state by adaptiveThemeViewModel.uiState.collectAsState()

			val hasPermission = hasWriteSecureSettingsPermission()
			val copyAdbCommand: (String) -> Unit =
				{ adbCommand -> copyToClipboard("ADB Command", adbCommand) }

			HecateTheme {
				AdaptiveThemeScreen(
					state,
					adaptiveThemeViewModel::updateAdaptiveThemeEnabled,
					hasPermission,
					copyAdbCommand
				)
			}
		}

	}

	private fun hasWriteSecureSettingsPermission(): Boolean {
		val permission = Manifest.permission.WRITE_SECURE_SETTINGS
		return ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED
	}

	private fun copyToClipboard(label: String, text: String) {
		val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
		val clip = ClipData.newPlainText(label, text)
		clipboard.setPrimaryClip(clip)
	}

}
