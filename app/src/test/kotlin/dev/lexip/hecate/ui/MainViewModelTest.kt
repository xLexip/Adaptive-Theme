/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
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

import androidx.test.core.app.ApplicationProvider
import dev.lexip.hecate.Application
import dev.lexip.hecate.FakeAdaptiveThemeServiceController
import dev.lexip.hecate.FakeInstallMetadataProvider
import dev.lexip.hecate.FakeProximitySensorReader
import dev.lexip.hecate.FakeSensorReader
import dev.lexip.hecate.FakeUserPreferencesDataSource
import dev.lexip.hecate.MainDispatcherRule
import dev.lexip.hecate.data.AdaptiveThreshold
import dev.lexip.hecate.data.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class MainViewModelTest {

	@get:Rule
	val mainDispatcherRule = MainDispatcherRule()

	private lateinit var application: Application
	private lateinit var preferences: FakeUserPreferencesDataSource
	private lateinit var lightSensor: FakeSensorReader
	private lateinit var proximitySensor: FakeProximitySensorReader
	private lateinit var serviceController: FakeAdaptiveThemeServiceController
	private lateinit var installMetadata: FakeInstallMetadataProvider

	@Before
	fun setUp() {
		application = ApplicationProvider.getApplicationContext()
		preferences = FakeUserPreferencesDataSource()
		lightSensor = FakeSensorReader()
		proximitySensor = FakeProximitySensorReader()
		serviceController = FakeAdaptiveThemeServiceController()
		installMetadata = FakeInstallMetadataProvider()
	}

	@Test
	fun mapsPreferencesAndInstallMetadataToUiState() = runTest(mainDispatcherRule.dispatcher) {
		installMetadata.fromPlayStore = true
		val viewModel = createViewModel()

		preferences.emit(
			UserPreferences(
				adaptiveThemeEnabled = false,
				adaptiveThemeThresholdLux = 42f,
				customAdaptiveThemeThresholdLux = 42f,
				hasSetupCompleted = true,
				stayDarkAtNightEnabled = true,
				nightStartMinutes = 20 * 60,
				nightEndMinutes = 7 * 60
			)
		)
		advanceUntilIdle()

		assertEquals(42f, viewModel.uiState.value.adaptiveThemeThresholdLux)
		assertEquals(42f, viewModel.uiState.value.customAdaptiveThemeThresholdLux)
		assertTrue(viewModel.uiState.value.hasSetupCompleted)
		assertTrue(viewModel.uiState.value.isInstalledFromPlayStore)
		assertTrue(viewModel.uiState.value.stayDarkAtNightEnabled)
		assertEquals(20 * 60, viewModel.uiState.value.nightStartMinutes)
		assertEquals(7 * 60, viewModel.uiState.value.nightEndMinutes)
	}

	@Test
	fun enablingWithoutPermissionNavigatesToSetupWithoutChangingPreference() =
		runTest(mainDispatcherRule.dispatcher) {
			val viewModel = createViewModel()
			val events = mutableListOf<UiEvent>()
			backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
				viewModel.uiEvents.toList(events)
			}
			advanceUntilIdle()

			val toggled = viewModel.onServiceToggleRequested(
				checked = true,
				hasPermission = false
			)
			advanceUntilIdle()

			assertFalse(toggled)
			assertFalse(preferences.current.adaptiveThemeEnabled)
			assertEquals(listOf(NavigateToSetup), events)
			assertEquals(0, serviceController.startCalls)
		}

	@Test
	fun serviceToggleWritesPreferenceAndControlsService() =
		runTest(mainDispatcherRule.dispatcher) {
			val viewModel = createViewModel()
			advanceUntilIdle()

			assertTrue(viewModel.onServiceToggleRequested(true, hasPermission = true))
			advanceUntilIdle()
			assertTrue(preferences.current.adaptiveThemeEnabled)
			assertEquals(1, preferences.thresholdDefaultCalls)
			assertEquals(1, serviceController.startCalls)

			assertTrue(viewModel.onServiceToggleRequested(false, hasPermission = true))
			advanceUntilIdle()
			assertFalse(preferences.current.adaptiveThemeEnabled)
			assertEquals(1, serviceController.stopCalls)
		}

	@Test
	fun proximityWarningUsesVirtualTimeAndCancelsWhenUncovered() =
		runTest(mainDispatcherRule.dispatcher) {
			preferences.emit(preferences.current.copy(adaptiveThemeEnabled = true))
			val viewModel = createViewModel()
			runCurrent()

			proximitySensor.emit(0f)
			advanceTimeBy(999)
			assertFalse(viewModel.uiState.value.isDeviceCovered)

			advanceTimeBy(1)
			runCurrent()
			assertTrue(viewModel.uiState.value.isDeviceCovered)

			proximitySensor.emit(10f)
			runCurrent()
			assertFalse(viewModel.uiState.value.isDeviceCovered)
			viewModel.stopSensors()
		}

	@Test
	fun thresholdCustomAndNightSettingsAreForwarded() =
		runTest(mainDispatcherRule.dispatcher) {
			val viewModel = createViewModel()
			advanceUntilIdle()

			viewModel.updateAdaptiveThemeThresholdByIndex(AdaptiveThreshold.BRIGHT.ordinal)
			advanceUntilIdle()
			assertEquals(AdaptiveThreshold.BRIGHT.lux, preferences.current.adaptiveThemeThresholdLux)

			viewModel.setCustomAdaptiveThemeThreshold(321f)
			advanceUntilIdle()
			assertEquals(321f, preferences.current.customAdaptiveThemeThresholdLux)

			viewModel.updateStayDarkAtNightEnabled(true)
			advanceUntilIdle()
			assertEquals(1, preferences.nightDefaultCalls)
			assertTrue(preferences.current.stayDarkAtNightEnabled)

			viewModel.updateNightWindow(22 * 60, 5 * 60)
			advanceUntilIdle()
			assertEquals(22 * 60, preferences.current.nightStartMinutes)
			assertEquals(5 * 60, preferences.current.nightEndMinutes)
		}

	@Test
	fun equalOrRepositoryRejectedNightWindowInvokesRejectionCallback() =
		runTest(mainDispatcherRule.dispatcher) {
			val viewModel = createViewModel()
			advanceUntilIdle()
			var rejectionCalls = 0

			viewModel.updateNightWindow(60, 60) { rejectionCalls++ }
			preferences.rejectNightWindow = true
			viewModel.updateNightWindow(22 * 60, 5 * 60) { rejectionCalls++ }
			advanceUntilIdle()

			assertEquals(2, rejectionCalls)
		}

	@Test
	fun reviewRequestIsEmittedOnlyOncePerViewModelSession() =
		runTest(mainDispatcherRule.dispatcher) {
			preferences.emit(preferences.current.copy(adaptiveThemeEnabled = true))
			installMetadata.installedDaysAgo = 3
			val viewModel = createViewModel()
			val events = mutableListOf<UiEvent>()
			backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
				viewModel.uiEvents.toList(events)
			}
			advanceUntilIdle()

			viewModel.checkReviewPrompt()
			viewModel.checkReviewPrompt()
			advanceUntilIdle()

			assertEquals(listOf(RequestInAppReview), events)
			viewModel.stopSensors()
		}

	private fun createViewModel(): MainViewModel = MainViewModel(
		application = application,
		userPreferencesRepository = preferences,
		lightSensorManager = lightSensor,
		proximitySensorManager = proximitySensor,
		serviceController = serviceController,
		installMetadataProvider = installMetadata,
		ioDispatcher = mainDispatcherRule.dispatcher,
		mainDispatcher = mainDispatcherRule.dispatcher
	)
}
