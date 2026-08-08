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

package dev.lexip.hecate.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.accessibility.disableAccessibilityChecks
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import dev.lexip.hecate.R
import dev.lexip.hecate.ui.components.preferences.CustomThresholdDialog
import dev.lexip.hecate.ui.components.preferences.TimePickerPreferenceDialog
import dev.lexip.hecate.ui.theme.HecateTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferenceDialogsTest {

	@get:Rule
	val composeRule = createComposeRule()

	private val context
		get() = InstrumentationRegistry.getInstrumentation().targetContext

	@Before
	fun enableAccessibilityValidation() {
		composeRule.enableAccessibilityChecks()
	}

	@Test
	fun customThresholdRejectsInvalidTextAndAcceptsValidLux() {
		var confirmed: Float? = null
		composeRule.setContent {
			HecateTheme {
				CustomThresholdDialog(
					show = true,
					currentLux = 100f,
					onConfirm = { confirmed = it },
					onDismiss = {}
				)
			}
		}
		val field = composeRule.onNode(
			hasText(context.getString(R.string.hint_custom_threshold_value))
				.and(hasSetTextAction())
		)
		field.performTextClearance()
		field.performTextInput("not-a-number")
		composeRule.onNodeWithText(context.getString(R.string.action_set)).performClick()

		assertNull(confirmed)
		composeRule.onNodeWithText(context.getString(R.string.error_invalid_lux_value))
			.assertIsDisplayed()

		field.performTextClearance()
		field.performTextInput("321")
		composeRule.onNodeWithText(context.getString(R.string.action_set)).performClick()
		assertEquals(321f, confirmed)
	}

	@Test
	fun timePickerConfirmsInitialTimeDeterministically() {
		// Material3 1.4.0 splits its fixed 80 dp AM/PM selector into two 40 dp
		// targets. Suppress only that upstream finding in this focused test.
		composeRule.disableAccessibilityChecks()
		composeRule.enableAccessibilityChecks(
			AccessibilityValidator()
				.setRunChecksFromRootView(true)
				.setSuppressingResultMatcher(matchesCheck(TouchTargetSizeCheck::class.java))
		)

		var confirmedMinutes: Int? = null
		composeRule.setContent {
			HecateTheme {
				TimePickerPreferenceDialog(
					show = true,
					title = "Night start",
					initialMinutes = 22 * 60 + 15,
					onConfirm = { confirmedMinutes = it },
					onDismiss = {}
				)
			}
		}

		composeRule.onNodeWithText(context.getString(R.string.action_set)).performClick()

		assertEquals(22 * 60 + 15, confirmedMinutes)
	}
}
