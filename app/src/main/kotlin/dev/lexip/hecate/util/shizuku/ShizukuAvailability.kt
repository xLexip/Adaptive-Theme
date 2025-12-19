package dev.lexip.hecate.util.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object ShizukuAvailability {

	private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
	private const val TAG = "ShizukuAvailability"

	fun isShizukuInstalled(context: Context): Boolean {
		return try {
			context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
			Log.d(TAG, "Found Shizuku package: $SHIZUKU_PACKAGE")
			true
		} catch (_: PackageManager.NameNotFoundException) {
			Log.d(TAG, "Shizuku package not found: $SHIZUKU_PACKAGE")
			false
		}
	}
}
