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

package dev.lexip.hecate.ui.setup

import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import dev.lexip.hecate.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36, 37], application = Application::class)
class SetupEnvironmentProviderTest {

	private val context = ApplicationProvider.getApplicationContext<Application>()
	private val provider = AndroidSetupEnvironmentProvider(context)

	@Test
	@Config(sdk = [36])
	fun zeroStillReportsDisabledOnOlderAndroid() {
		writeSettings("0")
		val snapshot = provider.snapshot()
		assertEquals(false, snapshot.isDeveloperOptionsEnabled)
		assertEquals(false, snapshot.isUsbDebuggingEnabled)
	}

	@Test
	@Config(sdk = [37])
	fun redactedZeroDoesNotClaimSettingsAreDisabled() {
		writeSettings("0")
		val snapshot = provider.snapshot()
		assertNull(snapshot.isDeveloperOptionsEnabled)
		assertNull(snapshot.isUsbDebuggingEnabled)
	}

	@Test
	fun positiveReadingsStillConfirmEnabledSettings() {
		writeSettings("1")
		val snapshot = provider.snapshot()
		assertEquals(true, snapshot.isDeveloperOptionsEnabled)
		assertEquals(true, snapshot.isUsbDebuggingEnabled)
	}

	@Test
	fun missingOrInvalidSettingsAreUnconfirmed() {
		writeSettings(null)
		assertNull(provider.snapshot().isDeveloperOptionsEnabled)
		assertNull(provider.snapshot().isUsbDebuggingEnabled)
		writeSettings("invalid")
		assertNull(provider.snapshot().isDeveloperOptionsEnabled)
		assertNull(provider.snapshot().isUsbDebuggingEnabled)
	}

	private fun writeSettings(value: String?) {
		Settings.Global.putString(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, value)
		Settings.Global.putString(context.contentResolver, Settings.Global.ADB_ENABLED, value)
	}
}
