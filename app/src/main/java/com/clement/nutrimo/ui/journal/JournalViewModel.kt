// State of the journal: entries of the selected day grouped by meal, totals, target and water.
package com.clement.nutrimo.ui.journal

import com.clement.nutrimo.data.db.DEFAULT_WEIGHT_KG
import com.clement.nutrimo.data.db.DEFAULT_WATER_GOAL_ML
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.db.ActivityEntity
import com.clement.nutrimo.data.db.DiaryEntity
import com.clement.nutrimo.data.db.WaterEntity
import com.clement.nutrimo.data.health.HEALTH_PERMISSIONS
import com.clement.nutrimo.data.health.HealthStatus
import com.clement.nutrimo.data.health.ImportedActivity
import com.clement.nutrimo.data.health.STEPS_PERMISSION
import com.clement.nutrimo.data.health.healthBudgetKcal
import com.clement.nutrimo.data.health.toImported
import com.clement.nutrimo.domain.calc.Activities
import com.clement.nutrimo.domain.calc.CalorieTarget
import com.clement.nutrimo.domain.calc.MacroSplit
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

data class JournalUiState(
    val date: LocalDate = LocalDate.now(),
    val meals: Map<Meal, List<DiaryEntity>> = emptyMap(),
    val total: Nutrients = Nutrients.ZERO,
    val target: CalorieTarget? = null,
    val waterMl: Int = 0,
    val waterGoalMl: Int = DEFAULT_WATER_GOAL_ML,
    val activities: List<ActivityEntity> = emptyList(),
    val macroSplit: MacroSplit = MacroSplit.BALANCED,
    val health: HealthInfo? = null,
    val stepGoal: Int = 10_000,
    val healthInBudget: Boolean = false,
) {
    /** kcal added to the target: logged activities, plus Health Connect data when the user opted in. */
    val burned: Double get() = activities.sumOf { it.kcal } + (if (healthInBudget) health?.budgetKcal ?: 0.0 else 0.0)

    /** What can be eaten today: the calorie target plus what was burned. */
    val budget: Double? get() = target?.let { it.target + burned }
}

/** Health Connect state and data of the selected day; [stepsKcal] counts only steps beyond the baseline. */
data class HealthInfo(
    val status: HealthStatus,
    val canReadSteps: Boolean,
    /** False when only some permissions were granted (e.g. steps but not workouts). */
    val allGranted: Boolean,
    val steps: Long?,
    val stepsKcal: Double,
    val sessions: List<ImportedActivity>,
) {
    val budgetKcal: Double get() = healthBudgetKcal(stepsKcal, sessions)
}

/** Everything stored for one day. */
private data class DayData(val date: LocalDate, val entries: List<DiaryEntity>, val water: WaterEntity?, val activities: List<ActivityEntity>)

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel(private val c: AppContainer) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)

    private val day = date.flatMapLatest { d ->
        combine(c.diary.day(d), c.diary.water(d), c.activities.day(d)) { entries, water, activities -> DayData(d, entries, water, activities) }
    }

    private val health = MutableStateFlow<HealthInfo?>(null)

    val state: StateFlow<JournalUiState> = combine(day, c.body.calorieTarget, c.body.profile, c.settings.settings, health) { d, target, profile, settings, healthInfo ->
        JournalUiState(
            date = d.date,
            meals = Meal.entries.associateWith { meal -> d.entries.filter { it.meal == meal } },
            total = Nutrients.sum(d.entries.map { it.nutrients }),
            target = target,
            waterMl = d.water?.ml ?: 0,
            waterGoalMl = profile?.waterGoalMl ?: DEFAULT_WATER_GOAL_ML,
            activities = d.activities,
            macroSplit = settings.macroSplit,
            health = healthInfo,
            stepGoal = settings.stepGoal,
            healthInBudget = settings.healthInBudget,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState())

    fun shiftDay(days: Long) = setDate(date.value.plusDays(days))

    fun setDate(d: LocalDate) {
        date.value = d
        refreshHealth()
    }

    /** Health Connect is not observable: re-read on resume, on day change and after granting the permission. */
    fun refreshHealth() = viewModelScope.launch {
        val day = date.value
        val granted = c.health.granted()
        val canReadSteps = STEPS_PERMISSION in granted
        val count = if (canReadSteps) c.health.stepsOn(day) else null
        val kg = c.body.latestWeight()?.kg ?: DEFAULT_WEIGHT_KG
        val sessions = if (canReadSteps) c.health.sessionsOn(day).map { it.toImported(kg) } else emptyList()
        health.value = HealthInfo(
            status = c.health.status(),
            canReadSteps = canReadSteps,
            allGranted = granted.containsAll(HEALTH_PERMISSIONS),
            steps = count,
            stepsKcal = count?.let { Activities.stepsKcal(it, kg) } ?: 0.0,
            sessions = sessions,
        )
    }

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

    fun addActivity(type: String, minutes: Int?, kcal: Double?) = viewModelScope.launch {
        c.activities.add(date.value, type, minutes, kcal)
    }

    fun deleteActivity(activity: ActivityEntity) = viewModelScope.launch { c.activities.delete(activity.id) }

    fun addWater(deltaMl: Int) = viewModelScope.launch { c.diary.addWater(date.value, state.value.waterMl, deltaMl) }
}
