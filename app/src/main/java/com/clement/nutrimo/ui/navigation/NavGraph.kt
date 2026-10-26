// The navigation graph: every destination and how screens navigate to each other.
package com.clement.nutrimo.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.clement.nutrimo.ui.goal.GoalEditScreen
import com.clement.nutrimo.ui.more.MoreScreen
import com.clement.nutrimo.ui.profile.ProfileScreen

fun NavGraphBuilder.nutrimoGraph(nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }
    composable<JournalRoute> { PlaceholderScreen("Journal") }
    composable<WeightRoute> { PlaceholderScreen("Poids") }
    composable<RecipesRoute> { PlaceholderScreen("Recettes") }
    composable<MoreRoute> { MoreScreen(onOpen = { nav.navigate(it) }) }
    composable<ProfileRoute> { ProfileScreen(onBack = back) }
    composable<GoalEditRoute> { GoalEditScreen(onBack = back) }
}
