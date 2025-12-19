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

package dev.lexip.hecate.ui.setup.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.ui.components.SegmentedProgressIndicator

/**
 * Common scaffold for setup screens with progress indicator.
 */
@Composable
fun SetupScreenScaffold(
	currentStepIndex: Int,
	totalSteps: Int,
	content: @Composable () -> Unit
) {
	Scaffold(
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
	) { paddingValues ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
		) {
			// Progress indicator section
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 24.dp)
					.padding(top = 16.dp)
					.padding(bottom = 36.dp)
			) {
				SegmentedProgressIndicator(
					segments = totalSteps,
					activeIndex = currentStepIndex,
					enabled = true
				)
			}

			// Main content
			Column(
				modifier = Modifier
					.weight(1f)
					.fillMaxWidth()
					.padding(horizontal = 24.dp)
			) {
				content()
			}
			Spacer(modifier = Modifier.height(8.dp))
		}
	}
}

