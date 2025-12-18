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
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.lexip.hecate.Application
import dev.lexip.hecate.R
import dev.lexip.hecate.broadcasts.ScreenOnReceiver
import dev.lexip.hecate.data.UserPreferencesRepository
import dev.lexip.hecate.util.DarkThemeHandler
import dev.lexip.hecate.util.LightSensorManager
import dev.lexip.hecate.util.ProximitySensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "BroadcastReceiverService"
private const val NOTIFICATION_CHANNEL_ID = "ForegroundServiceChannel"
private const val ACTION_PAUSE_SERVICE = "dev.lexip.hecate.action.STOP_SERVICE"
internal const val EXTRA_ENABLE_MONITORING = "dev.lexip.hecate.extra.ENABLE_MONITORING"

private var screenOnReceiver: ScreenOnReceiver? = null

class BroadcastReceiverService : Service() {

	// Utils
	private lateinit var darkThemeHandler: DarkThemeHandler
	private lateinit var lightSensorManager: LightSensorManager
	private lateinit var proximitySensorManager: ProximitySensorManager

	// Service-bound scope
	private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)

		if (intent == null) {
			Log.w(
				TAG,
				"onStartCommand called with null intent; likely system restart. Proceeding safely."
			)
		}

		// Initialize data store
		val dataStore = (this.applicationContext as Application).userPreferencesDataStore

		// Handle stop action from notification
		if (intent?.action == ACTION_PAUSE_SERVICE) {
			Log.i(TAG, "Pause action received from notification.")
			serviceScope.launch {
				Log.i(TAG, "Adaptive theme paused/killed via notification action.")
				stopForeground(STOP_FOREGROUND_REMOVE)
				stopSelf()
			}
			return START_NOT_STICKY
		}

		Log.i(TAG, "Service starting...")
		initializeUtils()

		// Start foreground immediately to comply with O+ requirements
		createNotificationChannel()
		val initialNotification = buildNotification()
		try {
			startForeground(
				1,
				initialNotification,
				ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
			)
		} catch (e: Exception) {
			/**
			 *  Catch required because some Android 14 ROMs (HyperOS/MIUI) are broken
			 * 	and throw false-positive SecurityExceptions for valid FGS types.
			 */
			startForeground(1, initialNotification)
		}


		// Load user preferences from data store
		serviceScope.launch {
			val userPreferencesRepository = UserPreferencesRepository(dataStore)
			val userPreferences = userPreferencesRepository.fetchInitialPreferences()

			// Create screen-on receiver if adaptive theme is enabled
			val forceEnable = intent?.getBooleanExtra(EXTRA_ENABLE_MONITORING, false) == true
			if (userPreferences.adaptiveThemeEnabled || forceEnable) {
				createScreenOnReceiver(userPreferences.adaptiveThemeThresholdLux)
			}

			// Abort service start when there is no receiver to handle
			if (screenOnReceiver == null) {
				Log.d(TAG, "No receiver to handle, stopping foreground and self.")
				stopForeground(STOP_FOREGROUND_REMOVE)
				stopSelf()
			}
		}

		// Collect preference updates while service runs
		serviceScope.launch {
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
			try {
				unregisterReceiver(it)
			} catch (e: IllegalArgumentException) {
				Log.w(TAG, "Receiver was not registered or already unregistered.", e)
			}
		}
		screenOnReceiver = null
		serviceScope.cancel()
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

		// Build notification
		val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
			.setContentTitle(getString(R.string.app_name))
			.setContentText(getString(R.string.description_notification_service_running))
			.setCategory(Notification.CATEGORY_SERVICE)
			.setSmallIcon(R.drawable.ic_app)
			.setOnlyAlertOnce(true)
			.setContentIntent(pendingIntent)
			.addAction(disableAction)
			.setOngoing(true)


		val notification = builder.build()
		notification.flags =
			notification.flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR or Notification.FLAG_FOREGROUND_SERVICE

		return notification
	}

	private fun createNotificationChannel() {
		val serviceChannel = NotificationChannel(
			NOTIFICATION_CHANNEL_ID,
			getString(R.string.title_notification_channel_service),
			NotificationManager.IMPORTANCE_DEFAULT
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

		// Log proximity sensor availability for debugging and transparency
		if (proximitySensorManager.hasProximitySensor) {
			Log.d(
				TAG,
				"Proximity sensor detected; using proximity + light sensors for adaptive theme."
			)
		} else {
			Log.w(
				TAG,
				"No proximity sensor detected; using light sensor only for adaptive theme."
			)
		}
	}

}