/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 * Licensed under the GNU General Public License, Version 3.0.
 * https://www.gnu.org/licenses/gpl-3.0
 */
package dev.lexip.hecate.ui

/** Patch and build suffixes share the announcement for their major/minor release. */
internal class WhatsNewRelease private constructor(
	val major: Int,
	val minor: Int
) : Comparable<WhatsNewRelease> {
	val id: String get() = "$major.$minor"
	val releaseNotesUrl: String
		get() = "https://github.com/xLexip/Adaptive-Theme/releases"

	fun shouldAnnounce(acknowledgedRelease: String?, hasSetupCompleted: Boolean): Boolean {
		if (!hasSetupCompleted) return false
		val acknowledged = acknowledgedRelease?.let(::parse) ?: return true
		return this > acknowledged
	}

	override fun compareTo(other: WhatsNewRelease): Int =
		compareValuesBy(this, other, WhatsNewRelease::major, WhatsNewRelease::minor)

	companion object {
		private val versionPattern = Regex("^(\\d+)\\.(\\d+)(?:\\.\\d+)?(?:[-+][0-9A-Za-z.+-]+)?$")

		fun parse(version: String): WhatsNewRelease? {
			val match = versionPattern.matchEntire(version) ?: return null
			val major = match.groupValues[1].toIntOrNull() ?: return null
			val minor = match.groupValues[2].toIntOrNull() ?: return null
			return WhatsNewRelease(major, minor)
		}
	}
}
