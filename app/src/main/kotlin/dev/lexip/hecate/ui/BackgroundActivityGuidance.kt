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

package dev.lexip.hecate.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri
import dev.lexip.hecate.R
import java.util.Locale

enum class BackgroundActivityGuidance(val guideUrl: String) {
	MOTOROLA("https://dontkillmyapp.com/motorola"),
	ONEPLUS("https://dontkillmyapp.com/oneplus"),
	XIAOMI("https://dontkillmyapp.com/xiaomi"),
	REALME("https://dontkillmyapp.com/realme"),
	OPPO("https://dontkillmyapp.com/oppo"),
	OTHER("https://dontkillmyapp.com");

	fun shouldShowHint(monitoringEnabled: Boolean, dismissed: Boolean, hasInterruptionEvidence: Boolean): Boolean =
		this != OTHER && monitoringEnabled && !dismissed && hasInterruptionEvidence

	companion object {
		fun forManufacturer(manufacturer: String): BackgroundActivityGuidance =
			when (manufacturer.trim().lowercase(Locale.ROOT)) {
				"motorola" -> MOTOROLA
				"oneplus" -> ONEPLUS
				"xiaomi", "redmi", "poco" -> XIAOMI
				"realme" -> REALME
				"oppo" -> OPPO
				else -> OTHER
			}
	}
}

internal fun openBackgroundActivitySettings(context: Context) {
	val appSettings = Intent(
		Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
		"package:${context.packageName}".toUri()
	)
	if (!tryOpenBackgroundHelpActivity(context, appSettings) &&
		!tryOpenBackgroundHelpActivity(context, Intent(Settings.ACTION_SETTINGS))
	) {
		showBackgroundHelpUnavailable(context)
	}
}

internal fun openBackgroundActivityGuide(context: Context, guidance: BackgroundActivityGuidance) {
	if (!tryOpenBackgroundHelpActivity(context, Intent(Intent.ACTION_VIEW, guidance.guideUrl.toUri()))) {
		showBackgroundHelpUnavailable(context)
	}
}

private fun tryOpenBackgroundHelpActivity(context: Context, intent: Intent): Boolean = try {
	context.startActivity(intent)
	true
} catch (_: ActivityNotFoundException) {
	false
} catch (_: SecurityException) {
	false
}

private fun showBackgroundHelpUnavailable(context: Context) {
	Toast.makeText(context, R.string.background_help_unavailable, Toast.LENGTH_LONG).show()
}
