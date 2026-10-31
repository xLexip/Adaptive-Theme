/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 * Licensed under the GNU General Public License, Version 3.0.
 * https://www.gnu.org/licenses/gpl-3.0
 */
package dev.lexip.hecate.util

import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.SharedPreferences
import android.system.OsConstants
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36, 37])
class MonitoringInterruptionTrackerTest {
	private lateinit var preferences: SharedPreferences
	private var now = 1_800_000_000_000L
	private var boot = 7
	private var summary: ByteArray? = null
	private var rejectSummaryWrites = false

	@Before fun setUp() {
		preferences = ApplicationProvider.getApplicationContext<Context>()
			.getSharedPreferences("interruption-test", Context.MODE_PRIVATE)
		preferences.edit { clear() }
	}

	private fun tracker() = MonitoringInterruptionTracker(
		preferences, "app", { boot },
		{ value ->
			if (rejectSummaryWrites) throw IllegalStateException("Throttled")
			summary = value?.copyOf()
		},
		{ now }
	)

	private fun exit(): MonitoringExitRecord {
		tracker().markMonitoringActive()
		now += 30_000
		return MonitoringExitRecord(
			now, "app", IMPORTANCE_FOREGROUND_SERVICE,
			ApplicationExitInfo.REASON_LOW_MEMORY, stateSummary = summary?.copyOf()
		)
	}

	private fun inspect(exit: MonitoringExitRecord, gap: Long = MIN_INTERRUPTION_GAP_MS) {
		now += gap
		tracker().inspect(listOf(exit))
	}

	@Test fun needsTwoDistinctInterruptionsAndPersistsAcrossTrackerRecreation() {
		assertFalse(tracker().hasRecentInterruptions())
		inspect(exit())
		assertFalse(tracker().hasRecentInterruptions())
		inspect(exit())
		assertTrue(tracker().hasRecentInterruptions())
	}

	@Test fun inspectingTheSameHistoricalExitAgainCannotReachTheThreshold() {
		val exit = exit()
		inspect(exit)
		now += MIN_INTERRUPTION_GAP_MS
		tracker().inspect(listOf(exit, exit))
		assertFalse(tracker().hasRecentInterruptions())
	}

	@Test fun promptlyRecoveredExitsDoNotCountEvenIfReadAgainLater() {
		inspect(exit())
		val quickExit = exit()
		inspect(quickExit, MIN_INTERRUPTION_GAP_MS - 1)
		now += MIN_INTERRUPTION_GAP_MS
		tracker().inspect(listOf(quickExit))
		assertFalse(tracker().hasRecentInterruptions())
	}

	@Test fun onlyTheNewestMainProcessExitHasAValidRecoveryGap() {
		inspect(exit())
		val older = exit()
		now += 10_000
		val newer = older.copy(timestampMs = now, reason = ApplicationExitInfo.REASON_USER_REQUESTED)
		now += MIN_INTERRUPTION_GAP_MS
		tracker().inspect(listOf(older, newer))
		assertFalse(tracker().hasRecentInterruptions())
	}

	@Test fun expectedOrAmbiguousReasonsNeverMakeTheHintEligible() {
		for (reason in listOf(
			ApplicationExitInfo.REASON_CRASH, ApplicationExitInfo.REASON_CRASH_NATIVE,
			ApplicationExitInfo.REASON_ANR, ApplicationExitInfo.REASON_EXIT_SELF,
			ApplicationExitInfo.REASON_USER_REQUESTED, ApplicationExitInfo.REASON_USER_STOPPED,
			ApplicationExitInfo.REASON_PACKAGE_UPDATED, ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE,
			ApplicationExitInfo.REASON_PERMISSION_CHANGE, ApplicationExitInfo.REASON_UNKNOWN,
			ApplicationExitInfo.REASON_OTHER, ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
			ApplicationExitInfo.REASON_FREEZER, ApplicationExitInfo.REASON_INITIALIZATION_FAILURE
		)) {
			preferences.edit { clear() }
			inspect(exit())
			inspect(exit().copy(reason = reason, description = "System maintenance"))
			assertFalse("Unexpected eligibility for reason $reason", tracker().hasRecentInterruptions())
		}
	}

	@Test fun cachedChildProcessAndUnmarkedExitsDoNotCount() {
		for (invalid in listOf<(MonitoringExitRecord) -> MonitoringExitRecord>(
			{ it.copy(importance = IMPORTANCE_CACHED) },
			{ it.copy(processName = "app:child") },
			{ it.copy(stateSummary = null) },
			{ it.copy(stateSummary = "unrecognized state".toByteArray()) },
			{ it.copy(reason = ApplicationExitInfo.REASON_SIGNALED, status = OsConstants.SIGTERM) }
		)) {
			preferences.edit { clear() }
			inspect(exit())
			inspect(invalid(exit()))
			assertFalse(tracker().hasRecentInterruptions())
		}
	}

	@Test fun explicitKillSignalsAndObservedCleanerReasonsQualify() {
		val candidates = listOf<(MonitoringExitRecord) -> MonitoringExitRecord>(
			{ it.copy(reason = ApplicationExitInfo.REASON_SIGNALED, status = OsConstants.SIGKILL) },
			{ it.copy(reason = ApplicationExitInfo.REASON_OTHER, description = "OneKeyClean") },
			{ it.copy(reason = ApplicationExitInfo.REASON_OTHER, description = "GameClean") },
			{ it.copy(reason = ApplicationExitInfo.REASON_OTHER, description = "[UNKNOWN] o-kill(99)") },
			{ it.copy(reason = ApplicationExitInfo.REASON_OTHER, description = "stop app due to o-stop(40)") },
			{ it.copy(reason = ApplicationExitInfo.REASON_OTHER, description = "[KILL UID] Foreground kill") }
		)
		for (candidate in candidates) {
			preferences.edit { clear() }
			inspect(exit())
			inspect(candidate(exit()))
			assertTrue(tracker().hasRecentInterruptions())
		}
	}

	@Test fun rebootAndUnavailableBootIdentityDoNotCreateKillEvidence() {
		inspect(exit())
		val beforeReboot = exit()
		boot++
		inspect(beforeReboot)
		assertFalse(tracker().hasRecentInterruptions())
		boot = -1
		summary = null
		tracker().markMonitoringActive()
		assertNull(summary)
	}

	@Test fun expiredAndFutureExitTimesRemainIneligible() {
		inspect(exit())
		inspect(exit())
		assertTrue(tracker().hasRecentInterruptions())
		now += INTERRUPTION_WINDOW_MS + 1
		assertFalse(tracker().hasRecentInterruptions())
		val valid = exit()
		tracker().inspect(listOf(valid.copy(timestampMs = now + 1)))
		inspect(valid)
		assertFalse(tracker().hasRecentInterruptions())
	}

	@Test fun aStoppedSessionCannotCountEvenIfTheOsKeepsAStaleMarker() {
		inspect(exit())
		val process = tracker()
		process.markMonitoringActive()
		val activeSummary = summary?.copyOf()
		now += 1000
		rejectSummaryWrites = true
		process.markMonitoringStopped()
		now += 1000
		val stale = MonitoringExitRecord(
			now, "app", IMPORTANCE_FOREGROUND_SERVICE, ApplicationExitInfo.REASON_LOW_MEMORY,
			stateSummary = activeSummary
		)
		inspect(stale)
		assertFalse(tracker().hasRecentInterruptions())
	}
}
