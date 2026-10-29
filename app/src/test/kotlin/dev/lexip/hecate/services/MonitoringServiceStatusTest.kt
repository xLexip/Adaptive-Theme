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

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringServiceStatusTest {
	@After fun reset() = MonitoringServiceStatus.ready()

	@Test fun failureRemainsVisibleUntilMonitoringIsReady() {
		MonitoringServiceStatus.failed()
		assertTrue(MonitoringServiceStatus.startFailed.value)
		MonitoringServiceStatus.ready()
		assertFalse(MonitoringServiceStatus.startFailed.value)
	}
}
