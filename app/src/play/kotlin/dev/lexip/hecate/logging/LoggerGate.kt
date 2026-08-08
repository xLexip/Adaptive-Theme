/*
 * Copyright (C) 2025 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gnu.org/licenses/gpl-3.0
 *
 * Please see the License for specific terms regarding permissions and limitations.
 */

package dev.lexip.hecate.logging

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dev.lexip.hecate.BuildConfig
import dev.lexip.hecate.util.InstallSourceChecker

object LoggerGate {
	private const val TAG = "LoggerGate"

	@Volatile
	private var analyticsEnabled = false

	@Volatile
	private var crashlyticsEnabled = false

	@Volatile
	private var isPlayStoreInstall = false

	private val EXCLUDED_ANDROID_IDS = listOf(
		"484b0d1ef56c9ab8"
	)

	@SuppressLint("HardwareIds")
	fun init(context: Context) {
		val firebaseApp = runCatching { FirebaseApp.initializeApp(context) }
			.onFailure { exception ->
				Log.e(TAG, "Firebase initialization failed; disabling telemetry", exception)
			}
			.getOrNull()
		if (firebaseApp == null) {
			Log.e(TAG, "Firebase configuration is unavailable; disabling telemetry")
			analyticsEnabled = false
			crashlyticsEnabled = false
			isPlayStoreInstall = false
			return
		}

		isPlayStoreInstall = InstallSourceChecker.fromPlayStore(context)

		val androidId =
			Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
		val isExcludedDevice = androidId in EXCLUDED_ANDROID_IDS
		val isDebug = BuildConfig.DEBUG
		val isPlayConsoleTest = android.os.Build.MODEL == "OnePlus8Pro" // for whatever reason

		analyticsEnabled = !isDebug && isPlayStoreInstall && !isPlayConsoleTest && !isExcludedDevice
		crashlyticsEnabled = !isDebug && isPlayStoreInstall && !isPlayConsoleTest

		// Apply settings to Firebase
		FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(analyticsEnabled)
		FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = crashlyticsEnabled
	}

	fun allowed(): Boolean = analyticsEnabled

}

