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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface DispatcherProvider {
	val main: CoroutineDispatcher
	val io: CoroutineDispatcher
	val default: CoroutineDispatcher
	val unconfined: CoroutineDispatcher
}

object DefaultDispatcherProvider : DispatcherProvider {
	override val main: CoroutineDispatcher = Dispatchers.Main
	override val io: CoroutineDispatcher = Dispatchers.IO
	override val default: CoroutineDispatcher = Dispatchers.Default
	override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}

