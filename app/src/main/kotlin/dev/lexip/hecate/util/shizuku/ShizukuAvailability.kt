package dev.lexip.hecate.util.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object ShizukuAvailability {

	private val SUPPORTED_PACKAGES = listOf(
		"com.hamondev.shevery",
		"kerneldroid.nightzuku",
		"af.shizuku.plus.api",
		"moe.shizuku.privileged.api"
	)
	private const val TAG = "ShizukuAvailability"

	fun isShizukuInstalled(context: Context): Boolean {
		return SUPPORTED_PACKAGES.any { isPackageInstalled(context, it) }
	}

	private fun isPackageInstalled(context: Context, packageName: String): Boolean {
		return try {
			context.packageManager.getPackageInfo(packageName, 0)
			Log.d(TAG, "Found package: $packageName")
			true
		} catch (_: PackageManager.NameNotFoundException) {
			Log.d(TAG, "Package not found: $packageName")
			false
		}
	}
}
