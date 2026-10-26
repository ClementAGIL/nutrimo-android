// Recipes tab: saved recipes with kcal per part; "+" creates one.
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class RecipeSummary(val recipe: RecipeEntity, val kcalPerPart: Double?)

class RecipesViewModel(c: AppContainer) : ViewModel() {
    val recipes = c.recipes.recipes
        .map { list -> list.map { RecipeSummary(it, c.recipes.totals(it.id)?.perPart?.kcal) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
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
            items(recipes, key = { it.recipe.id }) { (recipe, perPart) ->
                ListItem(
                    headlineContent = { Text(recipe.name) },
                    supportingContent = { Text("${formatParts(recipe.parts)} parts · ${perPart?.let { kcal(it) } ?: "–"} / part") },
                    trailingContent = if (recipe.isFavorite) ({ Icon(Icons.Default.Star, "Favori", tint = MaterialTheme.colorScheme.primary) }) else null,
                    modifier = Modifier.clickable { onOpen(recipe.id) },
                )
                HorizontalDivider()
            }
        }
    }
}
