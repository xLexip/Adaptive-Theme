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

package dev.lexip.hecate.analytics

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

object AnalyticsLogger {

	private fun analytics(context: Context): FirebaseAnalytics =
		FirebaseAnalytics.getInstance(context)

	private inline fun ifAllowed(block: () -> Unit) {
		if (AnalyticsGate.allowed()) block()
	}

	fun logPermissionErrorShown(context: Context, reason: String, attemptedAction: String) {
		ifAllowed {
			analytics(context).logEvent("permission_error_shown") {
				param("reason", reason)
				param("attempted_action", attemptedAction)
			}
		}
	}

	fun logServiceEnabled(context: Context, source: String) {
		ifAllowed {
			analytics(context).logEvent("adaptive_service_enabled") {
				param("source", source)
			}
		}
	}

	fun logServiceDisabled(context: Context, source: String) {
		ifAllowed {
			analytics(context).logEvent("adaptive_service_disabled") {
				param("source", source)
			}
		}
	}

	fun logBrightnessThresholdChanged(
		context: Context,
		oldLux: Float,
		newLux: Float
	) {
		if (oldLux == newLux) return
		ifAllowed {
			analytics(context).logEvent("brightness_threshold_changed") {
				param("old_lux", oldLux.toLong())
				param("new_lux", newLux.toLong())
			}
		}
	}

	fun logQuickSettingsTileAdded(context: Context) {
		ifAllowed {
			analytics(context).logEvent("qs_tile_added") { }
		}
	}

	fun logThemeSwitched(
		context: Context,
		targetMode: Int,
		succeeded: Boolean
	) {
		ifAllowed {
			analytics(context).logEvent("theme_switched") {
				param("target_mode", targetMode.toLong())
				param("succeeded", if (succeeded) 1L else 0L)
			}
		}
	}

	fun logOverflowMenuItemClicked(context: Context, menuItem: String) {
		ifAllowed {
			analytics(context).logEvent("overflow_menu_item_clicked") {
				param("menu_item", menuItem)
			}
		}
	}

	fun logViewWebsiteClicked(context: Context, source: String) {
		ifAllowed {
			analytics(context).logEvent("view_website_clicked") {
				param("source", source)
			}
		}
	}

	fun logShareLinkClicked(context: Context, source: String) {
		ifAllowed {
			analytics(context).logEvent("share_link_clicked") {
				param("source", source)
			}
		}
	}

	fun logSetupStarted(context: Context) {
		ifAllowed {
			analytics(context).logEvent("setup_started") { }
		}
	}

	fun logSetupStepOneCompleted(context: Context) {
		ifAllowed {
			analytics(context).logEvent("setup_step_one_completed") { }
		}
	}

	fun logSetupStepTwoCompleted(context: Context) {
		ifAllowed {
			analytics(context).logEvent("setup_step_two_completed") { }
		}
	}

	fun logSetupComplete(context: Context, source: String? = null) {
		ifAllowed {
			analytics(context).logEvent("setup_finished") {
				if (source != null) param("source", source)
			}
		}
	}

	fun logInAppUpdateInstalled(context: Context) {
		ifAllowed {
			analytics(context).logEvent("in_app_update_installed") { }
		}
	}
}
