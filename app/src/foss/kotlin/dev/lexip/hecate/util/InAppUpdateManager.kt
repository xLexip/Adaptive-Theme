/*
 * Copyright (C) 2025 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0
 *
 * Please see the License for specific terms regarding permissions and limitations.
 */

package dev.lexip.hecate.util

import androidx.activity.ComponentActivity

class InAppUpdateManager(activity: ComponentActivity) {

	fun registerUpdateLauncher(activity: ComponentActivity) {
		// No-op
	}

	fun checkAndLaunchUpdate(
		onNoUpdate: () -> Unit = {},
		onError: (Throwable) -> Unit = {}
	) {
		onNoUpdate()
	}

	fun checkForImmediateUpdate(
		onNoUpdate: () -> Unit = {},
		onError: (Throwable) -> Unit = {}
	) {
		onNoUpdate()
	}

	fun checkForFlexibleUpdate(
		onNoUpdate: () -> Unit = {},
		onError: (Throwable) -> Unit = {}
	) {
		onNoUpdate()
	}

	fun resumeImmediateUpdateIfNeeded() {
		// No-op
	}

	fun resumeFlexibleUpdateIfNeeded() {
		// No-op
	}
}

