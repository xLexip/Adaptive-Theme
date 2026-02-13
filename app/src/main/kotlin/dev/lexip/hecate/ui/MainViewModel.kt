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

import android.content.Intent
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.lexip.hecate.Application
import dev.lexip.hecate.data.AdaptiveThreshold
import dev.lexip.hecate.data.UserPreferencesRepository
import dev.lexip.hecate.logging.Logger
import dev.lexip.hecate.services.BroadcastReceiverService
import dev.lexip.hecate.util.DarkThemeHandler
import dev.lexip.hecate.util.InstallSourceChecker
import dev.lexip.hecate.util.LightSensorManager
import dev.lexip.hecate.util.ProximitySensorManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
private const val TAG = "MainViewModel"

sealed interface UiEvent

data class CopyToClipboard(val text: String) : UiEvent
data object NavigateToSetup : UiEvent

data class MainUiState(
	val adaptiveThemeEnabled: Boolean = false,
	val adaptiveThemeThresholdLux: Float = 1000f,
	val customAdaptiveThemeThresholdLux: Float? = null,
	val hasSetupCompleted: Boolean = false,
	val isDeviceCovered: Boolean = false,
	val isShizukuInstalled: Boolean = false,
	val isInstalledFromPlayStore: Boolean = false
)

class MainViewModel(
	private val application: Application,
	private val userPreferencesRepository: UserPreferencesRepository,
	private var _darkThemeHandler: DarkThemeHandler,
	private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
	private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

	private val _uiState = MutableStateFlow(MainUiState())
	val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

	fun setShizukuInstalled(installed: Boolean) {
		if (_uiState.value.isShizukuInstalled == installed) return
		_uiState.value = _uiState.value.copy(isShizukuInstalled = installed)
	}

	fun isAdaptiveThemeEnabled(): Boolean = _uiState.value.adaptiveThemeEnabled

	// One-shot UI events
	private val _uiEvents = MutableSharedFlow<UiEvent>(
		replay = 0,
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	)
	val uiEvents = _uiEvents.asSharedFlow()

	// Light Sensor
	private val lightSensorManager = LightSensorManager(application.applicationContext)
	private var isListeningToSensor = false

	private val _currentSensorLux = MutableStateFlow(0f)
	val currentSensorLuxFlow: StateFlow<Float> = _currentSensorLux.asStateFlow()
	val currentSensorLux: Float get() = _currentSensorLux.value

	fun updateCurrentSensorLux(lux: Float) {
		_currentSensorLux.value = lux
	}

	// Proximity Sensor
	private val proximitySensorManager = ProximitySensorManager(application.applicationContext)
	private var isListeningToProximity = false
	private var coveredJob: Job? = null

	private fun startProximityListening() {
		if (!proximitySensorManager.hasProximitySensor) {
			Log.w(
				TAG,
				"Proximity sensor not available; skipping proximity listening in MainViewModel."
			)
			if (_uiState.value.isDeviceCovered) {
				_uiState.value = _uiState.value.copy(isDeviceCovered = false)
			}
			return
		}

		if (isListeningToProximity) return
		isListeningToProximity = true
		proximitySensorManager.startListening({ distance: Float ->
			val covered = distance < 5f
			if (covered) {
				if (_uiState.value.isDeviceCovered || coveredJob?.isActive == true) return@startListening
				coveredJob = viewModelScope.launch {
					Log.d(TAG, "Proximity covered timer started")
					try {
						delay(1000)
						if (!_uiState.value.isDeviceCovered) {
							_uiState.value = _uiState.value.copy(isDeviceCovered = true)
							Log.d(TAG, "Proximity covered timer fired")
						}
					} finally {
						coveredJob = null
					}
				}
			} else {
				if (coveredJob?.isActive == true) {
					coveredJob?.cancel()
					coveredJob = null
					Log.d(TAG, "Proximity covered timer cancelled")
				}
				if (_uiState.value.isDeviceCovered) {
					_uiState.value = _uiState.value.copy(isDeviceCovered = false)
				}
			}
		}, sensorDelay = SensorManager.SENSOR_DELAY_UI)
	}

	private fun stopProximityListening() {
		if (!isListeningToProximity) return
		isListeningToProximity = false
		proximitySensorManager.stopListening()
		if (coveredJob?.isActive == true) {
			coveredJob?.cancel()
			coveredJob = null
			Log.d(TAG, "Proximity covered timer cancelled")
		}
		if (_uiState.value.isDeviceCovered) {
			_uiState.value = _uiState.value.copy(isDeviceCovered = false)
		}
	}

	fun startSensorsIfEnabled() {
		if (_uiState.value.adaptiveThemeEnabled) {
			startLightSensorListening()
			startProximityListening()
		}
	}

	fun stopSensors() {
		stopLightSensorListening()
		stopProximityListening()
	}

	private var customThresholdTemp: Float? = null

	init {
		viewModelScope.launch(ioDispatcher) {
			val fromPlayStore = InstallSourceChecker.fromPlayStore(application)
			_uiState.value = _uiState.value.copy(isInstalledFromPlayStore = fromPlayStore)
		}

		viewModelScope.launch {
			userPreferencesRepository.userPreferencesFlow.collect { userPreferences ->
				_uiState.value = _uiState.value.copy(
					adaptiveThemeEnabled = userPreferences.adaptiveThemeEnabled,
					adaptiveThemeThresholdLux = userPreferences.adaptiveThemeThresholdLux,
					customAdaptiveThemeThresholdLux = userPreferences.customAdaptiveThemeThresholdLux,
					hasSetupCompleted = userPreferences.hasSetupCompleted
				)

				if (userPreferences.adaptiveThemeEnabled) {
					startSensorsIfEnabled()
				} else {
					stopSensors()
				}
			}
		}
	}

	private fun startLightSensorListening() {
		if (isListeningToSensor) return
		isListeningToSensor = true
		lightSensorManager.startListening({ lux: Float ->
			viewModelScope.launch {
				updateCurrentSensorLux(lux)
			}
		}, sensorDelay = SensorManager.SENSOR_DELAY_NORMAL)
	}

	private fun stopLightSensorListening() {
		if (!isListeningToSensor) return
		isListeningToSensor = false
		lightSensorManager.stopListening()
	}

	override fun onCleared() {
		stopLightSensorListening()
		stopProximityListening()
		super.onCleared()
	}

	/**
	 * Toggle adaptive theme service or show setup.
	 * @return true if service was toggled, false if setup is shown.
	 */
	fun onServiceToggleRequested(
		checked: Boolean,
		hasPermission: Boolean
	): Boolean {
		if (checked && !hasPermission) {
			viewModelScope.launch {
				_uiEvents.emit(NavigateToSetup)
			}
			return false
		}
		updateAdaptiveThemeEnabled(checked)
		return true
	}

	private fun updateAdaptiveThemeEnabled(enable: Boolean) {
		val wasEnabled = _uiState.value.adaptiveThemeEnabled
		viewModelScope.launch {
			userPreferencesRepository.updateAdaptiveThemeEnabled(enable)
			if (enable) {
				startBroadcastReceiverService()
				userPreferencesRepository.ensureAdaptiveThemeThresholdDefault()
				Logger.logServiceEnabled(
					application.applicationContext,
					source = if (wasEnabled) "state_restore" else "ui_toggle"
				)
			} else {
				stopBroadcastReceiverService()
				Logger.logServiceDisabled(
					application.applicationContext,
					source = if (wasEnabled) "ui_toggle" else "state_restore"
				)
			}
		}
	}

	fun updateAdaptiveThemeThresholdByIndex(index: Int) {
		val threshold = AdaptiveThreshold.fromIndex(index)
		val oldLux = _uiState.value.adaptiveThemeThresholdLux
		viewModelScope.launch {
			userPreferencesRepository.updateAdaptiveThemeThresholdLux(threshold.lux)
			// Log threshold change
			Logger.logBrightnessThresholdChanged(
				application.applicationContext,
				oldLux = oldLux,
				newLux = threshold.lux
			)
		}
	}

	fun setCustomAdaptiveThemeThreshold(lux: Float) {
		val oldLux = _uiState.value.adaptiveThemeThresholdLux
		viewModelScope.launch {
			userPreferencesRepository.updateCustomAdaptiveThemeThresholdLux(lux)
			Logger.logBrightnessThresholdChanged(
				application.applicationContext,
				oldLux = oldLux,
				newLux = lux
			)
		}
	}

	val isUsingCustomThreshold: Boolean
		get() = _uiState.value.customAdaptiveThemeThresholdLux != null

	fun getDisplayLuxSteps(baseLux: List<Float>): List<Float> {
		val customLux = _uiState.value.customAdaptiveThemeThresholdLux ?: return baseLux
		val index = AdaptiveThreshold.fromLux(customLux).ordinal
		return baseLux.mapIndexed { i, value -> if (i == index) customLux else value }
	}

	fun getDisplayLabels(labels: List<String>, customLabel: String): List<String> {
		return if (isUsingCustomThreshold) {
			labels.mapIndexed { index, label ->
				if (index == getIndexForCurrentLux()) customLabel else label
			}
		} else labels
	}

	fun onSliderValueCommitted(index: Int) {
		if (isUsingCustomThreshold) {
			customThresholdTemp = null
		}
		updateAdaptiveThemeThresholdByIndex(index)
	}

	fun getIndexForCurrentLux(): Int {
		val lux = customThresholdTemp ?: _uiState.value.adaptiveThemeThresholdLux
		return AdaptiveThreshold.fromLux(lux).ordinal
	}

	fun setPendingCustomSliderLux(lux: Float) {
		customThresholdTemp = lux
	}

	private fun startBroadcastReceiverService() {
		val intent = Intent(application.applicationContext, BroadcastReceiverService::class.java)
		ContextCompat.startForegroundService(application.applicationContext, intent)
	}

	private fun stopBroadcastReceiverService() {
		val intent = Intent(application.applicationContext, BroadcastReceiverService::class.java)
		application.applicationContext.stopService(intent)
	}
}

class MainViewModelFactory(
	private val application: Application,
	private val userPreferencesRepository: UserPreferencesRepository,
	private val darkThemeHandler: DarkThemeHandler
) : ViewModelProvider.Factory {


	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return MainViewModel(
				application,
				userPreferencesRepository,
				darkThemeHandler
			) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}
}