/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 * Licensed under the GNU General Public License, Version 3.0.
 * https://www.gnu.org/licenses/gpl-3.0
 */
package dev.lexip.hecate.logging

import android.content.Context
import android.util.Log

internal enum class BackgroundGuidanceAction(val analyticsValue: String) {
	IMPRESSION("impression"), OPEN_GUIDE("open_guide"), OPEN_SETTINGS("open_settings"), DISMISS("dismiss")
}

internal enum class BackgroundGuidanceOutcome(val analyticsValue: String) {
	SHOWN("shown"), OPENED("opened"), FALLBACK_OPENED("fallback_opened"),
	UNAVAILABLE("unavailable"), DISMISSED("dismissed")
}

/** Local logs for diagnostics; remote collection uses the existing flavor-specific Logger gate. */
internal object BackgroundGuidanceTelemetry {
	fun record(
		context: Context,
		action: BackgroundGuidanceAction,
		outcome: BackgroundGuidanceOutcome,
		guideVendor: String
	) {
		Log.d("BackgroundGuidance", "action=${action.analyticsValue} outcome=${outcome.analyticsValue} guideVendor=$guideVendor")
		try {
			Logger.logBackgroundGuidance(context, action.analyticsValue, outcome.analyticsValue, guideVendor)
		} catch (cause: Exception) {
			// A logging failure must not block navigation or permanent dismissal.
			Log.w("BackgroundGuidance", "Unable to record prompt interaction", cause)
		}
	}
}
