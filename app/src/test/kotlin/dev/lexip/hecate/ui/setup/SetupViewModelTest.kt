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

package dev.lexip.hecate.ui.setup

import androidx.test.core.app.ApplicationProvider
import dev.lexip.hecate.Application
import dev.lexip.hecate.FakeAdaptiveThemeServiceController
import dev.lexip.hecate.FakeSetupEnvironmentProvider
import dev.lexip.hecate.FakeSetupPermissionGrantController
import dev.lexip.hecate.FakeUserPreferencesDataSource
import dev.lexip.hecate.MainDispatcherRule
import dev.lexip.hecate.ui.navigation.NavigationEvent
import dev.lexip.hecate.ui.navigation.NavigationManager
import dev.lexip.hecate.ui.navigation.SetupRoute
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
@Config(sdk = [36, 37], application = Application::class)
class SetupViewModelTest {

	@get:Rule
	val mainDispatcherRule = MainDispatcherRule()

	private lateinit var application: Application
	private lateinit var preferences: FakeUserPreferencesDataSource
	private lateinit var navigation: NavigationManager
	private lateinit var environment: FakeSetupEnvironmentProvider
	private lateinit var permissionController: FakeSetupPermissionGrantController
	private lateinit var serviceController: FakeAdaptiveThemeServiceController

	@Before
	fun setUp() {
		application = ApplicationProvider.getApplicationContext()
		preferences = FakeUserPreferencesDataSource()
		navigation = NavigationManager()
		environment = FakeSetupEnvironmentProvider()
		permissionController = FakeSetupPermissionGrantController()
		serviceController = FakeAdaptiveThemeServiceController()
	}

	@Test
	fun initialStepSkipsDeveloperModeWhenItsRequirementsAreAlreadyMet() =
		runTest(mainDispatcherRule.dispatcher) {
			environment.current = environment.current.copy(
				isDeveloperOptionsEnabled = true,
				isUsbDebuggingEnabled = true
			)

			val viewModel = createViewModel()
			runCurrent()

			assertEquals(SetupRoute.ConnectUsb, viewModel.uiState.value.currentStep)
			assertTrue(viewModel.uiState.value.isStep1Complete)
			assertTrue(viewModel.uiState.value.pendingAdbCommand.contains(application.packageName))
			viewModel.exitSetup()
		}

	@Test
	fun completedStepCountsDownAndAutomaticallyAdvances() =
		runTest(mainDispatcherRule.dispatcher) {
			val viewModel = createViewModel()
			runCurrent()

			environment.current = environment.current.copy(
				isDeveloperOptionsEnabled = true,
				isUsbDebuggingEnabled = true
			)
			advanceTimeBy(1_000)
			runCurrent()
			assertTrue(viewModel.uiState.value.isStep1Complete)
			assertEquals(2, viewModel.uiState.value.autoAdvanceCountdown)

			advanceTimeBy(2_000)
			runCurrent()
			assertEquals(SetupRoute.ConnectUsb, viewModel.uiState.value.currentStep)
			assertEquals(0, viewModel.uiState.value.autoAdvanceCountdown)
			viewModel.exitSetup()
		}

	@Test
	fun nextAndBackUpdateStateAndEmitNavigation() =
		runTest(mainDispatcherRule.dispatcher) {
			val viewModel = createViewModel()
			val events = mutableListOf<NavigationEvent>()
			backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
				navigation.navigationEvents.toList(events)
			}
			runCurrent()

			viewModel.navigateToNextStep()
			viewModel.navigateBack()
			runCurrent()

			assertEquals(SetupRoute.DeveloperMode, viewModel.uiState.value.currentStep)
			assertEquals(
				listOf(
					NavigationEvent.ToSetupStep(SetupRoute.ConnectUsb),
					NavigationEvent.Back
				),
				events
			)
			viewModel.exitSetup()
		}

	@Test
	fun completionIsIdempotentAndStartsServiceOnce() =
		runTest(mainDispatcherRule.dispatcher) {
			val viewModel = createViewModel()
			runCurrent()

			viewModel.completeSetup(source = "test")
			viewModel.completeSetup(source = "duplicate")
			advanceUntilIdle()

			assertTrue(viewModel.uiState.value.isSetupCompleted)
			assertTrue(preferences.current.hasSetupCompleted)
			assertTrue(preferences.current.adaptiveThemeEnabled)
			assertEquals(1, preferences.setupCompletedWrites)
			assertEquals(1, preferences.thresholdDefaultCalls)
			assertEquals(1, serviceController.startCalls)
		}

	@Test
	fun silentRootSuccessCompletesSetupThroughInjectedController() =
		runTest(mainDispatcherRule.dispatcher) {
			permissionController.rootResult = SetupGrantResult.Success

			val viewModel = createViewModel()
			advanceUntilIdle()

			assertTrue(viewModel.uiState.value.isSetupCompleted)
			assertEquals(1, permissionController.rootGrantCalls)
			assertEquals(1, serviceController.startCalls)
		}

	@Test
	fun shizukuSuccessCompletesSetupWithoutRealBinder() =
		runTest(mainDispatcherRule.dispatcher) {
			permissionController.binderReady = true
			permissionController.shizukuPermission = true
			permissionController.shizukuResult = SetupGrantResult.Success
			val viewModel = createViewModel()
			runCurrent()

			viewModel.onGrantViaShizukuRequested()
			advanceUntilIdle()

			assertTrue(viewModel.uiState.value.isSetupCompleted)
			assertEquals(1, permissionController.shizukuGrantCalls)
		}

	@Test
	fun missingShizukuAuthorizationRequestsPermissionWithoutExecutingGrant() =
		runTest(mainDispatcherRule.dispatcher) {
			permissionController.binderReady = true
			permissionController.shizukuPermission = false
			val viewModel = createViewModel()
			runCurrent()

			viewModel.onGrantViaShizukuRequested()
			runCurrent()

			assertEquals(1, permissionController.permissionRequestCalls)
			assertEquals(0, permissionController.shizukuGrantCalls)
			assertFalse(viewModel.uiState.value.isSetupCompleted)
			viewModel.exitSetup()
		}

	@Test
	fun rejectedPermissionCheckDoesNotCompleteSetup() =
		runTest(mainDispatcherRule.dispatcher) {
			val viewModel = createViewModel()
			runCurrent()

			viewModel.checkPermissionAndComplete()
			runCurrent()

			assertFalse(viewModel.uiState.value.isSetupCompleted)
			assertEquals(0, serviceController.startCalls)
			viewModel.exitSetup()
		}

	private fun createViewModel(): SetupViewModel = SetupViewModel(
		application = application,
		userPreferencesRepository = preferences,
		navigationManager = navigation,
		environmentProvider = environment,
		permissionGrantController = permissionController,
		serviceController = serviceController,
		ioDispatcher = mainDispatcherRule.dispatcher,
		mainDispatcher = mainDispatcherRule.dispatcher
	)
}
