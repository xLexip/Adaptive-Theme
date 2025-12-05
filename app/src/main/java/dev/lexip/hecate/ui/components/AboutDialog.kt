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

package dev.lexip.hecate.ui.components

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.R

@Composable
fun AboutDialog(show: Boolean, onDismiss: () -> Unit) {
	if (!show) return

	val info = rememberAppAboutInfo()

	AlertDialog(
		onDismissRequest = onDismiss,
		title = {},
		text = {
			Column(
				modifier = Modifier.fillMaxWidth(),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.Center
			) {
				Image(
					painter = painterResource(id = R.drawable.ic_app),
					contentDescription = null,
					modifier = Modifier.size(64.dp),
					colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
				)

				Spacer(modifier = Modifier.height(12.dp))
				Text(
					text = info.appName,
					style = MaterialTheme.typography.titleLarge,
					textAlign = TextAlign.Center
				)
				Spacer(modifier = Modifier.height(6.dp))
				Text(
					text = info.packageName,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					textAlign = TextAlign.Center
				)
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = "v${info.version}",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					textAlign = TextAlign.Center
				)
				Spacer(modifier = Modifier.height(32.dp))
				Text(
					text = stringResource(id = R.string.dialog_about_copyright),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					textAlign = TextAlign.Center
				)
			}
		},
		confirmButton = {
			TextButton(onClick = {
				onDismiss()
			}) {
				Text(text = stringResource(id = R.string.action_close))
			}
		}
	)
}

@Composable
fun rememberAppAboutInfo(): AppAboutInfo {
	val context = LocalContext.current
	return remember(context.packageName) {
		val pm = context.packageManager
		val pkg = context.packageName
		val packageInfo = pm.getPackageInfo(pkg, 0)
		val versionName =
			packageInfo.versionName ?: context.getString(R.string.dialog_about_version_unknown)
		val appInfo: ApplicationInfo = try {
			pm.getApplicationInfo(pkg, 0)
		} catch (_: PackageManager.NameNotFoundException) {
			context.applicationInfo
		}
		val appLabel = pm.getApplicationLabel(appInfo).toString()
		val icon = try {
			pm.getApplicationIcon(appInfo)
		} catch (_: Exception) {
			null
		}
		AppAboutInfo(
			appName = appLabel,
			version = versionName,
			packageName = pkg,
			icon = icon
		)
	}
}

data class AppAboutInfo(
	val appName: String,
	val version: String,
	val packageName: String,
	val icon: Drawable?
)
