package dev.lexip.hecate.util.shizuku

import android.os.Binder
import android.os.Parcel
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class GrantService : Binder() {

	companion object {
		private const val TAG = "GrantService"
		private const val DESCRIPTOR = "dev.lexip.hecate.util.shizuku.GrantService"
		private const val TRANSACTION_EXECUTE_CMD = FIRST_CALL_TRANSACTION + 1
	}

	override fun onTransact(
		code: Int,
		data: Parcel,
		reply: Parcel?,
		flags: Int
	): Boolean {
		return when (code) {
			INTERFACE_TRANSACTION -> {
				reply?.writeString(DESCRIPTOR)
				true
			}

			TRANSACTION_EXECUTE_CMD -> {
				data.enforceInterface(DESCRIPTOR)
				val cmd = data.readString()
				Log.d(TAG, "Received command via Shizuku: $cmd")
				val exitCode = runShell(cmd)
				reply?.writeInt(exitCode)
				true
			}

			else -> super.onTransact(code, data, reply, flags)
		}
	}

	private fun runShell(cmd: String?): Int {
		if (cmd.isNullOrBlank()) return -1

		return try {
			val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
			val out = BufferedReader(InputStreamReader(proc.inputStream))
			val err = BufferedReader(InputStreamReader(proc.errorStream))

			out.lineSequence().forEach { Log.d(TAG, "shizuku-out: $it") }
			err.lineSequence().forEach { Log.w(TAG, "shizuku-err: $it") }

			proc.waitFor()
		} catch (t: Throwable) {
			Log.e(TAG, "Shell execution failed", t)
			-1
		}
	}
}
