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

package dev.lexip.hecate.analytics

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import dev.lexip.hecate.util.InstallSourceChecker

/**
 * Controls whether analytics collection is enabled.
 * - App is not debuggable (release-like)
 * - App is installed from Play Store
 */
object AnalyticsGate {
	@Volatile
	private var enabled = false

	@Volatile
	private var playStoreInstall = false

	fun init(context: Context) {
		playStoreInstall = InstallSourceChecker.isInstalledFromPlayStore(context)
		enabled = playStoreInstall
		FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enabled)
	}

	fun allowed(): Boolean = enabled

	fun isPlayStoreInstall(): Boolean = playStoreInstall
}
