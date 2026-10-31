/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 * Licensed under the GNU General Public License, Version 3.0.
 * https://www.gnu.org/licenses/gpl-3.0
 */
package dev.lexip.hecate.util

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.system.OsConstants
import android.util.Log
import androidx.core.content.edit
import java.util.Locale

internal const val INTERRUPTION_WINDOW_MS = 7 * 24 * 60 * 60 * 1000L
internal const val MIN_INTERRUPTION_GAP_MS = 60_000L
private const val PREFS_NAME = "monitoring_interruption_tracker"
private const val RECENT_EXITS = "recent_exits"
private const val LAST_INSPECTED_EXIT = "last_inspected_exit"
private const val LAST_STOPPED_AT = "last_stopped_at"
private const val MARKER_PREFIX = "hecate-monitor-v1"
private const val TAG = "MonitoringInterruptions"

internal data class MonitoringExitRecord(
	val timestampMs: Long,
	val processName: String,
	val importance: Int,
	val reason: Int,
	val status: Int = 0,
	val description: String? = null,
	val stateSummary: ByteArray? = null
) {
	companion object {
		fun from(info: ApplicationExitInfo) = MonitoringExitRecord(
			info.timestamp, info.processName, info.importance, info.reason,
			info.status, info.description, info.processStateSummary
		)
	}
}

/** Independent of analytics; missing or ambiguous history never makes the hint eligible. */
internal class MonitoringInterruptionTracker(
	private val preferences: SharedPreferences,
	private val mainProcessName: String,
	private val bootCount: () -> Int,
	private val setProcessStateSummary: (ByteArray?) -> Unit,
	private val nowMs: () -> Long = System::currentTimeMillis
) {
	private var activeSinceMs: Long? = null

	fun markMonitoringActive() {
		val now = nowMs()
		val stoppedAt = preferences.getLong(LAST_STOPPED_AT, 0)
		if (activeSinceMs?.let { it > stoppedAt } == true) return
		val boot = bootCount()
		if (boot < 0 || now <= 0) return
		try {
			setProcessStateSummary("$MARKER_PREFIX:$boot:$now".toByteArray(Charsets.UTF_8))
			activeSinceMs = now
		} catch (cause: RuntimeException) {
			Log.w(TAG, "Could not mark monitoring active", cause)
		}
	}

	fun markMonitoringStopped() {
		activeSinceMs = null
		// Also guards against an old active marker if clearing the OS summary is throttled.
		preferences.edit { putLong(LAST_STOPPED_AT, nowMs()) }
		try {
			setProcessStateSummary(null)
		} catch (cause: RuntimeException) {
			Log.w(TAG, "Could not clear monitoring marker", cause)
		}
	}

	fun inspect(exits: List<MonitoringExitRecord>) {
		// Only the newest main-process exit has a known gap to this startup. Older entries
		// may already have recovered promptly; never infer their downtime from today's clock.
		val exit = exits.filter { it.processName == mainProcessName }.maxByOrNull { it.timestampMs }
			?: return
		val now = nowMs()
		if (exit.timestampMs <= 0 || exit.timestampMs > now) return
		if (exit.timestampMs <= preferences.getLong(LAST_INSPECTED_EXIT, 0)) return
		val recent = recentExits(now).toMutableList()
		if (qualifies(exit, now)) recent.add(exit.timestampMs)
		preferences.edit {
			putLong(LAST_INSPECTED_EXIT, exit.timestampMs)
			putString(RECENT_EXITS, recent.distinct().sorted().takeLast(2).joinToString(","))
		}
	}

	fun hasRecentInterruptions(): Boolean = recentExits(nowMs()).size >= 2

	private fun recentExits(now: Long): List<Long> =
		preferences.getString(RECENT_EXITS, "").orEmpty().split(',')
			.mapNotNull(String::toLongOrNull)
			.filter { it > 0 && it <= now && now - it <= INTERRUPTION_WINDOW_MS }
			.distinct()

	private fun qualifies(exit: MonitoringExitRecord, now: Long): Boolean {
		val age = now - exit.timestampMs
		if (age !in MIN_INTERRUPTION_GAP_MS..INTERRUPTION_WINDOW_MS) return false
		if (exit.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE) return false
		val marker = exit.stateSummary?.toString(Charsets.UTF_8)?.split(':') ?: return false
		if (marker.size != 3 || marker[0] != MARKER_PREFIX) return false
		val recordedBoot = marker[1].toIntOrNull() ?: return false
		if (recordedBoot < 0 || recordedBoot != bootCount()) return false
		val activeSince = marker[2].toLongOrNull() ?: return false
		if (activeSince <= preferences.getLong(LAST_STOPPED_AT, 0) || activeSince > exit.timestampMs) return false
		return when (exit.reason) {
			ApplicationExitInfo.REASON_LOW_MEMORY -> true
			ApplicationExitInfo.REASON_SIGNALED -> exit.status == OsConstants.SIGKILL
			ApplicationExitInfo.REASON_OTHER -> isKnownCleanup(exit.description)
			else -> false
		}
	}

	companion object {
		fun create(context: Context): MonitoringInterruptionTracker = MonitoringInterruptionTracker(
			context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
			context.applicationInfo.processName ?: context.packageName,
			bootCount = {
				runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1) }
					.getOrDefault(-1)
			},
			setProcessStateSummary = { state ->
				val manager = context.getSystemService(ActivityManager::class.java)
					?: error("ActivityManager unavailable")
				manager.setProcessStateSummary(state)
			}
		)

		private fun isKnownCleanup(description: String?): Boolean {
			val text = description?.lowercase(Locale.ROOT) ?: return false
			// Human-readable OEM descriptions are not a stable API: recognize only observed
			// cleanup signatures. Unrecognized OTHER records deliberately remain ineligible.
			return listOf("onekeyclean", "gameclean", "memoryguardian", "smartclean", "o-kill(", "o-stop(")
				.any(text::contains) || text.startsWith("[kill uid] foreground kill")
		}
	}
}
