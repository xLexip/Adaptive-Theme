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

package dev.lexip.hecate.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundServiceStartupTest {
	@Test
	fun typedPromotionDoesNotRetryOrReportFailure() {
		val calls = mutableListOf<String>()
		assertTrue(promoteForegroundService(
			{ calls += "typed" }, { calls += "compatibility" }, { _, _ -> calls += "error" }
		))
		assertEquals(listOf("typed"), calls)
	}

	@Test
	fun securityFailureUsesCompatibilityPromotion() {
		val calls = mutableListOf<String>()
		assertTrue(promoteForegroundService(
			{ calls += "typed"; throw SecurityException("ROM rejects explicit type") },
			{ calls += "compatibility" }, { stage, _ -> calls += "failed:$stage" }
		))
		assertEquals(listOf("typed", "compatibility", "failed:typed"), calls)
	}

	@Test
	fun completePromotionFailureCannotStartMonitoring() {
		val failures = mutableListOf<String>()
		val promoted = promoteForegroundService(
			{ throw SecurityException() }, { throw IllegalStateException() },
			{ stage, _ -> failures += stage }
		)
		assertFalse(promoted)
		assertEquals(listOf("typed", "compatibility"), failures)
	}

	@Test
	fun backgroundRestrictionDoesNotTriggerCompatibilityRetry() {
		var retries = 0
		assertFalse(promoteForegroundService(
			{ throw IllegalStateException("Background start prohibited") },
			{ retries++ }, { _, _ -> }
		))
		assertEquals(0, retries)
	}
}
