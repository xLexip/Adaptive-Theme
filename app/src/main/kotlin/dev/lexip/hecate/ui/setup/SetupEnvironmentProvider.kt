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

package dev.lexip.hecate.ui.setup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import dev.lexip.hecate.util.shizuku.ShizukuAvailability

data class SetupEnvironmentSnapshot(
	val isShizukuInstalled: Boolean,
	val isDeveloperOptionsEnabled: Boolean,
	val isUsbDebuggingEnabled: Boolean,
	val hasWriteSecureSettings: Boolean,
	val isUsbConnected: Boolean
)

fun interface SetupEnvironmentProvider {
	fun snapshot(): SetupEnvironmentSnapshot
}

class AndroidSetupEnvironmentProvider(
	private val context: Context
) : SetupEnvironmentProvider {
	override fun snapshot(): SetupEnvironmentSnapshot = SetupEnvironmentSnapshot(
		isShizukuInstalled = ShizukuAvailability.isShizukuInstalled(context),
		isDeveloperOptionsEnabled = readGlobalSetting(
			Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
		),
		isUsbDebuggingEnabled = readGlobalSetting(Settings.Global.ADB_ENABLED),
		hasWriteSecureSettings = ContextCompat.checkSelfPermission(
			context,
			Manifest.permission.WRITE_SECURE_SETTINGS
		) == PackageManager.PERMISSION_GRANTED,
		isUsbConnected = isUsbConnected()
	)

	private fun readGlobalSetting(key: String): Boolean = try {
		Settings.Global.getInt(context.contentResolver, key, 0) == 1
	} catch (_: Exception) {
		false
	}

	private fun isUsbConnected(): Boolean = try {
		val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
		usbManager?.deviceList?.isNotEmpty() == true
	} catch (_: Exception) {
		false
	}
}
