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

private const val DAYS_FOR_IMMEDIATE_UPDATE = 3
private const val MIN_PRIORITY_FOR_IMMEDIATE = 0
private const val DAYS_FOR_FLEXIBLE_UPDATE = 1
private const val MIN_PRIORITY_FOR_FLEXIBLE = 0

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
						AnalyticsLogger.logInAppUpdateInstalled(activity)
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

	fun checkAndLaunchUpdate(
		onNoUpdate: () -> Unit = {},
		onError: (Throwable) -> Unit = {}
	) {
		if (!AnalyticsGate.isPlayStoreInstall()) return

		val launcher = updateLauncher
		if (launcher == null) {
			Log.w(TAG, "checkAndLaunchUpdate called before launcher was registered")
			return
		}
		val manager = appUpdateManager ?: return

		manager.appUpdateInfo
			.addOnSuccessListener { appUpdateInfo ->
				val availability = appUpdateInfo.updateAvailability()
				if (availability != UpdateAvailability.UPDATE_AVAILABLE) {
					Log.d(TAG, "No update available: availability=$availability")
					onNoUpdate()
					return@addOnSuccessListener
				}

				val staleness = appUpdateInfo.clientVersionStalenessDays()
				val priority = appUpdateInfo.updatePriority()
				val isImmediateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
				val isFlexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

				val meetsImmediateStaleness =
					staleness != null && staleness >= DAYS_FOR_IMMEDIATE_UPDATE
				val meetsFlexibleStaleness =
					staleness != null && staleness >= DAYS_FOR_FLEXIBLE_UPDATE
				val meetsImmediatePriority = priority >= MIN_PRIORITY_FOR_IMMEDIATE
				val meetsFlexiblePriority = priority >= MIN_PRIORITY_FOR_FLEXIBLE

				Log.d(
					TAG,
					"Update info: availability=$availability, staleness=$staleness, " +
							"priority=$priority, immediateAllowed=$isImmediateAllowed, flexibleAllowed=$isFlexibleAllowed"
				)

				val immediateEligible =
					isImmediateAllowed && meetsImmediateStaleness && meetsImmediatePriority
				val flexibleEligible =
					isFlexibleAllowed && meetsFlexibleStaleness && meetsFlexiblePriority

				when {
					// Prefer immediate when both are eligible
					immediateEligible -> {
						Log.i(TAG, "Immediate in-app update eligible; starting update flow")
						launchImmediateUpdate(manager, appUpdateInfo, launcher, onError)
					}

					flexibleEligible -> {
						Log.i(TAG, "Flexible in-app update eligible; starting update flow")
						launchFlexibleUpdate(manager, appUpdateInfo, launcher, onError)
					}

					else -> {
						Log.d(
							TAG,
							"Update available but not eligible: " +
									"staleness=$staleness, priority=$priority, " +
									"meetsImmediateStaleness=$meetsImmediateStaleness, " +
									"meetsFlexibleStaleness=$meetsFlexibleStaleness, " +
									"meetsImmediatePriority=$meetsImmediatePriority, " +
									"meetsFlexiblePriority=$meetsFlexiblePriority"
						)
						onNoUpdate()
					}
				}
			}
			.addOnFailureListener { throwable ->
				Log.e(TAG, "Failed to retrieve appUpdateInfo", throwable)
				onError(throwable)
			}
	}

	fun checkForImmediateUpdate(
		onNoUpdate: () -> Unit = {},
		onError: (Throwable) -> Unit = {}
	) {
		checkAndLaunchUpdate(onNoUpdate, onError)
	}

	fun checkForFlexibleUpdate(
		onNoUpdate: () -> Unit = {},
		onError: (Throwable) -> Unit = {}
	) {
		checkAndLaunchUpdate(onNoUpdate, onError)
	}

	private fun launchImmediateUpdate(
		manager: AppUpdateManager,
		appUpdateInfo: com.google.android.play.core.appupdate.AppUpdateInfo,
		launcher: ActivityResultLauncher<IntentSenderRequest>,
		onError: (Throwable) -> Unit
	) {
		try {
			manager.startUpdateFlowForResult(
				appUpdateInfo,
				launcher,
				AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
			)
		} catch (t: Throwable) {
			Log.e(TAG, "Failed to launch immediate in-app update", t)
			onError(t)
		}
	}

	private fun launchFlexibleUpdate(
		manager: AppUpdateManager,
		appUpdateInfo: com.google.android.play.core.appupdate.AppUpdateInfo,
		launcher: ActivityResultLauncher<IntentSenderRequest>,
		onError: (Throwable) -> Unit
	) {
		try {
			manager.startUpdateFlowForResult(
				appUpdateInfo,
				launcher,
				AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
			)
		} catch (t: Throwable) {
			Log.e(TAG, "Failed to launch flexible in-app update", t)
			onError(t)
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

	fun resumeFlexibleUpdateIfNeeded() {
		if (!AnalyticsGate.isPlayStoreInstall()) {
			return
		}
		val launcher = updateLauncher
		if (launcher == null) {
			Log.w(TAG, "resumeFlexibleUpdateIfNeeded called before launcher was registered")
			return
		}
		val manager = appUpdateManager ?: return

		manager.appUpdateInfo
			.addOnSuccessListener { appUpdateInfo ->
				if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
					appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
				) {
					Log.i(TAG, "Resuming in-progress flexible in-app update")
					try {
						appUpdateManager.startUpdateFlowForResult(
							appUpdateInfo,
							launcher,
							AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
						)
					} catch (t: Throwable) {
						Log.e(TAG, "Failed to resume flexible in-app update", t)
					}
				}
			}
			.addOnFailureListener { throwable ->
				Log.e(TAG, "Failed to check for in-progress flexible update", throwable)
			}
	}
}
