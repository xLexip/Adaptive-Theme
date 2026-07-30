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

import android.content.Context
import android.content.pm.PackageManager
import dev.lexip.hecate.logging.Logger
import dev.lexip.hecate.util.shizuku.ShizukuManager
import rikka.shizuku.Shizuku
import java.io.DataOutputStream

sealed interface SetupGrantResult {
	data object Success : SetupGrantResult
	data object ServiceNotRunning : SetupGrantResult
	data object NotAuthorized : SetupGrantResult
	data class Failed(val reason: String) : SetupGrantResult
	data class Unexpected(val throwableName: String) : SetupGrantResult
}

fun interface SetupPermissionListenerRegistration {
	fun unregister()
}

interface SetupPermissionGrantController {
	fun registerShizukuPermissionListener(
		listener: (requestCode: Int, granted: Boolean) -> Unit
	): SetupPermissionListenerRegistration

	fun isShizukuBinderReady(): Boolean
	fun hasShizukuPermission(context: Context): Boolean
	fun requestShizukuPermission()
	suspend fun grantViaShizuku(context: Context, packageName: String): SetupGrantResult
	suspend fun grantViaRoot(packageName: String): SetupGrantResult
}

class AndroidSetupPermissionGrantController : SetupPermissionGrantController {
	override fun registerShizukuPermissionListener(
		listener: (requestCode: Int, granted: Boolean) -> Unit
	): SetupPermissionListenerRegistration {
		val platformListener =
			Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
				listener(requestCode, grantResult == PackageManager.PERMISSION_GRANTED)
			}
		Shizuku.addRequestPermissionResultListener(platformListener)
		return SetupPermissionListenerRegistration {
			Shizuku.removeRequestPermissionResultListener(platformListener)
		}
	}

	override fun isShizukuBinderReady(): Boolean = ShizukuManager.isBinderReady()

	override fun hasShizukuPermission(context: Context): Boolean =
		ShizukuManager.hasPermission(context)

	override fun requestShizukuPermission() {
		ShizukuManager.requestPermission()
	}

	override suspend fun grantViaShizuku(
		context: Context,
		packageName: String
	): SetupGrantResult {
		val result = ShizukuManager.executeGrantViaShizuku(context, packageName)
		Logger.logShizukuGrantResult(context, result, packageName)
		return when (result) {
			is ShizukuManager.GrantResult.Success -> SetupGrantResult.Success
			is ShizukuManager.GrantResult.ServiceNotRunning -> SetupGrantResult.ServiceNotRunning
			is ShizukuManager.GrantResult.NotAuthorized -> SetupGrantResult.NotAuthorized
			is ShizukuManager.GrantResult.ShellCommandFailed ->
				SetupGrantResult.Failed(result.toString())

			is ShizukuManager.GrantResult.Unexpected ->
				SetupGrantResult.Unexpected(result.toString())
		}
	}

	override suspend fun grantViaRoot(packageName: String): SetupGrantResult {
		for ((index, command) in ShizukuManager.buildAllGrantCommands(packageName).withIndex()) {
			when (val result = executeSingleRootCommand(command)) {
				RootCommandResult.Success -> Unit
				is RootCommandResult.Failure -> {
					return SetupGrantResult.Failed(
						"command_${index + 1}_exit_${result.exitCode}: ${result.command}"
					)
				}

				is RootCommandResult.Unexpected -> {
					return SetupGrantResult.Unexpected(result.throwableName)
				}
			}
		}
		return SetupGrantResult.Success
	}

	private fun executeSingleRootCommand(command: String): RootCommandResult = try {
		val process = Runtime.getRuntime().exec("su")
		DataOutputStream(process.outputStream).use { output ->
			output.writeBytes("$command\n")
			output.writeBytes("exit\n")
			output.flush()
		}
		val exitCode = process.waitFor()
		if (exitCode == 0) {
			RootCommandResult.Success
		} else {
			RootCommandResult.Failure(command = command, exitCode = exitCode)
		}
	} catch (exception: Exception) {
		RootCommandResult.Unexpected(
			command = command,
			throwableName = exception.javaClass.simpleName
		)
	}

	private sealed interface RootCommandResult {
		data object Success : RootCommandResult
		data class Failure(val command: String, val exitCode: Int) : RootCommandResult
		data class Unexpected(val command: String, val throwableName: String) : RootCommandResult
	}
}
