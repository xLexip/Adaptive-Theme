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

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.lexip.hecate.BuildConfig
import dev.lexip.hecate.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

	@get:Rule
	val composeRule = createAndroidComposeRule<MainActivity>()

	@Test
	fun activityLaunchesMainContent() {
		composeRule.onNodeWithText(composeRule.activity.getString(R.string.app_name))
			.assertExists()
	}

	@Test
	fun directDebugInstallShowsWhatsNewAndCloseSurvivesActivityRecreation() {
		val release = BuildConfig.VERSION_NAME.substringBefore('-').substringBeforeLast('.')
		val title = composeRule.activity.getString(R.string.whats_new_title, release)
		val close = composeRule.activity.getString(R.string.action_close)
		composeRule.waitUntil(timeoutMillis = 5_000) {
			composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
		}
		composeRule.onNodeWithText(title).assertIsDisplayed()
		composeRule.onNodeWithText(close).performClick()
		composeRule.onAllNodesWithText(title).assertCountEquals(0)
		composeRule.activityRule.scenario.recreate()
		composeRule.onAllNodesWithText(title).assertCountEquals(0)
	}
}
