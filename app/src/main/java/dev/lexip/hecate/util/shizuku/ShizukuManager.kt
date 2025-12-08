package dev.lexip.hecate.util.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import rikka.shizuku.Shizuku

object ShizukuManager {

	private const val TAG = "ShizukuManager"
	private const val REQUEST_CODE = 1001

	@Volatile
	private var binderReady: Boolean = false

	sealed class GrantResult {
		object Success : GrantResult()
		object ServiceNotRunning : GrantResult()
		object NotAuthorized : GrantResult()
		data class ShellCommandFailed(val exitCode: Int) : GrantResult()
		data class Unexpected(val error: Throwable) : GrantResult()
	}

	init {
		try {
			Shizuku.addBinderReceivedListener { onBinderReceived() }
			Shizuku.addBinderDeadListener { onBinderDead() }
		} catch (t: Throwable) {
			Log.w(TAG, "Failed to register Shizuku binder listeners", t)
		}
	}

	private fun onBinderReceived() {
		binderReady = true
	}

	private fun onBinderDead() {
		binderReady = false
	}

	fun isBinderReady(): Boolean = binderReady

	fun hasPermission(): Boolean {
		if (Shizuku.isPreV11()) return false
		if (!binderReady) return false

		return try {
			Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
		} catch (t: Throwable) {
			Log.w(TAG, "Failed to check Shizuku permission", t)
			false
		}
	}

	fun requestPermission(@Suppress("UNUSED_PARAMETER") context: Context) {
		if (Shizuku.isPreV11()) {
			Log.w(TAG, "Ignoring Shizuku.requestPermission on pre-v11")
			return
		}

		if (!binderReady) {
			Log.w(TAG, "Binder not ready, skipping Shizuku.requestPermission")
			return
		}

		if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
			return
		}

		if (Shizuku.shouldShowRequestPermissionRationale()) {
			return
		}

		Shizuku.requestPermission(REQUEST_CODE)
	}

	fun buildGrantWriteSecureSettingsCommand(packageName: String): String =
		"pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"

	fun executeGrantViaShizuku(packageName: String): GrantResult {
		if (Shizuku.isPreV11()) return GrantResult.ServiceNotRunning
		if (!binderReady) return GrantResult.ServiceNotRunning
		if (!hasPermission()) return GrantResult.NotAuthorized

		val cmd = buildGrantWriteSecureSettingsCommand(packageName)

		return try {
			val monitor = Object()
			var result: GrantResult = GrantResult.Unexpected(IllegalStateException("No result"))

			val component = ComponentName(
				"dev.lexip.hecate",
				GrantService::class.java.name
			)
			val args = Shizuku.UserServiceArgs(component)
				.processNameSuffix("shizuku_grant")

			val connection = object : ServiceConnection {
				override fun onServiceConnected(
					name: ComponentName?,
					binder: IBinder?
				) {
					if (binder == null) {
						result = GrantResult.ServiceNotRunning
						synchronized(monitor) { monitor.notifyAll() }
						return
					}

					try {
						val data = Parcel.obtain()
						val reply = Parcel.obtain()
						try {
							data.writeInterfaceToken("dev.lexip.hecate.util.shizuku.GrantService")
							data.writeString(cmd)
							val transactionCode = Binder.FIRST_CALL_TRANSACTION + 1
							val success = binder.transact(transactionCode, data, reply, 0)
							result = if (success) {
								val exitCode = reply.readInt()
								if (exitCode == 0) GrantResult.Success
								else GrantResult.ShellCommandFailed(exitCode)
							} else {
								GrantResult.ServiceNotRunning
							}
						} finally {
							data.recycle()
							reply.recycle()
						}
					} catch (t: Throwable) {
						result = when (t) {
							is SecurityException -> GrantResult.NotAuthorized
							else -> GrantResult.Unexpected(t)
						}
					} finally {
						try {
							Shizuku.unbindUserService(args, this, true)
						} catch (t: Throwable) {
							Log.w(TAG, "Error while unbinding Shizuku user service", t)
						}
						synchronized(monitor) { monitor.notifyAll() }
					}
				}

				override fun onServiceDisconnected(name: ComponentName?) {
					Log.d(TAG, "GrantService disconnected: $name")
				}
			}

			Shizuku.bindUserService(args, connection)

			synchronized(monitor) {
				try {
					monitor.wait(5000)
				} catch (t: InterruptedException) {
					Log.w(TAG, "Interrupted while waiting for Shizuku user service", t)
				}
			}

			result
		} catch (t: Throwable) {
			Log.e(TAG, "Grant via Shizuku failed", t)
			GrantResult.Unexpected(t)
		}
	}
}
