package dev.lexip.hecate.util.shizuku

import android.os.Binder
import android.os.Parcel
import android.util.Log
import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.InputStreamReader

@Keep
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

				val first = data.readInt()
				val commands: List<String?> = if (first == -1) {
					listOf(data.readString())
				} else {
					val count = first
					buildList {
						repeat(count.coerceAtLeast(0)) {
							add(data.readString())
						}
					}
				}

				val (failedIndex, exitCode) = runShellAll(commands)
				reply?.writeInt(failedIndex)
				reply?.writeInt(exitCode)
				true
			}

			else -> super.onTransact(code, data, reply, flags)
		}
	}

	private fun runShellAll(commands: List<String?>): Pair<Int, Int> {
		commands.forEachIndexed { index, cmd ->
			if (cmd.isNullOrBlank()) {
				Log.w(TAG, "Received blank command at index=$index")
				return index to -1
			}

			Log.d(TAG, "Received command via Shizuku (index=$index): $cmd")
			val code = runShell(cmd)
			if (code != 0) return index to code
		}

		return -1 to 0
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
