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

import android.app.ActivityManager
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.lexip.hecate.Application
import dev.lexip.hecate.data.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.fail

/**
 * Verifies that the persisted "Hide from Recents" preference drives the task property
 * that Android reads when populating the Recent Apps screen.
 *
 * The task flag has no public getter, so it is read from the hidden
 * [ActivityManager.RecentTaskInfo.excludeFromRecents] field. When the device cannot
 * expose it, these tests are skipped instead of reporting a false failure.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityRecentsTest {

	private val context: Context
		get() = InstrumentationRegistry.getInstrumentation().targetContext

	@After
	fun resetPreference() {
		writeHideFromRecents(false)
	}

	@Test
	fun enabledPreferenceExcludesMainTaskFromRecents() {
		writeHideFromRecents(true)
		withRecentsExclusion { awaitExcludeFromRecents { excluded -> excluded } }
	}

	@Test
	fun disabledPreferenceKeepsMainTaskInRecents() {
		writeHideFromRecents(false)
		withRecentsExclusion { awaitExcludeFromRecents { excluded -> !excluded } }
	}

	/**
	 * The exclusion flag can only be read once this app owns a task, which requires the
	 * activity to be running.
	 */
	private fun withRecentsExclusion(assertion: () -> Unit) {
		ActivityScenario.launch(MainActivity::class.java).use {
			assumeTrue(readExcludeFromRecents() != null)
			assertion()
		}
	}

	private fun writeHideFromRecents(enabled: Boolean) {
		val application = context.applicationContext as Application
		runBlocking(Dispatchers.IO) {
			UserPreferencesRepository(application.userPreferencesDataStore)
				.updateHideFromRecentsEnabled(enabled)
		}
	}

	private fun awaitExcludeFromRecents(expected: (Boolean) -> Boolean) {
		val deadline = System.currentTimeMillis() + TIMEOUT_MS
		while (System.currentTimeMillis() < deadline) {
			val excluded = readExcludeFromRecents()
			if (excluded != null && expected(excluded)) return
			Thread.sleep(POLL_INTERVAL_MS)
		}
		fail("Timed out waiting for the expected Recents visibility.")
	}

	/**
	 * Reads the task flag through the hidden `RecentTaskInfo` field. `getRecentTasks` is
	 * deprecated for third-party callers, but it is the only way to observe the flag.
	 */
	@Suppress("DEPRECATION")
	private fun readExcludeFromRecents(): Boolean? {
		val activityManager = context.getSystemService(ActivityManager::class.java)
			?: return null
		val taskId = activityManager.appTasks.firstOrNull()?.taskInfo?.taskId ?: return null
		val recentTask = activityManager.getRecentTasks(
			RECENT_TASK_LIMIT,
			ActivityManager.RECENT_WITH_EXCLUDED
		).firstOrNull { it.taskId == taskId } ?: return null
		return try {
			ActivityManager.RecentTaskInfo::class.java
				.getField("excludeFromRecents")
				.get(recentTask) as? Boolean
		} catch (_: ReflectiveOperationException) {
			null
		}
	}

	private companion object {
		const val RECENT_TASK_LIMIT = 30
		const val TIMEOUT_MS = 5_000L
		const val POLL_INTERVAL_MS = 50L
	}
}
