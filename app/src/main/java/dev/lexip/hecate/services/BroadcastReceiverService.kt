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

package dev.lexip.hecate.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.lexip.hecate.HecateApplication
import dev.lexip.hecate.R
import dev.lexip.hecate.analytics.AnalyticsLogger
import dev.lexip.hecate.broadcasts.ScreenOnReceiver
import dev.lexip.hecate.data.UserPreferencesRepository
import dev.lexip.hecate.util.DarkThemeHandler
import dev.lexip.hecate.util.LightSensorManager
import dev.lexip.hecate.util.ProximitySensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "BroadcastReceiverService"
private const val NOTIFICATION_CHANNEL_ID = "ForegroundServiceChannel"
private const val ACTION_STOP_SERVICE = "dev.lexip.hecate.action.STOP_SERVICE"

private var screenOnReceiver: ScreenOnReceiver? = null

private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

class BroadcastReceiverService : Service() {

	// Utils
	private lateinit var darkThemeHandler: DarkThemeHandler
	private lateinit var lightSensorManager: LightSensorManager
	private lateinit var proximitySensorManager: ProximitySensorManager

	override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)

		// Initialize data store
		val dataStore = (this.applicationContext as HecateApplication).userPreferencesDataStore

		// Handle stop action from notification
		if (intent.action == ACTION_STOP_SERVICE) {
			Log.i(
				TAG,
				"Disable action received from notification - disabling adaptive theme and stopping service..."
			)
			applicationScope.launch {
				try {
					val userPreferencesRepository = UserPreferencesRepository(dataStore)
					userPreferencesRepository.updateAdaptiveThemeEnabled(false)
					Log.i(TAG, "Adaptive theme disabled via notification action.")
					AnalyticsLogger.logServiceDisabled(
						applicationContext,
						source = "notification_action"
					)
				} catch (e: Exception) {
					Log.e(TAG, "Failed to update adaptive theme preference", e)
				}
				stopForeground(STOP_FOREGROUND_REMOVE)
				stopSelf()
			}
			return START_NOT_STICKY
		}

		Log.i(TAG, "Service starting...")
		initializeUtils()

		// Load user preferences from data store
		applicationScope.launch {
			val userPreferencesRepository = UserPreferencesRepository(dataStore)
			val userPreferences = userPreferencesRepository.fetchInitialPreferences()

			// Create screen-on receiver if adaptive theme is enabled
			if (userPreferences.adaptiveThemeEnabled) {
				createScreenOnReceiver(userPreferences.adaptiveThemeThresholdLux)
			}

			// Abort service start when there is no receiver to handle
			if (screenOnReceiver == null) {
				Log.d(TAG, "No receiver to handle, service start aborted.")
				stopSelf()
			} else {
				// Create service notification and channel
				createNotificationChannel()
				val notification = buildNotification()

				// Start the service in the foreground
				startForeground(1, notification)
			}
		}

		// Collect preference updates while service runs
		applicationScope.launch {
			val userPreferencesRepository = UserPreferencesRepository(dataStore)
			userPreferencesRepository.userPreferencesFlow.collect { prefs ->
				screenOnReceiver?.adaptiveThemeThresholdLux = prefs.adaptiveThemeThresholdLux
			}
		}

		return START_STICKY
	}

	override fun onDestroy() {
		super.onDestroy()
		Log.i(TAG, "Service is being destroyed...")
		screenOnReceiver?.let {
			Log.d(TAG, "Unregistering screen-on receiver...")
			unregisterReceiver(it)
		}
	}

	override fun onBind(intent: Intent?): IBinder? {
		return null
	}

	private fun buildNotification(): Notification {
		// Intent to the notification settings
		val settingsIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
			.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
			.putExtra(Settings.EXTRA_CHANNEL_ID, NOTIFICATION_CHANNEL_ID)
		val pendingIntent =
			PendingIntent.getActivity(this, 0, settingsIntent, PendingIntent.FLAG_IMMUTABLE)

		// Create action to disable the notification
		val disableAction = NotificationCompat.Action.Builder(
			0,
			getString(R.string.action_disable_notification),
			pendingIntent
		).build()

		// Create action to stop the service
		val stopIntent = Intent(this, BroadcastReceiverService::class.java).apply {
			action = ACTION_STOP_SERVICE
		}
		val stopPendingIntent = PendingIntent.getService(
			this,
			0,
			stopIntent,
			PendingIntent.FLAG_IMMUTABLE
		)
		val stopAction = NotificationCompat.Action.Builder(
			0,
			getString(R.string.action_stop_service),
			stopPendingIntent
		).build()

		// Build notification
		val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
			.setContentTitle(getString(R.string.app_name))
			.setContentText(getString(R.string.description_notification_service_running))
			.setCategory(Notification.CATEGORY_SERVICE)
			.setSmallIcon(R.drawable.ic_app)
			.setOnlyAlertOnce(true)
			.setContentIntent(pendingIntent)
			.addAction(disableAction)
			.addAction(stopAction)
			.setOngoing(true)
			.setPriority(NotificationCompat.PRIORITY_MIN)


		val notification = builder.build()
		notification.flags =
			notification.flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR or Notification.FLAG_FOREGROUND_SERVICE

		return notification
	}

	private fun createNotificationChannel() {
		val serviceChannel = NotificationChannel(
			NOTIFICATION_CHANNEL_ID,
			getString(R.string.title_notification_channel_service),
			NotificationManager.IMPORTANCE_MIN
		)

		serviceChannel.setSound(null, null) // Silent

		val manager = getSystemService(NotificationManager::class.java)
		manager?.createNotificationChannel(serviceChannel)
	}

	private fun createScreenOnReceiver(adaptiveThemeThresholdLux: Float) {
		screenOnReceiver = screenOnReceiver ?: ScreenOnReceiver(
			proximitySensorManager,
			lightSensorManager,
			darkThemeHandler,
			adaptiveThemeThresholdLux
		)
		Log.d(TAG, "Registering screen-on receiver...")
		registerReceiver(screenOnReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
	}

	private fun initializeUtils() {
		if (!this::darkThemeHandler.isInitialized)
			darkThemeHandler = DarkThemeHandler(this)
		if (!this::lightSensorManager.isInitialized)
			lightSensorManager = LightSensorManager(this)
		if (!this::proximitySensorManager.isInitialized)
			proximitySensorManager = ProximitySensorManager(this)
	}

}