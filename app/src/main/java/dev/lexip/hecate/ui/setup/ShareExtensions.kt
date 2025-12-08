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

package dev.lexip.hecate.ui.setup

import android.content.Intent

// Helper to share a URL via Android Sharesheet, reused by setup components.
internal fun android.content.Context.shareSetupUrl(url: String) {
	if (url.isBlank()) return

	val sendIntent = Intent().apply {
		action = Intent.ACTION_SEND
		putExtra(Intent.EXTRA_TEXT, url)
		putExtra(Intent.EXTRA_TITLE, "Setup - Adaptive Theme")
		type = "text/plain"
	}

	val shareIntent = Intent.createChooser(sendIntent, null)
	startActivity(shareIntent)
}

