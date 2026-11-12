/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.lexip.hecate.services

/** The compatibility retry is only for ROMs rejecting the explicit service type. */
internal fun promoteForegroundService(
	promoteTyped: () -> Unit,
	promoteCompatibility: () -> Unit,
	onFailure: (stage: String, cause: Exception) -> Unit
): Boolean {
	try {
		promoteTyped()
		return true
	} catch (cause: SecurityException) {
		// Complete the compatibility attempt before spending time reporting diagnostics.
		var compatibilityFailure: Exception? = null
		val promoted = try {
			promoteCompatibility()
			true
		} catch (failure: Exception) {
			compatibilityFailure = failure
			false
		}
		onFailure("typed", cause)
		compatibilityFailure?.let { onFailure("compatibility", it) }
		return promoted
	} catch (cause: Exception) {
		onFailure("typed", cause)
		return false
	}
}
