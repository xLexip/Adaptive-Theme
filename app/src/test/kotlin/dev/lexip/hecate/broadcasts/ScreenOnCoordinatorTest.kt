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

package dev.lexip.hecate.broadcasts

import dev.lexip.hecate.util.ProximitySensorReader
import dev.lexip.hecate.util.SensorReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenOnCoordinatorTest {
	@Test
	fun noProximitySensorReadsLightAndAppliesThemeOnce() {
		val proximity = FakeProximitySensor(hasProximitySensor = false)
		val light = FakeSensor()
		var requestedTheme: Boolean? = null
		val coordinator = coordinator(proximity, light) { requestedTheme = it }

		coordinator.onScreenOn()
		light.emit(25f)
		light.emit(10_000f)

		assertEquals(1, light.stopCount)
		assertEquals(true, requestedTheme)
	}

	@Test
	fun coveredDeviceStopsAfterProximityReading() {
		val proximity = FakeProximitySensor()
		val light = FakeSensor()
		var requestedTheme: Boolean? = null
		val coordinator = coordinator(proximity, light) { requestedTheme = it }

		coordinator.onScreenOn()
		proximity.emit(COVERED_DISTANCE_CENTIMETERS - 1f)

		assertEquals(1, proximity.stopCount)
		assertEquals(0, light.startCount)
		assertNull(requestedTheme)
	}

	@Test
	fun uncoveredDeviceReadsLightAfterProximity() {
		val proximity = FakeProximitySensor()
		val light = FakeSensor()
		var requestedTheme: Boolean? = null
		val coordinator = coordinator(proximity, light) { requestedTheme = it }

		coordinator.onScreenOn()
		proximity.emit(COVERED_DISTANCE_CENTIMETERS)
		proximity.emit(COVERED_DISTANCE_CENTIMETERS)
		light.emit(100f)

		assertEquals(1, proximity.stopCount)
		assertEquals(1, light.startCount)
		assertEquals(1, light.stopCount)
		assertEquals(true, requestedTheme)
	}

	private fun coordinator(
		proximity: FakeProximitySensor,
		light: FakeSensor,
		onThemeRequested: (Boolean) -> Unit
	) = ScreenOnCoordinator(
		proximitySensor = proximity,
		lightSensor = light,
		themeController = onThemeRequested,
		minuteProvider = { 12 * 60 },
		adaptiveThemeThresholdLux = 100f,
		stayDarkAtNightEnabled = false,
		nightStartMinutes = 21 * 60,
		nightEndMinutes = 6 * 60
	)
}

private open class FakeSensor : SensorReader {
	private var callback: ((Float) -> Unit)? = null
	var startCount = 0
	var stopCount = 0

	override fun startListening(callback: (Float) -> Unit, sensorDelay: Int) {
		startCount += 1
		this.callback = callback
	}

	override fun stopListening() {
		stopCount += 1
	}

	fun emit(value: Float) {
		callback?.invoke(value)
	}
}

private class FakeProximitySensor(
	override val hasProximitySensor: Boolean = true
) : FakeSensor(), ProximitySensorReader
