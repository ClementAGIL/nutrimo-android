// Loads photos A and B of the comparison with the weight of their day.
package com.clement.nutrimo.ui.photo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.db.PhotoEntity
import com.clement.nutrimo.data.db.points
import com.clement.nutrimo.domain.calc.kgAt
import com.clement.nutrimo.ui.navigation.PhotoCompareRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** [kg] is the weigh-in of the photo's day, or read on the weight curve. */
data class CompareSide(val photo: PhotoEntity, val kg: Double?)

data class CompareState(val a: CompareSide? = null, val b: CompareSide? = null)

class PhotoCompareViewModel(c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val state = MutableStateFlow(CompareState())

    init {
        val route = handle.toRoute<PhotoCompareRoute>()
        viewModelScope.launch {
            val weights = c.body.weightHistory.first().points()
            val side = { photo: PhotoEntity? -> photo?.let { CompareSide(it, weights.kgAt(it.date)) } }
            state.value = CompareState(side(c.photos.byId(route.aId)), side(c.photos.byId(route.bId)))
        }
    }
}
