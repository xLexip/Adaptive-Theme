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

// Suppress unused warnings - routes are used via KSP-generated code
@file:Suppress("unused")

package dev.lexip.hecate.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Deep link URIs used throughout the app.
 * These can be launched via adb or external apps.
 */
object DeepLinks {
	const val SCHEME = "hecate"
	const val HOST = "app"
	const val BASE_URI = "$SCHEME://$HOST"

	const val MAIN = "$BASE_URI/main"
	const val SETUP = "$BASE_URI/setup"
	const val SETUP_DEVELOPER = "$BASE_URI/setup/developer"
	const val SETUP_USB = "$BASE_URI/setup/usb"
	const val SETUP_PERMISSION = "$BASE_URI/setup/permission"
}

/** Main screen destination  */
@Serializable
data object MainRoute

/** Setup flow graph - setup step destinations */
@Serializable
data object SetupGraph

/** Setup step destinations within the setup graph */
@Serializable
sealed interface SetupRoute {
	/** Step 1: Enable developer mode and USB debugging */
	@Serializable
	data object DeveloperMode : SetupRoute

	/** Step 2: Connect to another device */
	@Serializable
	data object ConnectUsb : SetupRoute

	/** Step 3: Grant WRITE_SECURE_SETTINGS permission */
	@Serializable
	data object GrantPermission : SetupRoute
}
