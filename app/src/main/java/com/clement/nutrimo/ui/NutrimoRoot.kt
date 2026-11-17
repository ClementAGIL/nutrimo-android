// Root composable: waits for settings, shows onboarding or the main graph, with the bottom bar on the 4 main tabs.
package com.clement.nutrimo.ui

import androidx.compose.ui.res.stringResource
import com.clement.nutrimo.R
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.common.stateWhileSubscribed
import com.clement.nutrimo.ui.navigation.JournalRoute
import com.clement.nutrimo.ui.navigation.MoreRoute
import com.clement.nutrimo.ui.navigation.RecipesRoute
import com.clement.nutrimo.ui.navigation.WeightRoute
import com.clement.nutrimo.ui.navigation.nutrimoGraph
import com.clement.nutrimo.ui.onboarding.OnboardingScreen
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

private data class Tab(val route: Any, val type: KClass<*>, @StringRes val label: Int, val icon: ImageVector)

private val TABS = listOf(
    Tab(JournalRoute, JournalRoute::class, R.string.tab_journal, Icons.Default.Restaurant),
    Tab(WeightRoute, WeightRoute::class, R.string.tab_weight, Icons.Default.MonitorWeight),
    Tab(RecipesRoute, RecipesRoute::class, R.string.tab_recipes, Icons.AutoMirrored.Filled.MenuBook),
    Tab(MoreRoute, MoreRoute::class, R.string.tab_more, Icons.Default.MoreHoriz),
)

class RootViewModel(c: AppContainer) : ViewModel() {
    /** Null until the settings are read: nothing is shown before, so the onboarding never flashes. */
    val onboardingDone = c.settings.settings.map { it.onboardingDone }.stateWhileSubscribed(viewModelScope, null)
}

@Composable
fun NutrimoRoot() {
    val vm = appViewModel { c, _ -> RootViewModel(c) }
    val onboardingDone by vm.onboardingDone.collectAsStateWithLifecycle()
    val done = onboardingDone ?: return
    // Onboarding lives outside the graph: once it saves onboardingDone, this recomposes into the main app.
    if (!done) return OnboardingScreen(onDone = {})
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val showBar = TABS.any { tab -> entry?.destination?.hasRoute(tab.type) == true }
    // The inner screens have their own Scaffold: only the bottom bar's height is passed down (and consumed).
    Scaffold(contentWindowInsets = WindowInsets(0), bottomBar = { if (showBar) BottomBar(nav, entry?.destination) }) { p ->
        val bottom = PaddingValues(bottom = p.calculateBottomPadding())
        NavHost(nav, startDestination = JournalRoute, modifier = Modifier.padding(bottom).consumeWindowInsets(bottom)) {
            nutrimoGraph(nav)
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, destination: androidx.navigation.NavDestination?) {
    NavigationBar {
        TABS.forEach { tab ->
            NavigationBarItem(
                selected = destination?.hasRoute(tab.type) == true,
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}
