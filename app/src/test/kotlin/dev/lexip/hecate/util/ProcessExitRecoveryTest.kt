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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProcessExitRecoveryTest {
	@Test fun detectionDelayPreservesActualExitAge() {
		assertEquals(2_000L, elapsedSinceExit(10_000L, 12_000L))
		assertEquals(0L, elapsedSinceExit(10_000L, 10_000L))
	}

	@Test fun unknownAndFutureTimestampsAreNotReportedAsZeroDelay() {
		assertNull(elapsedSinceExit(0, 12_000))
		assertNull(elapsedSinceExit(-1, 12_000))
		assertNull(elapsedSinceExit(20_000, 12_000))
	}

	@Test fun childProcessExitIsNotAttributedToMonitoringRecovery() {
		val recovery = ProcessExitRecovery()
		recovery.observe("app:child", "app", 10_000, 1)
		assertNull(recovery.takeWhenReady(12_000))
	}

	@Test fun newestMainProcessExitIsConsumedOnlyOnce() {
		val recovery = ProcessExitRecovery()
		recovery.observe("app", "app", 10_000, 1)
		recovery.observe("app", "app", 8_000, 2)
		recovery.observe("app:child", "app", 11_000, 3)
		assertEquals(ObservedProcessExit(10_000, 1) to 2_000L, recovery.takeWhenReady(12_000))
		assertNull(recovery.takeWhenReady(13_000))
	}

	@Test fun clockRollbackDoesNotProduceNegativeRecoveryInterval() {
		val recovery = ProcessExitRecovery()
		recovery.observe("app", "app", 10_000, 1)
		assertNull(recovery.takeWhenReady(9_000))
		assertEquals(ObservedProcessExit(10_000, 1) to 1_000L, recovery.takeWhenReady(11_000))
	}
}
