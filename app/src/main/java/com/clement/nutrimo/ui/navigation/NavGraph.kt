// The navigation graph: every destination and how screens navigate to each other.
package com.clement.nutrimo.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.clement.nutrimo.ui.food.AddFoodActions
import com.clement.nutrimo.ui.food.AddFoodScreen
import com.clement.nutrimo.ui.food.CustomFoodScreen
import com.clement.nutrimo.ui.food.ItemDetailActions
import com.clement.nutrimo.ui.food.ItemDetailScreen
import com.clement.nutrimo.ui.food.ScannerScreen
import com.clement.nutrimo.ui.goal.GoalEditScreen
import com.clement.nutrimo.ui.goal.GoalsScreen
import com.clement.nutrimo.ui.journal.DayNutrientsScreen
import com.clement.nutrimo.ui.journal.JournalActions
import com.clement.nutrimo.ui.journal.JournalScreen
import com.clement.nutrimo.ui.more.MoreScreen
import com.clement.nutrimo.ui.profile.ProfileScreen
import com.clement.nutrimo.ui.weight.WeightScreen

fun NavGraphBuilder.nutrimoGraph(nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }
    composable<JournalRoute> {
        JournalScreen(
            JournalActions(
                onAdd = { date, meal -> nav.navigate(AddFoodRoute(date.toEpochDay(), meal.name)) },
                onEdit = { nav.navigate(ItemDetailRoute(entryId = it.id)) },
                onDetails = { nav.navigate(DayNutrientsRoute(it.toEpochDay())) },
            ),
        )
    }
    composable<WeightRoute> { WeightScreen(onNewGoal = { nav.navigate(GoalEditRoute) }, onGoals = { nav.navigate(GoalsRoute) }) }
    composable<RecipesRoute> { PlaceholderScreen("Recettes") }
    composable<MoreRoute> { MoreScreen(onOpen = { nav.navigate(it) }) }
    foodDestinations(nav, back)
    composable<DayNutrientsRoute> { DayNutrientsScreen(it.toRoute<DayNutrientsRoute>().epochDay, back) }
    composable<ProfileRoute> { ProfileScreen(onBack = back) }
    composable<GoalEditRoute> { GoalEditScreen(onBack = back) }
    composable<GoalsRoute> { GoalsScreen(onBack = back) }
}

/** Search → (scanner) → quantity screen, and custom foods. */
private fun NavGraphBuilder.foodDestinations(nav: NavHostController, back: () -> Unit) {
    composable<AddFoodRoute> { entry ->
        val route = entry.toRoute<AddFoodRoute>()
        val scanned by entry.savedStateHandle.getStateFlow<String?>(NavResults.BARCODE, null).collectAsStateWithLifecycle()
        val detail = { foodId: String?, recipeId: String? ->
            nav.navigate(ItemDetailRoute(foodId, recipeId, route.epochDay, route.meal, forRecipe = route.forRecipe))
        }
        AddFoodScreen(
            AddFoodActions(
                onBack = back,
                onFood = { detail(it, null) },
                onRecipe = { detail(null, it) },
                onScan = { nav.navigate(ScannerRoute) },
                onCreate = { nav.navigate(CustomFoodRoute(barcode = it)) },
            ),
            scannedBarcode = scanned,
            onBarcodeConsumed = { entry.savedStateHandle[NavResults.BARCODE] = null },
        )
    }
    composable<ItemDetailRoute> {
        ItemDetailScreen(
            ItemDetailActions(
                onBack = back,
                onDone = { nav.popBackStack<JournalRoute>(inclusive = false) },
                onPicked = { result ->
                    nav.getBackStackEntry<RecipeEditRoute>().savedStateHandle[NavResults.INGREDIENT] = result
                    nav.popBackStack<RecipeEditRoute>(inclusive = false)
                },
                onEditCustom = { nav.navigate(CustomFoodRoute(foodId = it)) },
            ),
        )
    }
    composable<ScannerRoute> {
        ScannerScreen(onBack = back, onResult = { code ->
            nav.previousBackStackEntry?.savedStateHandle?.set(NavResults.BARCODE, code)
            nav.popBackStack()
        })
    }
    composable<CustomFoodRoute> { CustomFoodScreen(onBack = back) }
}
