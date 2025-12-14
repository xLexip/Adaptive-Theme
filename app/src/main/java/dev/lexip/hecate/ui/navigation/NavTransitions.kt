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

package dev.lexip.hecate.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

/**
 * Default animation durations used across navigation transitions.
 */
object NavAnimations {
	const val DEFAULT_DURATION_MS = 300
}

object NavTransitions {

	/**
	 * Default fade-in enter transition for screens without directional animation.
	 */
	fun fadeEnter(durationMs: Int = NavAnimations.DEFAULT_DURATION_MS): EnterTransition =
		fadeIn(animationSpec = tween(durationMs))

	/**
	 * Default fade-out exit transition for screens without directional animation.
	 */
	fun fadeExit(durationMs: Int = NavAnimations.DEFAULT_DURATION_MS): ExitTransition =
		fadeOut(animationSpec = tween(durationMs))

	/**
	 * Horizontal slide-in from the end (right on LTR) - used for forward navigation.
	 */
	fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromEnd(
		durationMs: Int = NavAnimations.DEFAULT_DURATION_MS
	): EnterTransition = slideIntoContainer(
		towards = AnimatedContentTransitionScope.SlideDirection.Start,
		animationSpec = tween(durationMs)
	)

	/**
	 * Horizontal slide-out to the end (right on LTR) - used for back navigation.
	 */
	fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToEnd(
		durationMs: Int = NavAnimations.DEFAULT_DURATION_MS
	): ExitTransition = slideOutOfContainer(
		towards = AnimatedContentTransitionScope.SlideDirection.End,
		animationSpec = tween(durationMs)
	)

	/**
	 * Horizontal slide-out to the start (left on LTR) - used for forward navigation exit.
	 */
	fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToStart(
		durationMs: Int = NavAnimations.DEFAULT_DURATION_MS
	): ExitTransition = slideOutOfContainer(
		towards = AnimatedContentTransitionScope.SlideDirection.Start,
		animationSpec = tween(durationMs)
	)

	/**
	 * Horizontal slide-in from the start (left on LTR) - used for pop back navigation.
	 */
	fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromStart(
		durationMs: Int = NavAnimations.DEFAULT_DURATION_MS
	): EnterTransition = slideIntoContainer(
		towards = AnimatedContentTransitionScope.SlideDirection.End,
		animationSpec = tween(durationMs)
	)
}

