// The navigation graph: every destination and how screens navigate to each other.
package com.clement.nutrimo.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.clement.nutrimo.ui.goal.GoalEditScreen
import com.clement.nutrimo.ui.goal.GoalsScreen
import com.clement.nutrimo.ui.more.MoreScreen
import com.clement.nutrimo.ui.profile.ProfileScreen
import com.clement.nutrimo.ui.weight.WeightScreen

fun NavGraphBuilder.nutrimoGraph(nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }
    composable<JournalRoute> { PlaceholderScreen("Journal") }
    composable<WeightRoute> { WeightScreen(onNewGoal = { nav.navigate(GoalEditRoute) }, onGoals = { nav.navigate(GoalsRoute) }) }
    composable<RecipesRoute> { PlaceholderScreen("Recettes") }
    composable<MoreRoute> { MoreScreen(onOpen = { nav.navigate(it) }) }
    composable<ProfileRoute> { ProfileScreen(onBack = back) }
    composable<GoalEditRoute> { GoalEditScreen(onBack = back) }
    composable<GoalsRoute> { GoalsScreen(onBack = back) }
}
