/*
 * Copyright (C) 2024-2025 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0
 *
 * Please see the License for specific terms regarding permissions and limitations.
 */

package dev.lexip.hecate.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.lexip.hecate.BuildConfig
import dev.lexip.hecate.R
import dev.lexip.hecate.data.AdaptiveThreshold
import dev.lexip.hecate.ui.components.MainSwitchPreferenceCard
import dev.lexip.hecate.ui.components.PermissionMissingDialog
import dev.lexip.hecate.ui.components.preferences.CustomThresholdDialog
import dev.lexip.hecate.ui.components.preferences.ProgressDetailCard
import dev.lexip.hecate.ui.components.preferences.SliderDetailCard
import dev.lexip.hecate.ui.theme.hecateTopAppBarColors
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveThemeScreen(
	uiState: AdaptiveThemeUiState,
	onAboutClick: () -> Unit = {}
) {
	val scrollBehavior =
		TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
	val horizontalOffsetPadding = 8.dp

	val context = LocalContext.current
	val haptic = LocalHapticFeedback.current
	val packageName = context.packageName

	val adaptiveThemeViewModel: AdaptiveThemeViewModel = viewModel(
		factory = AdaptiveThemeViewModelFactory(
			context.applicationContext as dev.lexip.hecate.HecateApplication,
			dev.lexip.hecate.data.UserPreferencesRepository((context.applicationContext as dev.lexip.hecate.HecateApplication).userPreferencesDataStore),
			dev.lexip.hecate.util.DarkThemeHandler(context)
		)
	)

	val showMissingPermissionDialog by adaptiveThemeViewModel.showMissingPermissionDialog.collectAsState()
	val pendingAdbCommand by adaptiveThemeViewModel.pendingAdbCommand.collectAsState()

	val showCustomDialog = remember { mutableStateOf(false) }

	LaunchedEffect(adaptiveThemeViewModel) {
		adaptiveThemeViewModel.uiEvents.collect { event ->
			when (event) {
				is UiEvent.CopyToClipboard -> {
					val clipboard = context.getSystemService(ClipboardManager::class.java)
					val clip = ClipData.newPlainText("ADB Command", event.text)
					clipboard?.setPrimaryClip(clip)
				}
			}
		}
	}

	Scaffold(
		modifier = Modifier
			.nestedScroll(scrollBehavior.nestedScrollConnection),
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
		topBar = {
			LargeTopAppBar(
				modifier = Modifier
					.padding(horizontal = horizontalOffsetPadding)
					.padding(top = 22.dp, bottom = 12.dp),
				colors = hecateTopAppBarColors(),
				title = {
					Text(
						text = stringResource(id = R.string.app_name),
						style = MaterialTheme.typography.displaySmall,
					)
				},
				actions = {
					val noEmailClientMessage = stringResource(id = R.string.error_no_email_client)
					var menuExpanded by remember { mutableStateOf(false) }
					androidx.compose.foundation.layout.Box {
						IconButton(onClick = { menuExpanded = true }) {
							Icon(
								imageVector = Icons.Filled.MoreVert,
								contentDescription = stringResource(id = R.string.title_more)
							)
						}
						DropdownMenu(
							expanded = menuExpanded,
							onDismissRequest = { menuExpanded = false }
						) {
							val feedbackSubject =
								"Adaptive Theme Feedback (v${BuildConfig.VERSION_NAME})"

							// 1) Custom Threshold
							DropdownMenuItem(
								text = { Text(text = stringResource(id = R.string.title_custom_threshold)) },
								enabled = uiState.adaptiveThemeEnabled,
								onClick = {
									menuExpanded = false
									if (uiState.adaptiveThemeEnabled) {
										showCustomDialog.value = true
									}
								}
							)

							// 2) Change Language (Android 13+)
							if (android.os.Build.VERSION.SDK_INT >= 33) {
								DropdownMenuItem(
									text = { Text(text = stringResource(id = R.string.title_change_language)) },
									onClick = {
										menuExpanded = false
										val intent =
											Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
												data = "package:$packageName".toUri()
												addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
											}
										try {
											context.startActivity(intent)
										} catch (_: ActivityNotFoundException) {
											Toast.makeText(
												context,
												context.getString(R.string.error_no_app_language_settings),
												Toast.LENGTH_SHORT
											).show()
										}
									}
								)
							}

							// 3) Send Feedback
							DropdownMenuItem(
								text = { Text(text = stringResource(id = R.string.title_send_feedback)) },
								onClick = {
									menuExpanded = false
									val encodedSubject = URLEncoder.encode(
										feedbackSubject,
										StandardCharsets.UTF_8.toString()
									)
									val feedbackUri =
										"https://lexip.dev/hecate/feedback?subject=$encodedSubject".toUri()
									val feedbackIntent = Intent(Intent.ACTION_VIEW, feedbackUri)
									try {
										context.startActivity(feedbackIntent)
									} catch (_: ActivityNotFoundException) {
										Toast.makeText(
											context,
											noEmailClientMessage,
											Toast.LENGTH_SHORT
										).show()
									}
								}
							)

							// 4) About
							DropdownMenuItem(
								text = { Text(stringResource(R.string.title_about)) },
								onClick = {
									menuExpanded = false
									val aboutUri = "https://lexip.dev/hecate/about".toUri()
									val aboutIntent = Intent(Intent.ACTION_VIEW, aboutUri)
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
				},
				scrollBehavior = scrollBehavior
			)
		}
	) { innerPadding ->
		Column(
			modifier = Modifier
				.windowInsetsPadding(WindowInsets.systemGestures.only(WindowInsetsSides.Horizontal))
				.fillMaxSize()
				.padding(innerPadding)
				.verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(32.dp)

		) {
			Text(
				modifier = Modifier.padding(horizontal = horizontalOffsetPadding),
				text = stringResource(id = R.string.description_adaptive_theme),
				style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 21.sp)
			)
			MainSwitchPreferenceCard(
				text = stringResource(id = R.string.action_use_adaptive_theme),
				isChecked = uiState.adaptiveThemeEnabled,
				onCheckedChange = { checked ->
					val hasPermission = ContextCompat.checkSelfPermission(
						context, Manifest.permission.WRITE_SECURE_SETTINGS
					) == PackageManager.PERMISSION_GRANTED

					adaptiveThemeViewModel.onServiceToggleRequested(
						checked,
						hasPermission,
						packageName
					).also { wasToggled ->
						if (wasToggled)
							haptic.performHapticFeedback(
								if (checked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
							)
						else
							haptic.performHapticFeedback(HapticFeedbackType.Reject)
					}

				}
			)

			val customLabel = stringResource(id = R.string.adaptive_threshold_custom)
			val labels = adaptiveThemeViewModel.getDisplayLabels(
				AdaptiveThreshold.entries.map { stringResource(id = it.labelRes) },
				customLabel
			)
			val baseLux = AdaptiveThreshold.entries.map { it.lux }
			val lux = adaptiveThemeViewModel.getDisplayLuxSteps(baseLux)
			val currentLux by adaptiveThemeViewModel.currentSensorLuxFlow.collectAsState(initial = adaptiveThemeViewModel.currentSensorLux)

			Column(
				verticalArrangement = Arrangement.spacedBy(2.dp)
			) {
				SliderDetailCard(
					title = stringResource(id = R.string.title_brightness_threshold),
					valueIndex = adaptiveThemeViewModel.getIndexForCurrentLux(),
					steps = labels.size,
					labels = labels,
					lux = lux,
					onValueChange = { index ->
						adaptiveThemeViewModel.setPendingCustomSliderLux(lux[index])
						adaptiveThemeViewModel.onSliderValueCommitted(index)
					},
					enabled = uiState.adaptiveThemeEnabled,
					firstCard = true,
					lastCard = false
				)

				ProgressDetailCard(
					title = stringResource(id = R.string.title_current_brightness),
					currentLux = currentLux,
					luxSteps = lux,
					enabled = uiState.adaptiveThemeEnabled,
					firstCard = false,
					lastCard = true
				)

			}
		}
	}


	PermissionMissingDialog(
		show = showMissingPermissionDialog,
		adbCommand = pendingAdbCommand,
		onCopy = { adaptiveThemeViewModel.requestCopyAdbCommand() },
		onDismiss = { adaptiveThemeViewModel.dismissDialog() }
	)

	CustomThresholdDialog(
		show = showCustomDialog.value,
		currentLux = uiState.customAdaptiveThemeThresholdLux ?: uiState.adaptiveThemeThresholdLux,
		onConfirm = { luxValue: Float ->
			adaptiveThemeViewModel.setCustomAdaptiveThemeThreshold(luxValue)
			showCustomDialog.value = false
		},
		onDismiss = { showCustomDialog.value = false }
	)
}