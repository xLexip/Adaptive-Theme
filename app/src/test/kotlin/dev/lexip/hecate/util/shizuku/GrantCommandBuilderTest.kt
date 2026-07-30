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

import org.junit.Assert.assertEquals
import org.junit.Test

class GrantCommandBuilderTest {
	@Test
	fun all_buildsCommandsInRequiredOrder() {
		assertEquals(
			listOf(
				"pm grant dev.example android.permission.WRITE_SECURE_SETTINGS",
				"cmd appops set dev.example RUN_ANY_IN_BACKGROUND allow",
				"dumpsys deviceidle whitelist +dev.example"
			),
			GrantCommandBuilder.all("dev.example")
		)
	}

	@Test
	fun adbCommand_prefixesShellInvocation() {
		assertEquals(
			"adb shell pm grant dev.example android.permission.WRITE_SECURE_SETTINGS",
			GrantCommandBuilder.adbGrantWriteSecureSettings("dev.example")
		)
	}
}
