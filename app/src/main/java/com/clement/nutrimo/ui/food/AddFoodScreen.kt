// Food search screen: search field with barcode button, quick tabs when empty, merged results otherwise.
package com.clement.nutrimo.ui.food

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.domain.model.Meal
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.WarningText
import com.clement.nutrimo.ui.common.appViewModel

/** Where the search screen can go next. */
data class AddFoodActions(
    val onBack: () -> Unit,
    val onFood: (String) -> Unit,
    val onRecipe: (String) -> Unit,
    val onScan: () -> Unit,
    val onCreate: (barcode: String?) -> Unit,
    /** After a saved meal was added to the journal. */
    val onLogged: () -> Unit,
)

@Composable
fun AddFoodScreen(actions: AddFoodActions, scannedBarcode: String?, onBarcodeConsumed: () -> Unit) {
    val vm = appViewModel { c, handle -> AddFoodViewModel(c, handle) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(scannedBarcode) {
        val code = scannedBarcode ?: return@LaunchedEffect
        onBarcodeConsumed()
        vm.lookupBarcode(code, actions.onFood) { actions.onCreate(it) }
    }
    val title = if (vm.route.forRecipe) "Ajouter un ingrédient" else Meal.valueOf(vm.route.meal).label
    ScreenScaffold(title, onBack = actions.onBack) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQuery,
                placeholder = { Text("Rechercher un aliment") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { IconButton(onClick = actions.onScan) { Icon(Icons.Default.QrCodeScanner, "Scanner") } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.onSubmit() }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.message?.let { Text(it, Modifier.padding(horizontal = 16.dp)) }
            if (state.query.isBlank()) QuickTabs(vm, actions) else SearchResults(state, vm, actions)
        }
    }
}

@Composable
private fun SearchResults(state: SearchUiState, vm: AddFoodViewModel, actions: AddFoodActions) {
    LazyColumn {
        if (state.local.isNotEmpty() || state.recipes.isNotEmpty()) item { SectionHeader("Sur le téléphone") }
        items(state.recipes, key = { "r" + it.id }) { RecipeRow(it) { actions.onRecipe(it.id) } }
        items(state.local, key = { it.id }) { FoodRow(it) { actions.onFood(it.id) } }
        remoteSection(state, vm, actions)
        item {
            TextButton(onClick = { actions.onCreate(null) }, modifier = Modifier.padding(8.dp)) {
                Text("Introuvable ? Créer un aliment")
            }
        }
    }
}

private fun LazyListScope.remoteSection(state: SearchUiState, vm: AddFoodViewModel, actions: AddFoodActions) {
    state.remoteSource?.let { item { SectionHeader(it.label) } }
    items(state.remote, key = { "remote" + it.id }) { dto -> RemoteFoodRow(dto) { vm.pickRemote(dto, actions.onFood) } }
    state.remoteError?.let { item { Column(Modifier.padding(16.dp)) { WarningText(it) } } }
}
