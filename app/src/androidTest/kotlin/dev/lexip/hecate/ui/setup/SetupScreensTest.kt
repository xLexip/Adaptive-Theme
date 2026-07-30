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

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.lexip.hecate.R
import dev.lexip.hecate.ui.setup.screens.A_DeveloperModeScreen
import dev.lexip.hecate.ui.setup.screens.B_ConnectUsbScreen
import dev.lexip.hecate.ui.setup.screens.C_GrantPermissionScreen
import dev.lexip.hecate.ui.theme.HecateTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupScreensTest {

	@get:Rule
	val composeRule = createComposeRule()

	private val context
		get() = InstrumentationRegistry.getInstrumentation().targetContext

	@Before
	fun enableAccessibilityValidation() {
		composeRule.enableAccessibilityChecks()
	}

	@Test
	fun developerStepDisablesContinueWhileRequirementsAreIncomplete() {
		setDeveloperScreen(SetupUiState())
		composeRule.onNodeWithText(context.getString(R.string.action_continue))
			.assertIsNotEnabled()
	}

	@Test
	fun developerStepContinueInvokesCallback() {
		var nextCalls = 0
		setDeveloperScreen(
			state = SetupUiState(
				isDeveloperOptionsEnabled = true,
				isUsbDebuggingEnabled = true
			),
			onNext = { nextCalls++ }
		)

		composeRule.onNodeWithText(context.getString(R.string.action_continue)).performClick()

		assertEquals(1, nextCalls)
	}

	@Test
	fun connectStepOffersSkipWhenUsbIsNotConnected() {
		var nextCalls = 0
		setConnectScreen(SetupUiState(), onNext = { nextCalls++ })
		composeRule.onNodeWithText(context.getString(R.string.action_skip)).performClick()
		assertEquals(1, nextCalls)
	}

	@Test
	fun connectStepOffersContinueWhenUsbIsConnected() {
		var nextCalls = 0
		setConnectScreen(
			SetupUiState(isUsbConnected = true),
			onNext = { nextCalls++ }
		)
		composeRule.onNodeWithText(context.getString(R.string.action_continue))
			.assertIsEnabled()
			.performClick()
		assertEquals(1, nextCalls)
	}

	@Test
	fun grantStepDisablesFinishWithoutPermission() {
		setGrantScreen(SetupUiState())
		composeRule.onNodeWithText(context.getString(R.string.action_finish))
			.assertIsNotEnabled()
	}

	@Test
	fun grantStepEnablesFinishAfterPermissionAndInvokesCallback() {
		var finishCalls = 0
		setGrantScreen(
			SetupUiState(hasWriteSecureSettings = true),
			onFinish = { finishCalls++ }
		)
		composeRule.onNodeWithText(context.getString(R.string.action_finish))
			.assertIsEnabled()
			.performClick()
		assertEquals(1, finishCalls)
	}

	private fun setDeveloperScreen(
		state: SetupUiState,
		onNext: () -> Unit = {}
	) {
		composeRule.setContent {
			HecateTheme {
				A_DeveloperModeScreen(
					uiState = state,
					onGrantViaShizuku = {},
					onNext = onNext,
					onExit = {},
					onOpenSettings = {},
					onOpenDeveloperSettings = {}
				)
			}
		}
	}

	private fun setConnectScreen(
		state: SetupUiState,
		onNext: () -> Unit = {}
	) {
		composeRule.setContent {
			HecateTheme {
				B_ConnectUsbScreen(
					uiState = state,
					onGrantViaShizuku = {},
					onNext = onNext,
					onBack = {},
					onShareExpertCommand = {},
					onUseRoot = {},
					onInstallShizuku = {}
				)
			}
		}
	}

	private fun setGrantScreen(
		state: SetupUiState,
		onFinish: () -> Unit = {}
	) {
		composeRule.setContent {
			HecateTheme {
				C_GrantPermissionScreen(
					uiState = state,
					onShareSetupUrl = {},
					onShareExpertCommand = {},
					onFinish = onFinish,
					onBack = {},
					onUseRoot = {},
					onInstallShizuku = {}
				)
			}
		}
	}
}
