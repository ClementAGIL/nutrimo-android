// Food search: instant local results, then the PC backend (or Open Food Facts on submit), plus barcode lookup.
package com.clement.nutrimo.ui.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.db.FoodEntity
import com.clement.nutrimo.data.db.RecipeEntity
import com.clement.nutrimo.data.network.FoodDto
import com.clement.nutrimo.data.repository.RemoteSource
import com.clement.nutrimo.ui.navigation.AddFoodRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOCAL_DEBOUNCE_MS = 200L
private const val REMOTE_DEBOUNCE_MS = 600L
private const val MIN_REMOTE_CHARS = 3

data class SearchUiState(
    val query: String = "",
    val local: List<FoodEntity> = emptyList(),
    val recipes: List<RecipeEntity> = emptyList(),
    val remote: List<FoodDto> = emptyList(),
    val remoteSource: RemoteSource? = null,
    val remoteError: String? = null,
    val loading: Boolean = false,
    val message: String? = null,
)

class AddFoodViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val route = handle.toRoute<AddFoodRoute>()
    val state = MutableStateFlow(SearchUiState())
    val recent = c.foods.recent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favorites = c.foods.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val custom = c.foods.custom.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allRecipes = c.recipes.recipes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var searchJob: Job? = null

    fun onQuery(q: String) {
        state.update { it.copy(query = q, message = null) }
        searchJob?.cancel()
        if (q.isBlank()) return state.update { SearchUiState() }
        searchJob = viewModelScope.launch {
            delay(LOCAL_DEBOUNCE_MS)
            searchLocal(q)
            if (q.trim().length < MIN_REMOTE_CHARS) return@launch
            delay(REMOTE_DEBOUNCE_MS)
            searchRemote(q, allowOff = false)
        }
    }

    /** Explicit search (keyboard action): may also query Open Food Facts when the PC is unreachable. */
    fun onSubmit() {
        val q = state.value.query
        if (q.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch { searchLocal(q); searchRemote(q, allowOff = true) }
    }

    private suspend fun searchLocal(q: String) {
        val foods = c.foods.searchLocal(q)
        val recipes = c.recipes.search(q)
        state.update { it.copy(local = foods, recipes = recipes) }
    }

    private suspend fun searchRemote(q: String, allowOff: Boolean) {
        state.update { it.copy(loading = true) }
        val result = c.foods.searchRemote(q, allowOff)
        val localIds = state.value.local.map { it.id }.toSet()
        state.update {
            it.copy(
                remote = result.items.filter { dto -> dto.id !in localIds },
                remoteSource = result.source,
                remoteError = result.error,
                loading = false,
            )
        }
    }

    /** Saves the remote food in the local cache, then opens it. */
    fun pickRemote(dto: FoodDto, open: (String) -> Unit) = viewModelScope.launch { open(c.foods.cache(dto).id) }

    fun lookupBarcode(code: String, found: (String) -> Unit, notFound: (String) -> Unit) = viewModelScope.launch {
        state.update { it.copy(loading = true, message = "Recherche du code $code…") }
        val food = c.foods.findByBarcode(code)
        state.update { it.copy(loading = false, message = null) }
        if (food != null) found(food.id) else notFound(code)
    }
}
