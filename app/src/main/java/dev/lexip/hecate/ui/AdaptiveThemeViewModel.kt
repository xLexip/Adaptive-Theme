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
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.lexip.hecate.HecateApplication
import dev.lexip.hecate.analytics.AnalyticsLogger
import dev.lexip.hecate.data.AdaptiveThreshold
import dev.lexip.hecate.data.UserPreferencesRepository
import dev.lexip.hecate.services.BroadcastReceiverService
import dev.lexip.hecate.util.DarkThemeHandler
import dev.lexip.hecate.util.LightSensorManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiEvent {
	data class CopyToClipboard(val text: String) : UiEvent
}

data class AdaptiveThemeUiState(
	val adaptiveThemeEnabled: Boolean = false,
	val adaptiveThemeThresholdLux: Float = AdaptiveThreshold.BRIGHT.lux,
	val customAdaptiveThemeThresholdLux: Float? = null
)

class AdaptiveThemeViewModel(
	private val application: HecateApplication,
	private val userPreferencesRepository: UserPreferencesRepository,
	@Suppress("unused")
	private var _darkThemeHandler: DarkThemeHandler
) : ViewModel() {

	private val _uiState = MutableStateFlow(AdaptiveThemeUiState())
	val uiState: StateFlow<AdaptiveThemeUiState> = _uiState.asStateFlow()

	// One-shot UI events
	private val _uiEvents = MutableSharedFlow<UiEvent>(
		replay = 0,
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	)
	val uiEvents = _uiEvents.asSharedFlow()

	// Permission Error Dialog
	private val _showMissingPermissionDialog = MutableStateFlow(false)
	val showMissingPermissionDialog: StateFlow<Boolean> = _showMissingPermissionDialog.asStateFlow()
	private val _pendingAdbCommand = MutableStateFlow("")
	val pendingAdbCommand: StateFlow<String> = _pendingAdbCommand.asStateFlow()

	// Light Sensor
	private val lightSensorManager = LightSensorManager(application.applicationContext)
	private var isListeningToSensor = false

	private val _currentSensorLux = MutableStateFlow(0f)
	val currentSensorLuxFlow: StateFlow<Float> = _currentSensorLux.asStateFlow()
	val currentSensorLux: Float get() = _currentSensorLux.value

	fun updateCurrentSensorLux(lux: Float) {
		_currentSensorLux.value = lux
	}

	// Temporary variable for custom threshold
	private var customThresholdTemp: Float? = null

	init {
		viewModelScope.launch {
			userPreferencesRepository.userPreferencesFlow.collect { userPreferences ->
				_uiState.value = AdaptiveThemeUiState(
					adaptiveThemeEnabled = userPreferences.adaptiveThemeEnabled,
					adaptiveThemeThresholdLux = userPreferences.adaptiveThemeThresholdLux,
					customAdaptiveThemeThresholdLux = userPreferences.customAdaptiveThemeThresholdLux
				)

				if (userPreferences.adaptiveThemeEnabled) startLightSensorListening()
				else stopLightSensorListening()
			}
		}
	}

	private fun startLightSensorListening() {
		if (isListeningToSensor) return
		isListeningToSensor = true
		lightSensorManager.startListening { lux ->
			viewModelScope.launch {
				updateCurrentSensorLux(lux)
			}
		}
	}

	private fun stopLightSensorListening() {
		if (!isListeningToSensor) return
		isListeningToSensor = false
		lightSensorManager.stopListening()
	}

	override fun onCleared() {
		stopLightSensorListening()
		super.onCleared()
	}

	/**
	 * Toggle adaptive theme service or show permission dialog.
	 * @return true if service was toggled, false if permission dialog is shown.
	 */
	fun onServiceToggleRequested(
		checked: Boolean,
		hasPermission: Boolean,
		packageName: String
	): Boolean {
		if (checked && !hasPermission) {
			_pendingAdbCommand.value =
				"adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
			_showMissingPermissionDialog.value = true
			// Log permission error shown
			AnalyticsLogger.logPermissionErrorShown(
				application.applicationContext,
				reason = "missing_write_secure_settings",
				attemptedAction = "enable_adaptive_theme"
			)
			return false
		}
		_showMissingPermissionDialog.value = false
		updateAdaptiveThemeEnabled(checked)
		return true
	}

	fun dismissDialog() {
		_showMissingPermissionDialog.value = false
	}

	fun requestCopyAdbCommand() {
		val cmd = _pendingAdbCommand.value
		viewModelScope.launch {
			_uiEvents.emit(UiEvent.CopyToClipboard(cmd))
		}
	}

	private fun updateAdaptiveThemeEnabled(enable: Boolean) {
		val wasEnabled = _uiState.value.adaptiveThemeEnabled
		viewModelScope.launch {
			userPreferencesRepository.updateAdaptiveThemeEnabled(enable)
			if (enable) {
				startBroadcastReceiverService()
				userPreferencesRepository.ensureAdaptiveThemeThresholdDefault()
				AnalyticsLogger.logServiceEnabled(
					application.applicationContext,
					source = if (wasEnabled) "state_restore" else "ui_toggle"
				)
			} else {
				stopBroadcastReceiverService()
				AnalyticsLogger.logServiceDisabled(
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
			AnalyticsLogger.logBrightnessThresholdChanged(
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
			AnalyticsLogger.logBrightnessThresholdChanged(
				application.applicationContext,
				oldLux = oldLux,
				newLux = lux
			)
		}
	}

	fun clearCustomAdaptiveThemeThreshold() {
		viewModelScope.launch {
			userPreferencesRepository.clearCustomAdaptiveThemeThreshold()
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

class AdaptiveThemeViewModelFactory(
	private val application: HecateApplication,
	private val userPreferencesRepository: UserPreferencesRepository,
	private val darkThemeHandler: DarkThemeHandler
) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(AdaptiveThemeViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return AdaptiveThemeViewModel(
				application,
				userPreferencesRepository,
				darkThemeHandler
			) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}
}