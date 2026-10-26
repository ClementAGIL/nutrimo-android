// The navigation graph: every destination and how screens navigate to each other.
package com.clement.nutrimo.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.clement.nutrimo.domain.model.Meal
import com.clement.nutrimo.ui.food.AddFoodActions
import com.clement.nutrimo.ui.food.AddFoodScreen
import com.clement.nutrimo.ui.food.CustomFoodScreen
import com.clement.nutrimo.ui.food.ItemDetailActions
import com.clement.nutrimo.ui.food.ItemDetailScreen
import com.clement.nutrimo.ui.food.ScannerScreen
import com.clement.nutrimo.ui.goal.GoalEditScreen
import com.clement.nutrimo.ui.goal.GoalsScreen
import com.clement.nutrimo.ui.journal.CalorieHistoryScreen
import com.clement.nutrimo.ui.journal.DayNutrientsScreen
import com.clement.nutrimo.ui.journal.JournalActions
import com.clement.nutrimo.ui.journal.JournalScreen
import com.clement.nutrimo.ui.more.AboutScreen
import com.clement.nutrimo.ui.more.BackupScreen
import com.clement.nutrimo.ui.more.DevScreen
import com.clement.nutrimo.ui.more.MacrosScreen
import com.clement.nutrimo.ui.more.MeasurementsScreen
import com.clement.nutrimo.ui.more.MoreScreen
import com.clement.nutrimo.ui.more.RemindersScreen
import com.clement.nutrimo.ui.more.ServerScreen
import com.clement.nutrimo.ui.more.StepsScreen
import com.clement.nutrimo.ui.profile.ProfileScreen
import com.clement.nutrimo.ui.recipe.RecipeEditScreen
import com.clement.nutrimo.ui.recipe.RecipeViewScreen
import com.clement.nutrimo.ui.recipe.RecipesScreen
import com.clement.nutrimo.ui.weight.WeightScreen
import java.time.LocalDate
import java.time.LocalTime

fun NavGraphBuilder.nutrimoGraph(nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }
    composable<JournalRoute> {
        JournalScreen(
            JournalActions(
                onAdd = { date, meal -> nav.navigate(AddFoodRoute(date.toEpochDay(), meal.name)) },
                onEdit = { nav.navigate(ItemDetailRoute(entryId = it.id)) },
                onDetails = { nav.navigate(DayNutrientsRoute(it.toEpochDay())) },
                onHistory = { nav.navigate(CalorieHistoryRoute) },
            ),
        )
    }
    composable<WeightRoute> { WeightScreen(onNewGoal = { nav.navigate(GoalEditRoute) }, onGoals = { nav.navigate(GoalsRoute) }) }
    composable<RecipesRoute> { RecipesScreen(onOpen = { nav.navigate(RecipeViewRoute(it)) }, onCreate = { nav.navigate(RecipeEditRoute()) }) }
    recipeDestinations(nav, back)
    composable<MoreRoute> { MoreScreen(onOpen = { nav.navigate(it) }) }
    foodDestinations(nav, back)
    composable<CalorieHistoryRoute> { CalorieHistoryScreen(onBack = back) }
    composable<DayNutrientsRoute> { DayNutrientsScreen(it.toRoute<DayNutrientsRoute>().epochDay, back) }
    composable<ProfileRoute> { ProfileScreen(onBack = back) }
    composable<GoalEditRoute> { GoalEditScreen(onBack = back) }
    composable<GoalsRoute> { GoalsScreen(onBack = back) }
    composable<MeasurementsRoute> { MeasurementsScreen(onBack = back) }
    composable<RemindersRoute> { RemindersScreen(onBack = back) }
    composable<ServerRoute> { ServerScreen(onBack = back) }
    composable<BackupRoute> { BackupScreen(onBack = back) }
    composable<AboutRoute> { AboutScreen(onBack = back) }
    composable<DevRoute> { DevScreen(onBack = back) }
    composable<MacrosRoute> { MacrosScreen(onBack = back) }
    composable<StepsRoute> { StepsScreen(onBack = back) }
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
                onLogged = { nav.popBackStack<JournalRoute>(inclusive = false) },
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

/** Recipe detail and editor; the editor receives picked ingredients through NavResults.INGREDIENT. */
private fun NavGraphBuilder.recipeDestinations(nav: NavHostController, back: () -> Unit) {
    composable<RecipeViewRoute> {
        RecipeViewScreen(
            onBack = back,
            onEdit = { nav.navigate(RecipeEditRoute(it)) },
            onLog = { id ->
                val meal = Meal.atHour(LocalTime.now().hour).name
                nav.navigate(ItemDetailRoute(recipeId = id, epochDay = LocalDate.now().toEpochDay(), meal = meal))
            },
        )
    }
    composable<RecipeEditRoute> { entry ->
        val picked by entry.savedStateHandle.getStateFlow<String?>(NavResults.INGREDIENT, null).collectAsStateWithLifecycle()
        RecipeEditScreen(
            onBack = back,
            onAddIngredient = { nav.navigate(AddFoodRoute(epochDay = 0, meal = Meal.LUNCH.name, forRecipe = true)) },
            picked = picked,
            onPickedConsumed = { entry.savedStateHandle[NavResults.INGREDIENT] = null },
        )
    }
}
