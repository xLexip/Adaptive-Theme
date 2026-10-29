/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.lexip.hecate.logging

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlin.random.Random

internal fun shouldLogMonitoringDiagnostic(sampleBucket: Int, failure: Boolean): Boolean =
	failure || sampleBucket == 0

/** A fixed 5% local cohort keeps routine lifecycle stages comparable without an identifier. */
internal object MonitoringDiagnostics {
	private var cachedBucket: Int? = null

	@Synchronized
	private fun sampleBucket(context: Context): Int {
		cachedBucket?.let { return it }
		val prefs = context.getSharedPreferences("monitoring_diagnostics", Context.MODE_PRIVATE)
		val stored = prefs.getInt("sample_bucket", -1)
		val bucket = if (stored in 0..19) stored else Random.nextInt(20).also {
			prefs.edit { putInt("sample_bucket", it) }
		}
		cachedBucket = bucket
		return bucket
	}

	fun record(
		context: Context,
		stage: String,
		outcome: String,
		failure: Boolean = false,
		detail: String = "",
		elapsedMs: Long? = null,
		startId: Int? = null
	) {
		val message = "stage=$stage outcome=$outcome detail=$detail elapsedMs=$elapsedMs startId=$startId"
		if (failure) Log.w("MonitoringDiagnostics", message) else Log.d("MonitoringDiagnostics", message)
		// Diagnostic failures must never interrupt monitoring or foreground promotion.
		try {
			if (!LoggerGate.allowed()) return
			val bucket = sampleBucket(context)
			val sampled = bucket == 0
			if (!shouldLogMonitoringDiagnostic(bucket, failure)) return
			val processInfo = ActivityManager.RunningAppProcessInfo()
			ActivityManager.getMyMemoryState(processInfo)
			Logger.logMonitoringDiagnostic(
				context, stage, outcome, failure, sampled, detail,
				elapsedMs, startId, processInfo.importance
			)
		} catch (cause: Exception) {
			Log.w("MonitoringDiagnostics", "Unable to record diagnostic", cause)
		}
	}
}
