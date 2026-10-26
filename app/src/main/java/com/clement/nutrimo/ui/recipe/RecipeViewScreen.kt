// Recipe detail: per part and per 100 g nutrition, ingredients; edit, delete, add to the journal.
package com.clement.nutrimo.ui.recipe

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.repository.RecipeDetail
import com.clement.nutrimo.data.repository.formatParts
import com.clement.nutrimo.ui.common.ConfirmDialog
import com.clement.nutrimo.ui.common.NutrientTable
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.kcal
import com.clement.nutrimo.ui.navigation.RecipeViewRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class RecipeViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val id = handle.toRoute<RecipeViewRoute>().recipeId
    val detail = MutableStateFlow<RecipeDetail?>(null)
    /** Recipes that use this one as a sub-recipe. */
    val parents = MutableStateFlow<List<String>>(emptyList())

    /** Reloaded when coming back from the editor. */
    fun load() = viewModelScope.launch {
        detail.value = c.recipes.detail(id)
        parents.value = c.recipes.parentsOf(id)
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch { c.recipes.delete(id); onDone() }
}

@Composable
fun RecipeViewScreen(onBack: () -> Unit, onEdit: (String) -> Unit, onLog: (String) -> Unit) {
    val vm = appViewModel { c, handle -> RecipeViewModel(c, handle) }
    val detail by vm.detail.collectAsStateWithLifecycle()
    val parents by vm.parents.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) { vm.load(); onPauseOrDispose {} }
    val d = detail ?: return
    ScreenScaffold(d.recipe.name, onBack = onBack, actions = {
        IconButton(onClick = { onEdit(vm.id) }) { Icon(Icons.Default.Edit, "Modifier") }
        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Supprimer") }
    }) { padding ->
        ScrollColumn(padding) {
            Button(onClick = { onLog(vm.id) }, modifier = Modifier.fillMaxWidth()) { Text("Ajouter au journal") }
            SectionCard("${formatParts(d.recipe.parts)} parts · ${fmt(d.totals.weightPerPart, 0)} g par part") {
                d.lines.forEach { StatRow(it.name, ingredientQuantity(it)) }
            }
            NutrientTable(d.totals.perPart, "Par part")
            d.totals.per100?.let { SectionCard { StatRow("Pour 100 g", kcal(it.kcal)) } }
        }
    }
    if (confirmDelete) {
        val warning = if (parents.isEmpty()) "" else "Elle est utilisée dans : ${parents.joinToString()}. "
        ConfirmDialog("Supprimer la recette ?", "${warning}Les jours déjà enregistrés ne changent pas.", "Supprimer",
            { vm.delete(onBack) }, { confirmDelete = false })
    }
}
