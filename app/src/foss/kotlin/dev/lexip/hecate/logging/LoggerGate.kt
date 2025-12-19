/*
 * Copyright (C) 2025 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gnu.org/licenses/gpl-3.0
 *
 * Please see the License for specific terms regarding permissions and limitations.
 */

package dev.lexip.hecate.logging

import android.content.Context

object LoggerGate {
	fun init(context: Context) {
		// No-op
	}

	fun allowed(): Boolean = false
	fun isPlayStoreInstall(): Boolean = false
}

