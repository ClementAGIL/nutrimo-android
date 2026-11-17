// Tabs shown before typing: recent foods, favorites, custom foods and recipes.
package com.clement.nutrimo.ui.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.R
import com.clement.nutrimo.data.db.FoodEntity
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.util.fmt

private enum class QuickTab(val label: String) { RECENT("Récents"), MEALS("Repas"), FAVORITES("Favoris"), CUSTOM("Mes aliments"), RECIPES("Recettes") }

@Composable
fun QuickTabs(vm: AddFoodViewModel, actions: AddFoodActions) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val recent by vm.recent.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val custom by vm.custom.collectAsStateWithLifecycle()
    val recipes by vm.allRecipes.collectAsStateWithLifecycle()
    // A whole saved meal makes no sense as one recipe ingredient.
    val tabs = QuickTab.entries.filter { !(vm.route.forRecipe && it == QuickTab.MEALS) }
    Column {
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp, containerColor = MaterialTheme.colorScheme.background) {
            tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t.label) }) }
        }
        when (tabs[tab.coerceIn(tabs.indices)]) {
            QuickTab.RECENT -> FoodList(recent, stringResource(R.string.quick_tabs_aliments_utilises_apparaitront_ici), actions)
            QuickTab.MEALS -> SavedMealList(vm, actions)
            QuickTab.FAVORITES -> FoodList(favorites, stringResource(R.string.quick_tabs_ajoutez_favoris_avec_etoile_aliment), actions)
            QuickTab.CUSTOM -> Column {
                OutlinedButton(onClick = { actions.onCreate(null) }, modifier = Modifier.padding(16.dp)) { Text(stringResource(R.string.quick_tabs_creer_aliment)) }
                FoodList(custom, stringResource(R.string.quick_tabs_aucun_aliment_personnel), actions)
            }
            QuickTab.RECIPES -> if (recipes.isEmpty()) EmptyState(stringResource(R.string.quick_tabs_aucune_recette)) else LazyColumn {
                items(recipes, key = { it.id }) { RecipeRow(it) { actions.onRecipe(it.id) } }
            }
        }
    }
}

@Composable
private fun FoodList(foods: List<FoodEntity>, empty: String, actions: AddFoodActions) {
    if (foods.isEmpty()) return EmptyState(empty)
    LazyColumn { items(foods, key = { it.id }) { FoodRow(it) { actions.onFood(it.id) } } }
}

@Composable
private fun SavedMealList(vm: AddFoodViewModel, actions: AddFoodActions) {
    val meals by vm.savedMeals.collectAsStateWithLifecycle()
    if (meals.isEmpty()) return EmptyState(stringResource(R.string.quick_tabs_aucun_repas_enregistre_ndans_journal))
    LazyColumn {
        items(meals, key = { it.id }) { meal ->
            val kcal by produceState<Double?>(null, meal.id) { value = vm.savedMealKcal(meal.id) }
            ListItem(
                headlineContent = { Text(meal.name) },
                supportingContent = { Text(kcal?.let { stringResource(R.string.quick_tabs_1_kcal_toucher_pour_ajouter, fmt(it, 0)) } ?: "") },
                trailingContent = { IconButton(onClick = { vm.deleteSavedMeal(meal.id) }) { Icon(Icons.Default.Delete, "Supprimer") } },
                modifier = Modifier.clickable { vm.logSavedMeal(meal.id, actions.onLogged) },
            )
            HorizontalDivider()
        }
    }
}
