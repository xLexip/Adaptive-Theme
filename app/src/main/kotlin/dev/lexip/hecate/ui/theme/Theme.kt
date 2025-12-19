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

package dev.lexip.hecate.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.lexip.hecate.R

@Composable
fun HecateTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	context: Context = LocalContext.current,
	content: @Composable () -> Unit
) {
	val colorScheme = when {
		darkTheme -> dynamicDarkColorScheme(context)
		else -> dynamicLightColorScheme(context)
	}

	// Bundle Nunito Font to match system settings design
	val nunitoFontFamily = FontFamily(
		Font(R.font.nunito_bold, weight = FontWeight.Bold),
		Font(R.font.nunito_semibold, weight = FontWeight.SemiBold),
		Font(R.font.nunito_regular, weight = FontWeight.Normal),
	)

	val appTypography = Typography(
		displaySmall = TextStyle(
			fontFamily = nunitoFontFamily,
			fontWeight = FontWeight.Bold,
			fontSize = 36.sp
		),
		titleMedium = TextStyle(
			fontFamily = nunitoFontFamily,
			fontWeight = FontWeight.Bold,
			fontSize = 16.sp
		),
		bodyLarge = TextStyle(
			fontFamily = nunitoFontFamily,
			fontWeight = FontWeight.Normal,
			fontSize = 17.sp
		),
		bodySmall = TextStyle(
			fontFamily = nunitoFontFamily,
			fontWeight = FontWeight.SemiBold,
			fontSize = 12.sp
		)
	)

	MaterialTheme(
		colorScheme = colorScheme,
		typography = appTypography,
		content = content
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun hecateTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
	// This represents the top app bar style of the stock/pixel android system settings.
	containerColor = MaterialTheme.colorScheme.surfaceContainer,
	scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
	navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
	titleContentColor = MaterialTheme.colorScheme.onSurface,
	actionIconContentColor = MaterialTheme.colorScheme.onSurface
)