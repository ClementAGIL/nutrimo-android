// Type-safe navigation routes. Enum values travel as their name (String) to keep NavTypes simple.
package com.clement.nutrimo.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object JournalRoute
@Serializable object WeightRoute
@Serializable object RecipesRoute
@Serializable object MoreRoute

/** Food search. With [forRecipe] the picked item goes back to the recipe editor instead of the diary. */
@Serializable data class AddFoodRoute(val epochDay: Long, val meal: String, val forRecipe: Boolean = false)

/** Quantity screen for a food or a recipe; [entryId] edits an existing diary entry. */
@Serializable data class ItemDetailRoute(
    val foodId: String? = null,
    val recipeId: String? = null,
    val epochDay: Long = 0,
    val meal: String = "",
    val entryId: String? = null,
    val forRecipe: Boolean = false,
)

@Serializable object ScannerRoute
@Serializable data class CustomFoodRoute(val foodId: String? = null, val barcode: String? = null)
@Serializable data class DayNutrientsRoute(val epochDay: Long)
@Serializable object HistoryRoute
@Serializable object GoalEditRoute
@Serializable object GoalsRoute
@Serializable data class RecipeEditRoute(val recipeId: String? = null)
@Serializable data class RecipeViewRoute(val recipeId: String)
@Serializable object ProfileRoute
@Serializable object RemindersRoute
@Serializable object ServerRoute
@Serializable object BackupRoute
@Serializable object AboutRoute
@Serializable object DevRoute
@Serializable object IntroRoute
@Serializable object MacrosRoute
@Serializable object StepsRoute

/** savedStateHandle keys used to return results to a previous screen. */
object NavResults {
    const val BARCODE = "barcode"

    /** "food|<id>|<grams>" or "recipe|<id>|<parts>", read by the recipe editor. */
    const val INGREDIENT = "ingredient"
}
