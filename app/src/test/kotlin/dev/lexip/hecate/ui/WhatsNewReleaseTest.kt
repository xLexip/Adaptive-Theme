/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 * Licensed under the GNU General Public License, Version 3.0.
 * https://www.gnu.org/licenses/gpl-3.0
 */
package dev.lexip.hecate.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewReleaseTest {
	@Test
	fun minorMajorAndSkippedReleasesAreAnnounced() {
		assertTrue(release("2.7.0").shouldAnnounce("2.6", true))
		assertTrue(release("3.0.0").shouldAnnounce("2.9", true))
		assertTrue(release("2.10.1").shouldAnnounce("2.6", true))
	}

	@Test
	fun patchesAndBuildSuffixesShareOneAnnouncement() {
		for (version in listOf("2.6.0", "2.6.1", "2.6.12-beta", "2.6.0-debug", "2.6.0+123")) {
			assertFalse(version, release(version).shouldAnnounce("2.6", true))
			assertEquals("2.6", release(version).id)
		}
	}

	@Test
	fun unreadMinorAnnouncementSurvivesPatchAndLinksToReleasesOverview() {
		val patchedRelease = release("2.7.3-debug")
		assertTrue(patchedRelease.shouldAnnounce("2.6", true))
		assertEquals("https://github.com/xLexip/Adaptive-Theme/releases", patchedRelease.releaseNotesUrl)
		assertEquals(patchedRelease.releaseNotesUrl, release("3.0.0").releaseNotesUrl)
	}

	@Test
	fun freshInstallsAreSkippedAndExistingUsersReceiveInitialAnnouncement() {
		assertFalse(release("2.6.0").shouldAnnounce(null, false))
		assertTrue(release("2.6.0").shouldAnnounce(null, true))
	}

	@Test
	fun downgradesAndReturningToAnAcknowledgedVersionAreSilent() {
		assertFalse(release("2.6.0").shouldAnnounce("3.0", true))
		assertFalse(release("3.0.1").shouldAnnounce("3.0", true))
	}

	@Test
	fun malformedOrOverflowingVersionNamesAreIgnored() {
		for (version in listOf("", "next", "2", "2.6.x", "-1.2.0", "2147483648.0.0")) {
			assertNull(version, WhatsNewRelease.parse(version))
		}
	}

	private fun release(version: String) = requireNotNull(WhatsNewRelease.parse(version))
}
