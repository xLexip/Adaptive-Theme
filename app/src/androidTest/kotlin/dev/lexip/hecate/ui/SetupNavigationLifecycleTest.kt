/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 * Licensed under the GNU General Public License, Version 3.0.
 */
package dev.lexip.hecate.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.createGraph
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.lexip.hecate.ui.navigation.MainRoute
import dev.lexip.hecate.ui.navigation.NavigationManager
import dev.lexip.hecate.ui.navigation.SetupGraph
import dev.lexip.hecate.ui.navigation.SetupRoute
import dev.lexip.hecate.ui.setup.SetupViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupNavigationLifecycleTest {
	@get:Rule
	val composeRule = createAndroidComposeRule<ComponentActivity>()
	private val store = ViewModelStore()

	@After
	fun clearViewModels() {
		composeRule.runOnIdle { store.clear() }
	}

	@Test
	fun outgoingSetupRecompositionDoesNotReadDestroyedGraphAndReentryGetsNewViewModel() {
		lateinit var navController: NavHostController
		lateinit var mainEntry: NavBackStackEntry
		lateinit var setupDestination: NavBackStackEntry
		val navigator = ComposeNavigator()
		val entry = mutableStateOf<NavBackStackEntry?>(null)
		val revision = mutableIntStateOf(0)
		val navigationManager = NavigationManager()
		var renderedRevision = -1
		var renderedViewModel: SetupViewModel? = null
		composeRule.runOnIdle {
			navController = NavHostController(composeRule.activity).apply {
				setLifecycleOwner(composeRule.activity)
				setViewModelStore(store)
				navigatorProvider.addNavigator(navigator)
				graph = createGraph(startDestination = MainRoute) {
					composable<MainRoute> {}
					navigation<SetupGraph>(startDestination = SetupRoute.DeveloperMode) {
						composable<SetupRoute.DeveloperMode> {}
					}
				}
				mainEntry = currentBackStackEntry!!
				navigate(SetupGraph)
				setupDestination = currentBackStackEntry!!
			}
			navigator.onTransitionComplete(mainEntry)
			navigator.onTransitionComplete(setupDestination)
			entry.value = navController.getBackStackEntry(SetupGraph)
		}
		composeRule.setContent {
			val currentRevision = revision.intValue
			val viewModel = rememberSetupViewModel(
				entry.value!!, composeRule.activity, navigationManager
			)
			SideEffect {
				renderedRevision = currentRevision
				renderedViewModel = viewModel
			}
		}
		composeRule.waitForIdle()
		val originalViewModel = renderedViewModel

		// Keep the outgoing composition alive after the graph's lifecycle ends,
		// then trigger the late recomposition seen in Crashlytics.
		composeRule.runOnIdle {
			navController.popBackStack(MainRoute, inclusive = false)
			navigator.onTransitionComplete(setupDestination)
			navigator.onTransitionComplete(mainEntry)
			assertEquals(Lifecycle.State.DESTROYED, entry.value!!.lifecycle.currentState)
			revision.intValue++
		}
		composeRule.waitForIdle()
		assertEquals(1, renderedRevision)
		assertSame(originalViewModel, renderedViewModel)

		composeRule.runOnIdle {
			navController.navigate(SetupGraph)
			navigator.onTransitionComplete(mainEntry)
			navigator.onTransitionComplete(navController.currentBackStackEntry!!)
			entry.value = navController.getBackStackEntry(SetupGraph)
		}
		composeRule.waitForIdle()
		assertNotSame(originalViewModel, renderedViewModel)
	}
}
