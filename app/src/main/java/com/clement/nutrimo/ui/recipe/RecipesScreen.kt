// Recipes tab: saved recipes (favorites first, then the most logged) with kcal per part and use count; "+" creates one.
package com.clement.nutrimo.ui.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.db.RecipeEntity
import com.clement.nutrimo.data.repository.formatParts
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.common.kcal
import com.clement.nutrimo.ui.common.stateWhileSubscribed
import kotlinx.coroutines.flow.map

data class RecipeSummary(val recipe: RecipeEntity, val kcalPerPart: Double?, val useCount: Int)

class RecipesViewModel(c: AppContainer) : ViewModel() {
    val recipes = c.recipes.recipesWithUses
        .map { list -> list.map { RecipeSummary(it.recipe, c.recipes.totals(it.recipe.id)?.perPart?.kcal, it.useCount) } }
        .stateWhileSubscribed(viewModelScope, emptyList())
}

@Composable
fun RecipesScreen(onOpen: (String) -> Unit, onCreate: () -> Unit) {
    val vm = appViewModel { c, _ -> RecipesViewModel(c) }
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    ScreenScaffold("Recettes", floatingActionButton = {
        FloatingActionButton(onClick = onCreate) { Icon(Icons.Default.Add, "Nouvelle recette") }
    }) { padding ->
        if (recipes.isEmpty()) return@ScreenScaffold EmptyState(
            "Aucune recette.\nCréez-en une avec + : ingrédients en grammes et nombre de parts.",
            Modifier.padding(padding),
        )
        LazyColumn(Modifier.padding(padding)) {
            items(recipes, key = { it.recipe.id }) { (recipe, perPart, uses) ->
                ListItem(
                    headlineContent = { Text(recipe.name) },
                    supportingContent = {
                        Text("${formatParts(recipe.parts)} parts · ${perPart?.let { kcal(it) } ?: "–"} / part · ${usesText(uses)}")
                    },
                    trailingContent = if (recipe.isFavorite) ({ Icon(Icons.Default.Star, "Favori", tint = MaterialTheme.colorScheme.primary) }) else null,
                    modifier = Modifier.clickable { onOpen(recipe.id) },
                )
                HorizontalDivider()
            }
        }
    }
}

private fun usesText(n: Int) = when (n) {
    0 -> "jamais utilisée"
    1 -> "utilisée 1 fois"
    else -> "utilisée $n fois"
}
