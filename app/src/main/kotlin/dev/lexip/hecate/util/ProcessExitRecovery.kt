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

package dev.lexip.hecate.util

internal fun elapsedSinceExit(exitTimestampMs: Long, nowMs: Long): Long? =
	if (exitTimestampMs > 0 && nowMs >= exitTimestampMs) nowMs - exitTimestampMs else null

internal data class ObservedProcessExit(val timestampMs: Long, val pid: Int)

/** Readiness after an exit is not proof that monitoring was enabled at the time of death. */
internal class ProcessExitRecovery {
	private var pending: ObservedProcessExit? = null

	@Synchronized
	fun observe(processName: String, mainProcessName: String, timestampMs: Long, pid: Int) {
		if (processName != mainProcessName || timestampMs <= 0) return
		if (timestampMs > (pending?.timestampMs ?: 0)) pending = ObservedProcessExit(timestampMs, pid)
	}

	@Synchronized
	fun takeWhenReady(nowMs: Long): Pair<ObservedProcessExit, Long>? {
		val exit = pending ?: return null
		val elapsed = elapsedSinceExit(exit.timestampMs, nowMs) ?: return null
		pending = null
		return exit to elapsed
	}
}
