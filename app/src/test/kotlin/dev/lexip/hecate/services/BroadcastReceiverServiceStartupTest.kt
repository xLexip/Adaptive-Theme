/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 * Licensed under the GNU General Public License, Version 3.0.
 */
package dev.lexip.hecate.services

import android.app.Service
import android.content.Intent
import dev.lexip.hecate.Application
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36, 37], application = Application::class)
class BroadcastReceiverServiceStartupTest {
	@After fun reset() = MonitoringServiceStatus.ready()

	@Test fun failedPromotionStopsBeforeInitializingMonitoringAndPreservesRetryError() {
		val controller = Robolectric.buildService(BroadcastReceiverService::class.java).create()
		val service = controller.get()
		val shadow = shadowOf(service)
		shadow.setThrowInStartForeground(SecurityException("Promotion rejected"))
		val result = service.onStartCommand(
			Intent(service, BroadcastReceiverService::class.java)
				.putExtra(EXTRA_ENABLE_MONITORING, true),
			0, 42
		)
		assertEquals(Service.START_NOT_STICKY, result)
		assertEquals(42, shadow.stopSelfResultId)
		for (name in listOf("lightSensorManager", "proximitySensorManager", "adaptiveAppearanceHandler")) {
			val field = BroadcastReceiverService::class.java.getDeclaredField(name)
			field.isAccessible = true
			assertNull("$name must not initialize before promotion", field.get(service))
		}
		assertTrue(MonitoringServiceStatus.startFailed.value)
		controller.destroy()
		assertTrue(MonitoringServiceStatus.startFailed.value)
	}
}
