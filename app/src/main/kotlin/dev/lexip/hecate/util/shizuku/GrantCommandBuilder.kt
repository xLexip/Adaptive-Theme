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

package dev.lexip.hecate.util.shizuku

object GrantCommandBuilder {
	fun grantWriteSecureSettings(packageName: String): String =
		"pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"

	fun all(packageName: String): List<String> =
		listOf(
			grantWriteSecureSettings(packageName),
			"cmd appops set $packageName RUN_ANY_IN_BACKGROUND allow",
			"dumpsys deviceidle whitelist +$packageName"
		)

	fun adbGrantWriteSecureSettings(packageName: String): String =
		"adb shell ${grantWriteSecureSettings(packageName)}"
}
