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

package dev.lexip.hecate.ui

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dev.lexip.hecate.analytics.AnalyticsGate
import dev.lexip.hecate.analytics.AnalyticsLogger

private const val TAG = "InAppUpdateManager"
private const val DAYS_FOR_IMMEDIATE_UPDATE = 0
private const val MIN_PRIORITY_FOR_IMMEDIATE = 0

class InAppUpdateManager(activity: ComponentActivity) {

	private val appUpdateManager: AppUpdateManager? = if (AnalyticsGate.isPlayStoreInstall()) {
		AppUpdateManagerFactory.create(activity)
	} else null

	private var updateLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

	fun registerUpdateLauncher(activity: ComponentActivity) {
		if (updateLauncher != null) return
		if (!AnalyticsGate.isPlayStoreInstall()) {
			return
		}
		appUpdateManager ?: return

		updateLauncher =
			activity.registerForActivityResult(StartIntentSenderForResult()) { result ->
				when (result.resultCode) {
					Activity.RESULT_OK -> {
						Log.i(TAG, "In-app update completed successfully")
					}

					Activity.RESULT_CANCELED -> {
						Log.w(TAG, "In-app update was cancelled by the user")
						Toast.makeText(
							activity,
							"Update cancelled. You can update later from the Play Store.",
							Toast.LENGTH_SHORT
						).show()
					}

					else -> {
						Log.e(TAG, "In-app update failed with resultCode=${result.resultCode}")
						Toast.makeText(
							activity,
							"Update failed to start. Please try again later.",
							Toast.LENGTH_SHORT
						).show()
					}
				}
			}
	}

	fun checkForImmediateUpdate(
		onNoUpdate: () -> Unit = {},
		onError: (Throwable) -> Unit = {}
	) {
		if (!AnalyticsGate.isPlayStoreInstall()) {
			return
		}
		val launcher = updateLauncher
		if (launcher == null) {
			Log.w(TAG, "checkForImmediateUpdate called before launcher was registered")
			return
		}
		val manager = appUpdateManager ?: return

		manager.appUpdateInfo
			.addOnSuccessListener { appUpdateInfo ->
				val availability = appUpdateInfo.updateAvailability()
				val isImmediateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
				val staleness = appUpdateInfo.clientVersionStalenessDays() ?: -1
				val priority = appUpdateInfo.updatePriority()

				val meetsStaleness = staleness == -1 || staleness >= DAYS_FOR_IMMEDIATE_UPDATE
				val meetsPriority = priority >= MIN_PRIORITY_FOR_IMMEDIATE

				if (availability == UpdateAvailability.UPDATE_AVAILABLE && isImmediateAllowed && meetsStaleness && meetsPriority) {
					Log.i(TAG, "Immediate in-app update: starting update flow")
					try {
						appUpdateManager.startUpdateFlowForResult(
							appUpdateInfo,
							launcher,
							AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
						)
					} catch (t: Throwable) {
						Log.e(TAG, "Failed to launch immediate in-app update", t)
						onError(t)
					}
				} else {
					onNoUpdate()
				}
			}
			.addOnFailureListener { throwable ->
				Log.e(TAG, "Failed to retrieve appUpdateInfo", throwable)
				onError(throwable)
			}
	}

	fun resumeImmediateUpdateIfNeeded() {
		if (!AnalyticsGate.isPlayStoreInstall()) {
			return
		}
		val launcher = updateLauncher
		if (launcher == null) {
			Log.w(TAG, "resumeImmediateUpdateIfNeeded called before launcher was registered")
			return
		}
		val manager = appUpdateManager ?: return

		manager.appUpdateInfo
			.addOnSuccessListener { appUpdateInfo ->
				if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
					appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
				) {
					Log.i(TAG, "Resuming in-progress immediate in-app update")
					try {
						appUpdateManager.startUpdateFlowForResult(
							appUpdateInfo,
							launcher,
							AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
						)
					} catch (t: Throwable) {
						Log.e(TAG, "Failed to resume immediate in-app update", t)
					}
				}
			}
			.addOnFailureListener { throwable ->
				Log.e(TAG, "Failed to check for in-progress immediate update", throwable)
			}
	}
}
