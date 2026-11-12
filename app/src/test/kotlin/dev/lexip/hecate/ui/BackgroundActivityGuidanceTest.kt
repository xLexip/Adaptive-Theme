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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import dev.lexip.hecate.logging.BackgroundGuidanceOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36, 37])
class BackgroundActivityGuidanceTest {
	@Test
	fun onlySupportedManufacturersGetAutomaticHints() {
		assertEquals(BackgroundActivityGuidance.MOTOROLA, BackgroundActivityGuidance.forManufacturer(" motorola "))
		assertEquals(BackgroundActivityGuidance.ONEPLUS, BackgroundActivityGuidance.forManufacturer("OnePlus"))
		for (name in listOf("Xiaomi", " redmi ", "POCO")) {
			assertEquals(BackgroundActivityGuidance.XIAOMI, BackgroundActivityGuidance.forManufacturer(name))
		}
		assertEquals(BackgroundActivityGuidance.REALME, BackgroundActivityGuidance.forManufacturer("realme"))
		assertEquals(BackgroundActivityGuidance.OPPO, BackgroundActivityGuidance.forManufacturer("OPPO"))
		for (name in listOf("Google", "Samsung", "", "unknown")) {
			val guidance = BackgroundActivityGuidance.forManufacturer(name)
			assertEquals(BackgroundActivityGuidance.OTHER, guidance)
			assertFalse(guidance.shouldShowHint(monitoringEnabled = true, dismissed = false, hasInterruptionEvidence = true))
		}
		for (guidance in BackgroundActivityGuidance.entries.filter { it != BackgroundActivityGuidance.OTHER }) {
			assertTrue(guidance.shouldShowHint(monitoringEnabled = true, dismissed = false, hasInterruptionEvidence = true))
			assertFalse(guidance.shouldShowHint(monitoringEnabled = true, dismissed = false, hasInterruptionEvidence = false))
			assertFalse(guidance.shouldShowHint(monitoringEnabled = false, dismissed = false, hasInterruptionEvidence = true))
			assertFalse(guidance.shouldShowHint(monitoringEnabled = true, dismissed = true, hasInterruptionEvidence = true))
		}
	}

	@Test
	fun settingsOpenTheCurrentAppsDetails() {
		val launched = mutableListOf<Intent>()
		val context = recordingContext { launched.add(it) }
		assertEquals(BackgroundGuidanceOutcome.OPENED, openBackgroundActivitySettings(context))
		assertEquals(1, launched.size)
		assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, launched.single().action)
		assertEquals("package:${context.packageName}", launched.single().dataString)
	}

	@Test
	fun unavailableAppDetailsFallBackToSystemSettings() {
		for (failure in listOf(ActivityNotFoundException(), SecurityException())) {
			val launched = mutableListOf<Intent>()
			val context = recordingContext {
				launched.add(it)
				if (it.action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) throw failure
			}
			assertEquals(BackgroundGuidanceOutcome.FALLBACK_OPENED, openBackgroundActivitySettings(context))
			assertEquals(listOf(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Settings.ACTION_SETTINGS), launched.map { it.action })
		}
	}

	@Test
	fun guideOpensTheMatchingManufacturerPage() {
		val pages = mapOf(
			BackgroundActivityGuidance.MOTOROLA to "https://dontkillmyapp.com/motorola",
			BackgroundActivityGuidance.ONEPLUS to "https://dontkillmyapp.com/oneplus",
			BackgroundActivityGuidance.XIAOMI to "https://dontkillmyapp.com/xiaomi",
			BackgroundActivityGuidance.REALME to "https://dontkillmyapp.com/realme",
			BackgroundActivityGuidance.OPPO to "https://dontkillmyapp.com/oppo",
			BackgroundActivityGuidance.OTHER to "https://dontkillmyapp.com"
		)
		for ((guidance, expectedUrl) in pages) {
			val launched = mutableListOf<Intent>()
			assertEquals(BackgroundGuidanceOutcome.OPENED, openBackgroundActivityGuide(recordingContext { launched.add(it) }, guidance))
			assertEquals(Intent.ACTION_VIEW, launched.single().action)
			assertEquals(expectedUrl, launched.single().dataString)
		}
	}

	@Test
	fun missingSettingsAndBrowserShowFeedbackWithoutCrashing() {
		val context = recordingContext { throw ActivityNotFoundException() }
		assertEquals(BackgroundGuidanceOutcome.UNAVAILABLE, openBackgroundActivitySettings(context))
		assertTrue(ShadowToast.getTextOfLatestToast().contains("dontkillmyapp.com"))
		assertEquals(BackgroundGuidanceOutcome.UNAVAILABLE, openBackgroundActivityGuide(context, BackgroundActivityGuidance.MOTOROLA))
		assertTrue(ShadowToast.getTextOfLatestToast().contains("dontkillmyapp.com"))
	}

	private fun recordingContext(onStart: (Intent) -> Unit): Context =
		object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
			override fun startActivity(intent: Intent) = onStart(intent)
		}
}
