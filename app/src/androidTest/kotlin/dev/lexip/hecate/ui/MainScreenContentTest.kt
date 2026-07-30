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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.lexip.hecate.R
import dev.lexip.hecate.ui.theme.HecateTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenContentTest {

	@get:Rule
	val composeRule = createComposeRule()

	private val context
		get() = InstrumentationRegistry.getInstrumentation().targetContext

	@Before
	fun enableAccessibilityValidation() {
		composeRule.enableAccessibilityChecks()
	}

	@Test
	fun setupRequiredInactiveStateShowsSetupAndRequestsSetupFromSwitch() {
		var toggleRequest: Pair<Boolean, Boolean>? = null
		setMainContent(
			uiState = MainUiState(),
			hasPermission = false,
			callbacks = callbacks(
				onToggle = { checked, hasPermission ->
					toggleRequest = checked to hasPermission
					false
				}
			)
		)

		composeRule.onNodeWithText(context.getString(R.string.setup_required_title))
			.assertIsDisplayed()
		composeRule.onNodeWithText(adaptiveThemeAction()).performClick()

		assertEquals(null, toggleRequest)
	}

	@Test
	fun activeStateExposesThresholdAndAdvancedSettings() {
		setMainContent(
			uiState = MainUiState(
				adaptiveThemeEnabled = true,
				adaptiveThemeThresholdLux = 100f
			),
			hasPermission = true
		)

		composeRule.onNodeWithText(context.getString(R.string.title_brightness_threshold))
			.assertExists()
		composeRule.onNodeWithText(context.getString(R.string.action_advanced_settings))
			.assertExists()
	}

	@Test
	fun customThresholdAndNightLockStateAreRendered() {
		setMainContent(
			uiState = MainUiState(
				adaptiveThemeEnabled = true,
				adaptiveThemeThresholdLux = 321f,
				customAdaptiveThemeThresholdLux = 321f,
				stayDarkAtNightEnabled = true,
				nightStartMinutes = 22 * 60,
				nightEndMinutes = 5 * 60
			),
			hasPermission = true
		)

		composeRule.onNodeWithText(context.getString(R.string.adaptive_threshold_custom))
			.assertExists()
		composeRule.onNodeWithText(context.getString(R.string.title_night_dark_lock))
			.assertExists()
	}

	@Test
	fun permissionGrantedSwitchForwardsCallback() {
		var toggleRequest: Pair<Boolean, Boolean>? = null
		setMainContent(
			uiState = MainUiState(),
			hasPermission = true,
			callbacks = callbacks(
				onToggle = { checked, hasPermission ->
					toggleRequest = checked to hasPermission
					true
				}
			)
		)

		composeRule.onNodeWithText(adaptiveThemeAction()).performClick()

		assertEquals(true to true, toggleRequest)
	}

	private fun setMainContent(
		uiState: MainUiState,
		hasPermission: Boolean,
		callbacks: MainScreenCallbacks = callbacks()
	) {
		composeRule.setContent {
			HecateTheme {
				MainScreenContent(
					uiState = uiState,
					currentSensorLux = 25f,
					isDeviceCovered = false,
					isBatterySaverActive = false,
					hasWriteSecureSettingsPermission = hasPermission,
					packageName = context.packageName,
					callbacks = callbacks
				)
			}
		}
	}

	private fun callbacks(
		onToggle: (Boolean, Boolean) -> Boolean = { _, _ -> true }
	): MainScreenCallbacks = MainScreenCallbacks(
		onServiceToggleRequested = onToggle,
		onThresholdSelected = { _, _ -> },
		onCheckReviewPrompt = {},
		onStayDarkAtNightChanged = {},
		onCustomThresholdConfirmed = {},
		onNightWindowChanged = { _, _, _ -> }
	)

	private fun adaptiveThemeAction(): String = context.getString(
		R.string.action_use_adaptive_theme,
		context.getString(R.string.app_name)
	)
}
