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

package dev.lexip.hecate.util

import android.content.Context
import android.os.Build
import android.util.Log

object InstallSourceChecker {

	private const val TAG = "InstallSourceChecker"
	private const val PLAY_STORE_PACKAGE = "com.android.vending"

	/**
	 * Returns true only if the app was installed from the Google Play Store.
	 * On any error or unknown installer, this returns false to safely disable in-app updates.
	 */
	fun isInstalledFromPlayStore(context: Context): Boolean {
		val installer = getInstallerPackageName(context)
		val fromPlay = installer == PLAY_STORE_PACKAGE
		Log.d(TAG, "Installer package: $installer, isPlayStore=$fromPlay")
		return fromPlay
	}

	private fun getInstallerPackageName(context: Context): String? {
		val pm = context.packageManager
		val packageName = context.packageName
		return try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				pm.getInstallSourceInfo(packageName).installingPackageName
			} else {
				@Suppress("DEPRECATION")
				pm.getInstallerPackageName(packageName)
			}
		} catch (t: Throwable) {
			Log.w(TAG, "Failed to resolve installer package", t)
			null
		}
	}
}

