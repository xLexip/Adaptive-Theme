package dev.lexip.hecate.util.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import dev.lexip.hecate.analytics.AnalyticsLogger
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
		object Unexpected : GrantResult()
		data class ShellCommandFailed(val exitCode: Int) : GrantResult()
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

	fun hasPermission(context: Context): Boolean {
		if (Shizuku.isPreV11()) return false
		if (!binderReady) return false

		return try {
			Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
		} catch (t: Throwable) {
			Log.w(TAG, "Failed to check Shizuku permission", t)
			AnalyticsLogger.logUnexpectedShizukuError(
				context = context,
				operation = "check_permission",
				stage = "check_self_permission",
				throwable = t,
				binderReady = binderReady
			)
			false
		}
	}

	fun requestPermission() {
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

	fun executeGrantViaShizuku(context: Context, packageName: String): GrantResult {
		if (Shizuku.isPreV11()) return GrantResult.ServiceNotRunning
		if (!binderReady) return GrantResult.ServiceNotRunning
		if (!hasPermission(context)) return GrantResult.NotAuthorized

		val cmd = buildGrantWriteSecureSettingsCommand(packageName)

		return try {
			val monitor = Object()
			var result: GrantResult = GrantResult.Unexpected

			val args = createGrantServiceArgs()
			val connection = createGrantServiceConnection(
				context,
				args,
				cmd,
				monitor,
				packageName
			) { grantResult ->
				result = grantResult
			}

			Shizuku.bindUserService(args, connection)
			waitForGrantResult(monitor)

			result
		} catch (t: Throwable) {
			Log.e(TAG, "Grant via Shizuku failed", t)
			AnalyticsLogger.logUnexpectedShizukuError(
				context = context,
				operation = "grant_write_secure_settings",
				stage = "execute_grant_via_shizuku",
				throwable = t,
				binderReady = binderReady,
				packageName = packageName
			)
			GrantResult.Unexpected
		}
	}

	private fun createGrantServiceArgs(): Shizuku.UserServiceArgs {
		val component = ComponentName(
			"dev.lexip.hecate",
			GrantService::class.java.name
		)
		return Shizuku.UserServiceArgs(component)
			.processNameSuffix("shizuku_grant")
	}

	private fun createGrantServiceConnection(
		context: Context,
		args: Shizuku.UserServiceArgs,
		cmd: String,
		monitor: Object,
		packageName: String,
		onResult: (GrantResult) -> Unit
	): ServiceConnection {
		return object : ServiceConnection {
			override fun onServiceConnected(
				name: ComponentName?,
				binder: IBinder?
			) {
				val result = try {
					if (binder == null) {
						GrantResult.ServiceNotRunning
					} else {
						executeGrantTransaction(binder, cmd)
					}
				} catch (t: Throwable) {
					when (t) {
						is SecurityException -> GrantResult.NotAuthorized
						else -> {
							AnalyticsLogger.logUnexpectedShizukuError(
								context = context,
								operation = "grant_write_secure_settings",
								stage = "on_service_connected_execute",
								throwable = t,
								binderReady = binderReady,
								packageName = packageName
							)
							GrantResult.Unexpected
						}
					}
				} finally {
					try {
						Shizuku.unbindUserService(args, this, true)
					} catch (t: Throwable) {
						Log.w(TAG, "Error while unbinding Shizuku user service", t)
						AnalyticsLogger.logUnexpectedShizukuError(
							context = context,
							operation = "grant_write_secure_settings",
							stage = "unbind_user_service",
							throwable = t,
							binderReady = binderReady,
							packageName = packageName
						)
					}
					synchronized(monitor) {
						monitor.notifyAll()
					}
				}

				onResult(result)
			}

			override fun onServiceDisconnected(name: ComponentName?) {
				Log.d(TAG, "GrantService disconnected: $name")
			}
		}
	}

	private fun executeGrantTransaction(
		binder: IBinder,
		cmd: String
	): GrantResult {
		val data = Parcel.obtain()
		val reply = Parcel.obtain()
		return try {
			data.writeInterfaceToken("dev.lexip.hecate.util.shizuku.GrantService")
			data.writeString(cmd)
			val transactionCode = Binder.FIRST_CALL_TRANSACTION + 1
			val success = binder.transact(transactionCode, data, reply, 0)
			if (!success) {
				GrantResult.ServiceNotRunning
			} else {
				val exitCode = reply.readInt()
				if (exitCode == 0) GrantResult.Success
				else GrantResult.ShellCommandFailed(exitCode)
			}
		} finally {
			data.recycle()
			reply.recycle()
		}
	}

	private fun waitForGrantResult(monitor: Object) {
		synchronized(monitor) {
			try {
				monitor.wait(5000)
			} catch (t: InterruptedException) {
				Log.w(TAG, "Interrupted while waiting for Shizuku user service", t)
			}
		}
	}
}
