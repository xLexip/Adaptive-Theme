/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 * Licensed under the GNU General Public License, Version 3.0.
 * https://www.gnu.org/licenses/gpl-3.0
 */
package dev.lexip.hecate.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.lexip.hecate.R
import dev.lexip.hecate.ui.components.BackgroundActivityHint
import dev.lexip.hecate.ui.theme.HecateTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackgroundActivityHintTest {
	@get:Rule val composeRule = createComposeRule()
	private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

	@Test fun offscreenCompositionDoesNotLogAndScrollingOrRecompositionDoesNotDuplicate() {
		val owner = PromptLifecycleOwner(Lifecycle.State.RESUMED)
		val unrelatedState = mutableStateOf(0)
		var impressions = 0
		composeRule.setContent {
			CompositionLocalProvider(LocalLifecycleOwner provides owner) {
				HecateTheme {
					Column(Modifier.height(300.dp).verticalScroll(rememberScrollState())) {
						Text("Top")
						Spacer(Modifier.height(600.dp))
						BackgroundActivityHint({}, {}, {}, onImpression = { impressions++ })
						Text(unrelatedState.value.toString())
					}
				}
			}
		}
		composeRule.waitForIdle()
		assertEquals(0, impressions)
		composeRule.onNodeWithText(context.getString(R.string.background_help_website)).performScrollTo()
		composeRule.waitForIdle()
		assertEquals(1, impressions)
		composeRule.runOnIdle { unrelatedState.value++ }
		composeRule.onNodeWithText("Top").performScrollTo()
		composeRule.onNodeWithText(context.getString(R.string.background_help_website)).performScrollTo()
		composeRule.waitForIdle()
		assertEquals(1, impressions)
	}

	@Test fun waitsForResumedLifecycleAndSurvivesPauseAndSavedStateRestoration() {
		val owner = PromptLifecycleOwner(Lifecycle.State.STARTED)
		var impressions = 0
		val restoration = StateRestorationTester(composeRule)
		restoration.setContent {
			CompositionLocalProvider(LocalLifecycleOwner provides owner) {
				HecateTheme {
					BackgroundActivityHint({}, {}, {}, onImpression = { impressions++ })
				}
			}
		}
		composeRule.waitForIdle()
		assertEquals(0, impressions)
		composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
		composeRule.waitForIdle()
		assertEquals(1, impressions)
		composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
		composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
		restoration.emulateSavedInstanceStateRestore()
		composeRule.waitForIdle()
		assertEquals(1, impressions)
	}

	private class PromptLifecycleOwner(state: Lifecycle.State) : LifecycleOwner {
		val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = state }
		override val lifecycle: Lifecycle get() = registry
	}
}
