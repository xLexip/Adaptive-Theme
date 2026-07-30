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

package dev.lexip.hecate.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {
	private lateinit var directory: File
	private lateinit var scope: TestScope
	private lateinit var repository: UserPreferencesRepository

	@Before
	fun setUp() {
		directory = Files.createTempDirectory("adaptive-theme-prefs").toFile()
		scope = TestScope(UnconfinedTestDispatcher())
		val dataStore = PreferenceDataStoreFactory.create(
			scope = scope,
			produceFile = { File(directory, "preferences.preferences_pb") }
		)
		repository = UserPreferencesRepository(dataStore)
	}

	@After
	fun tearDown() {
		scope.cancel()
		directory.deleteRecursively()
	}

	@Test
	fun emptyStore_exposesApplicationDefaults() = runTest {
		val preferences = repository.userPreferencesFlow.first()

		assertFalse(preferences.adaptiveThemeEnabled)
		assertEquals(AdaptiveThreshold.DAYLIGHT.lux, preferences.adaptiveThemeThresholdLux)
		assertNull(preferences.customAdaptiveThemeThresholdLux)
		assertEquals(21 * 60, preferences.nightStartMinutes)
		assertEquals(6 * 60, preferences.nightEndMinutes)
	}

	@Test
	fun ensureDefaults_doesNotOverwriteExistingValues() = runTest {
		repository.updateAdaptiveThemeThresholdLux(100f)
		repository.updateNightWindow(20 * 60, 7 * 60)

		repository.ensureAdaptiveThemeThresholdDefault()
		repository.ensureNightDefaults()

		val preferences = repository.userPreferencesFlow.first()
		assertEquals(100f, preferences.adaptiveThemeThresholdLux)
		assertEquals(20 * 60, preferences.nightStartMinutes)
		assertEquals(7 * 60, preferences.nightEndMinutes)
	}

	@Test
	fun presetThreshold_clearsCustomThreshold() = runTest {
		repository.updateCustomAdaptiveThemeThresholdLux(42f)
		repository.updateAdaptiveThemeThresholdLux(100f)

		val preferences = repository.userPreferencesFlow.first()
		assertEquals(100f, preferences.adaptiveThemeThresholdLux)
		assertNull(preferences.customAdaptiveThemeThresholdLux)
	}

	@Test
	fun invalidNightWindow_isRejectedWithoutChangingStoredValues() = runTest {
		val updated = repository.updateNightWindow(60, 60)

		assertFalse(updated)
		val preferences = repository.userPreferencesFlow.first()
		assertEquals(21 * 60, preferences.nightStartMinutes)
		assertEquals(6 * 60, preferences.nightEndMinutes)
	}

	@Test
	fun updates_arePersistedTogether() = runTest {
		repository.updateAdaptiveThemeEnabled(true)
		repository.updateSetupCompleted(true)
		repository.updateStayDarkAtNightEnabled(true)
		assertTrue(repository.updateNightWindow(22 * 60, 5 * 60))

		val preferences = repository.fetchInitialPreferences()
		assertTrue(preferences.adaptiveThemeEnabled)
		assertTrue(preferences.hasSetupCompleted)
		assertTrue(preferences.stayDarkAtNightEnabled)
		assertEquals(22 * 60, preferences.nightStartMinutes)
		assertEquals(5 * 60, preferences.nightEndMinutes)
	}

	@Test
	fun readIOExceptionFallsBackToDefaults() = runTest {
		val failingDataStore = object : DataStore<Preferences> {
			override val data: Flow<Preferences> = flow {
				throw IOException("simulated read failure")
			}

			override suspend fun updateData(
				transform: suspend (t: Preferences) -> Preferences
			): Preferences = transform(emptyPreferences())
		}

		val preferences = UserPreferencesRepository(failingDataStore)
			.userPreferencesFlow
			.first()

		assertFalse(preferences.adaptiveThemeEnabled)
		assertEquals(AdaptiveThreshold.DAYLIGHT.lux, preferences.adaptiveThemeThresholdLux)
		assertEquals(21 * 60, preferences.nightStartMinutes)
		assertEquals(6 * 60, preferences.nightEndMinutes)
	}
}
