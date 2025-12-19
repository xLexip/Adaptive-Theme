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

package dev.lexip.hecate.ui.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Sealed hierarchy of navigation events that can be emitted from ViewModels
 * and consumed by the navigation host.
 */
@Suppress("unused") // Events are used via when-expressions in AppNavHost
sealed interface NavigationEvent {
	/** Navigate to the setup flow */
	data object ToSetup : NavigationEvent

	/** Navigate back (pop the back stack) */
	data object Back : NavigationEvent

	/** Navigate to main screen, clearing setup from back stack */
	data object ToMainClearingSetup : NavigationEvent

	/** Navigate to a specific setup step */
	data class ToSetupStep(val step: SetupRoute) : NavigationEvent
}

/**
 * Central navigation manager that decouples ViewModels from NavController.
 * ViewModels emit navigation events via this manager, and the AppNavHost
 * collects and handles them.
 */
class NavigationManager {
	private val _navigationEvents = MutableSharedFlow<NavigationEvent>(
		replay = 0,
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	)
	val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

	/**
	 * Emit a navigation event to be handled by the navigation host.
	 */
	suspend fun navigate(event: NavigationEvent) {
		_navigationEvents.emit(event)
	}

	/**
	 * Try to emit a navigation event without suspending.
	 * Returns true if the event was emitted, false if the buffer was full.
	 */
	fun tryNavigate(event: NavigationEvent): Boolean {
		return _navigationEvents.tryEmit(event)
	}
}

