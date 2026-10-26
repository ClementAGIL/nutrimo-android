// Tabs shown before typing: recent foods, favorites, custom foods and recipes.
package com.clement.nutrimo.ui.food

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.data.db.FoodEntity
import com.clement.nutrimo.ui.common.EmptyState

private val TABS = listOf("Récents", "Favoris", "Mes aliments", "Recettes")

@Composable
fun QuickTabs(vm: AddFoodViewModel, actions: AddFoodActions) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val recent by vm.recent.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val custom by vm.custom.collectAsStateWithLifecycle()
    val recipes by vm.allRecipes.collectAsStateWithLifecycle()
    Column {
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
            TABS.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        when (tab) {
            0 -> FoodList(recent, "Les aliments utilisés apparaîtront ici.", actions)
            1 -> FoodList(favorites, "Ajoutez des favoris avec l'étoile d'un aliment.", actions)
            2 -> Column {
                OutlinedButton(onClick = { actions.onCreate(null) }, modifier = Modifier.padding(16.dp)) { Text("Créer un aliment") }
                FoodList(custom, "Aucun aliment personnel.", actions)
            }
            else -> if (recipes.isEmpty()) EmptyState("Aucune recette.") else LazyColumn {
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
