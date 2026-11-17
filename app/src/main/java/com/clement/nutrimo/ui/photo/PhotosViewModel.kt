// State of the photo timeline: photo days placed on the weight curve, pose filter, selected day, photos A and B.
package com.clement.nutrimo.ui.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.db.PhotoEntity
import com.clement.nutrimo.data.db.WeightEntity
import com.clement.nutrimo.data.db.points
import com.clement.nutrimo.domain.calc.kgAt
import com.clement.nutrimo.domain.model.PhotoPose
import com.clement.nutrimo.ui.common.stateWhileSubscribed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The photos of one day; [kg] is that day's weigh-in, or the weight read on the curve between two weigh-ins. */
data class PhotoDay(val date: LocalDate, val photos: List<PhotoEntity>, val kg: Double?, val weighedThatDay: Boolean)

data class PhotosUiState(
    /** Oldest first. */
    val weights: List<WeightEntity> = emptyList(),
    /** Days with at least one photo of the filtered pose, oldest first. */
    val days: List<PhotoDay> = emptyList(),
    val pose: PhotoPose? = null,
    val selected: LocalDate? = null,
    val a: PhotoEntity? = null,
    val b: PhotoEntity? = null,
    val loaded: Boolean = false,
) {
    val selectedDay: PhotoDay? get() = days.firstOrNull { it.date == selected }

    fun dayOf(photo: PhotoEntity): PhotoDay? = days.firstOrNull { it.date == photo.date }
}

private data class Choice(val pose: PhotoPose?, val selected: LocalDate?, val a: PhotoEntity?, val b: PhotoEntity?)

class PhotosViewModel(private val c: AppContainer) : ViewModel() {
    private val choice = MutableStateFlow(Choice(null, null, null, null))

    val state: StateFlow<PhotosUiState> = combine(c.photos.photos, c.body.weightHistory, choice) { photos, weights, ch ->
        val days = photos.filter { ch.pose == null || it.pose == ch.pose }.groupBy { it.date }
            .map { (date, list) -> PhotoDay(date, list, weights.points().kgAt(date), weights.any { it.date == date }) }
            .sortedBy { it.date }
        // Deleted photos must not stay selected.
        val ids = photos.map { it.id }.toSet()
        PhotosUiState(
            weights = weights,
            days = days,
            pose = ch.pose,
            selected = ch.selected ?: days.lastOrNull()?.date,
            a = ch.a?.takeIf { it.id in ids },
            b = ch.b?.takeIf { it.id in ids },
            loaded = true,
        )
    }.stateWhileSubscribed(viewModelScope, PhotosUiState())

    fun setPose(pose: PhotoPose?) = choice.value.let { choice.value = it.copy(pose = pose, selected = null) }

    fun select(date: LocalDate) = choice.value.let { choice.value = it.copy(selected = date) }

    fun setA(photo: PhotoEntity?) = choice.value.let { choice.value = it.copy(a = photo) }

    fun setB(photo: PhotoEntity?) = choice.value.let { choice.value = it.copy(b = photo) }

    fun delete(photo: PhotoEntity) = viewModelScope.launch { c.photos.delete(photo) }
}
