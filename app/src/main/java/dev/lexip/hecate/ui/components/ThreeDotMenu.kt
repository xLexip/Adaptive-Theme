package dev.lexip.hecate.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.lexip.hecate.BuildConfig
import dev.lexip.hecate.R
import dev.lexip.hecate.analytics.AnalyticsGate
import dev.lexip.hecate.analytics.AnalyticsLogger
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


const val FEEDBACK_SUBJECT = "Adaptive Theme Feedback (v${BuildConfig.VERSION_NAME})"

@Composable
fun ThreeDotMenu(
	isAdaptiveThemeEnabled: Boolean,
	packageName: String,
	onShowCustomThresholdDialog: () -> Unit,
	onAboutClick: () -> Unit = {}
) {
	val context = LocalContext.current
	var menuExpanded by remember { mutableStateOf(false) }

	Box {
		IconButton(onClick = { menuExpanded = true }) {
			Icon(
				imageVector = Icons.Filled.MoreVert,
				contentDescription = stringResource(id = R.string.title_more)
			)
		}


		MaterialTheme(
			shapes = MaterialTheme.shapes.copy(
				extraSmall = RoundedCornerShape(14.dp),
				small = RoundedCornerShape(14.dp),
				medium = RoundedCornerShape(14.dp)
			)
		) {
			DropdownMenu(
				expanded = menuExpanded,
				onDismissRequest = { menuExpanded = false },
				modifier = Modifier.clip(MaterialTheme.shapes.extraSmall),
				containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
			) {

				// 1) Custom Threshold
				DropdownMenuItem(
					text = { Text(text = stringResource(id = R.string.title_custom_threshold)) },
					enabled = isAdaptiveThemeEnabled,
					onClick = {
						menuExpanded = false
						AnalyticsLogger.logOverflowMenuItemClicked(
							context,
							"custom_threshold"
						)
						if (isAdaptiveThemeEnabled) {
							onShowCustomThresholdDialog()
						}
					}
				)

				// 2) Change Language (Android 13+)
				DropdownMenuItem(
					text = { Text(text = stringResource(id = R.string.title_change_language)) },
					onClick = {
						menuExpanded = false
						AnalyticsLogger.logOverflowMenuItemClicked(
							context,
							"change_language"
						)
						val intent =
							Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
								data = "package:$packageName".toUri()
							}
						context.startActivity(intent)
					}
				)

				// 3) Send Feedback
				DropdownMenuItem(
					text = { Text(text = stringResource(id = R.string.title_send_feedback)) },
					onClick = {
						menuExpanded = false
						AnalyticsLogger.logOverflowMenuItemClicked(
							context,
							"send_feedback"
						)
						val encodedSubject = URLEncoder.encode(
							FEEDBACK_SUBJECT,
							StandardCharsets.UTF_8.toString()
						)
						val feedbackUri =
							"https://lexip.dev/hecate/feedback?subject=$encodedSubject".toUri()
						val feedbackIntent = Intent(Intent.ACTION_VIEW, feedbackUri)
						context.startActivity(feedbackIntent)

					}
				)

				// 4) Beta Feedback (only on beta builds)
				if (BuildConfig.VERSION_NAME.contains("-beta") && AnalyticsGate.isPlayStoreInstall()) {
					DropdownMenuItem(
						text = { Text(text = "Beta Feedback") },
						onClick = {
							menuExpanded = false
							AnalyticsLogger.logOverflowMenuItemClicked(
								context,
								"beta_feedback"
							)
							val betaUri =
								"https://play.google.com/store/apps/details?id=dev.lexip.hecate".toUri()
							val betaIntent = Intent(Intent.ACTION_VIEW, betaUri)
							context.startActivity(betaIntent)
						}
					)
				}

				// 5) Support the project
				DropdownMenuItem(
					text = { Text(text = stringResource(R.string.title_support_project)) },
					onClick = {
						menuExpanded = false
						AnalyticsLogger.logOverflowMenuItemClicked(
							context,
							"support_project"
						)
						val supportUri =
							"https://github.com/xLexip/Adaptive-Theme?tab=readme-ov-file#%EF%B8%8F-support-the-project".toUri()
						val supportIntent = Intent(Intent.ACTION_VIEW, supportUri)
						try {
							context.startActivity(supportIntent)
						} catch (_: ActivityNotFoundException) {
							context.startActivity(Intent(Intent.ACTION_VIEW, supportUri))
						}
					}
				)

				// 6) About
				DropdownMenuItem(
					text = { Text(stringResource(R.string.title_about_github)) },
					onClick = {
						menuExpanded = false
						AnalyticsLogger.logOverflowMenuItemClicked(context, "about")
						val aboutUri = "https://lexip.dev/hecate/about".toUri()
						val aboutIntent = Intent(Intent.ACTION_VIEW, aboutUri)
						Toast.makeText(
							context,
							"v${BuildConfig.VERSION_NAME}",
							Toast.LENGTH_SHORT
						).show()
						try {
							context.startActivity(aboutIntent)
						} catch (_: ActivityNotFoundException) {
							context.startActivity(Intent(Intent.ACTION_VIEW, aboutUri))
						}
						onAboutClick()
					}
				)
			}
		}
	}
}
