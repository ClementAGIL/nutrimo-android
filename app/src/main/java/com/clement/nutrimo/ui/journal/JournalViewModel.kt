// State of the journal: entries of the selected day grouped by meal, totals, target and water.
package com.clement.nutrimo.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.db.DiaryEntity
import com.clement.nutrimo.domain.calc.CalorieTarget
import com.clement.nutrimo.domain.model.Meal
import com.clement.nutrimo.domain.model.Nutrients
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val DEFAULT_WATER_GOAL_ML = 2000

data class JournalUiState(
    val date: LocalDate = LocalDate.now(),
    val meals: Map<Meal, List<DiaryEntity>> = emptyMap(),
    val total: Nutrients = Nutrients.ZERO,
    val target: CalorieTarget? = null,
    val waterMl: Int = 0,
    val waterGoalMl: Int = DEFAULT_WATER_GOAL_ML,
)

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel(private val c: AppContainer) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)

    val state: StateFlow<JournalUiState> = combine(
        date,
        date.flatMapLatest { c.diary.day(it) },
        date.flatMapLatest { c.diary.water(it) },
        c.body.calorieTarget,
        c.body.profile,
    ) { day, entries, water, target, profile ->
        JournalUiState(
            date = day,
            meals = Meal.entries.associateWith { meal -> entries.filter { it.meal == meal } },
            total = Nutrients.sum(entries.map { it.nutrients }),
            target = target,
            waterMl = water?.ml ?: 0,
            waterGoalMl = profile?.waterGoalMl ?: DEFAULT_WATER_GOAL_ML,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState())

    fun shiftDay(days: Long) { date.value = date.value.plusDays(days) }
    fun setDate(d: LocalDate) { date.value = d }

    fun delete(entry: DiaryEntity) = viewModelScope.launch { c.diary.delete(entry.id) }

    fun copyFromPreviousDay(meal: Meal) = viewModelScope.launch {
        val day = date.value
        val count = c.diary.copyMeal(day.minusDays(1), day, meal)
        messages.tryEmit(if (count == 0) "Rien à copier pour ce repas la veille" else "$count aliment(s) copié(s)")
    }

    fun saveMeal(name: String, entries: List<DiaryEntity>) = viewModelScope.launch {
        c.savedMeals.saveFrom(name, entries)
        messages.tryEmit("Repas « $name » enregistré : retrouvez-le dans l'onglet Repas de la recherche")
    }

    fun addWater(deltaMl: Int) = viewModelScope.launch { c.diary.addWater(date.value, state.value.waterMl, deltaMl) }
}
