/*
 * Copyright (C) 2025-2026 xLexip <https://lexip.dev>
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

import android.content.Context

interface InstallMetadataProvider {
	fun isInstalledFromPlayStore(): Boolean
	fun daysSinceFirstInstall(): Long
}

class AndroidInstallMetadataProvider(
	private val context: Context
) : InstallMetadataProvider {
	override fun isInstalledFromPlayStore(): Boolean =
		InstallSourceChecker.fromPlayStore(context)

	override fun daysSinceFirstInstall(): Long =
		InstallSourceChecker.getDaysSinceFirstInstall(context)
}
