// State of the journal: entries of the selected day grouped by meal, totals, target, water and "Ma journée".
package com.clement.nutrimo.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.R
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.db.ActivityEntity
import com.clement.nutrimo.data.db.DEFAULT_WEIGHT_KG
import com.clement.nutrimo.data.db.DayNoteEntity
import com.clement.nutrimo.data.db.DiaryEntity
import com.clement.nutrimo.data.db.WaterEntity
import com.clement.nutrimo.data.health.HealthInfo
import com.clement.nutrimo.data.health.day
import com.clement.nutrimo.data.repository.goalTargets
import com.clement.nutrimo.domain.calc.CalorieTarget
import com.clement.nutrimo.domain.calc.GoalTargets
import com.clement.nutrimo.domain.calc.MacroSplit
import com.clement.nutrimo.domain.model.Meal
import com.clement.nutrimo.domain.model.Nutrients
import com.clement.nutrimo.ui.common.stateWhileSubscribed
import com.clement.nutrimo.util.UiText
import com.clement.nutrimo.util.uiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.Duration
import com.clement.nutrimo.util.full
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.delay

data class JournalUiState(
    val date: LocalDate = LocalDate.now(),
    val meals: Map<Meal, List<DiaryEntity>> = emptyMap(),
    val total: Nutrients = Nutrients.ZERO,
    val target: CalorieTarget? = null,
    val waterMl: Int = 0,
    val activities: List<ActivityEntity> = emptyList(),
    val macroSplit: MacroSplit = MacroSplit.BALANCED,
    val health: HealthInfo? = null,
    val targets: GoalTargets = GoalTargets(),
    val healthInBudget: Boolean = false,
    /** Mood and short journal of the day, null when nothing written yet. */
    val dayNote: DayNoteEntity? = null,
) {
    /** kcal added to the target: logged activities, plus Health Connect data when the user opted in. */
    val burned: Double get() = activities.sumOf { it.kcal } + (if (healthInBudget) health?.budgetKcal ?: 0.0 else 0.0)

    /** What can be eaten today: the calorie target plus what was burned. */
    val budget: Double? get() = target?.let { it.target + burned }

    /** What keeps the weight stable today: TDEE plus what was burned. */
    val maintenance: Double? get() = target?.let { it.tdee + burned }

    val kcalZone: KcalZone? get() {
        val b = budget ?: return null
        val m = maintenance ?: return null
        return when {
            total.kcal <= b -> KcalZone.UNDER_BUDGET
            total.kcal <= m -> KcalZone.UNDER_MAINTENANCE
            else -> KcalZone.OVER_MAINTENANCE
        }
    }
}

/** Where the day's intake stands: on pace, still losing (slower), or above maintenance. */
enum class KcalZone { UNDER_BUDGET, UNDER_MAINTENANCE, OVER_MAINTENANCE }

/** Everything stored for one day. */
private data class DayData(
    val date: LocalDate, val entries: List<DiaryEntity>, val water: WaterEntity?, val activities: List<ActivityEntity>, val note: DayNoteEntity?,
)

/** The midnight check waits this long past 00:00, so LocalDate.now() is surely the new day. */
private const val MIDNIGHT_MARGIN_MS = 1_000L

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel(private val c: AppContainer) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())

    /**
     * The current day, kept up to date while the app lives across midnight (the ViewModel can survive for days in
     * the background): checked on resume and at each midnight. A journal showing "today" follows it to the new day.
     */
    val today = MutableStateFlow(LocalDate.now())
    val messages = MutableSharedFlow<UiText>(extraBufferCapacity = 1)

    private val day = date.flatMapLatest { d ->
        combine(c.diary.day(d), c.diary.water(d), c.activities.day(d), c.dayNotes.day(d)) { entries, water, activities, note ->
            DayData(d, entries, water, activities, note)
        }
    }

    private val health = MutableStateFlow<HealthInfo?>(null)

    val state: StateFlow<JournalUiState> = combine(day, c.body.calorieTarget, c.body.profile, c.settings.settings, health) { d, target, profile, settings, healthInfo ->
        JournalUiState(
            date = d.date,
            meals = Meal.entries.associateWith { meal -> d.entries.filter { it.meal == meal } },
            total = Nutrients.sum(d.entries.map { it.nutrients }),
            target = target,
            waterMl = d.water?.ml ?: 0,
            activities = d.activities,
            macroSplit = settings.macroSplit,
            health = healthInfo,
            targets = settings.goalTargets(profile),
            healthInBudget = settings.healthInBudget,
            dayNote = d.note,
        )
    }.stateWhileSubscribed(viewModelScope, JournalUiState())

    init {
        viewModelScope.launch {
            while (true) {
                val untilMidnight = Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay())
                delay(untilMidnight.toMillis() + MIDNIGHT_MARGIN_MS)
                checkToday()
            }
        }
    }

    /** Called on resume and at midnight: moves "today" (and the shown day, if it was today) to the real date. */
    fun checkToday() {
        val now = LocalDate.now()
        val previous = today.value
        if (now == previous) return
        today.value = now
        if (date.value == previous) setDate(now)
    }

    fun shiftDay(days: Long) = setDate(date.value.plusDays(days))

    fun setDate(d: LocalDate) {
        date.value = d
        refreshHealth()
    }

    /** Health Connect is not observable: re-read on resume, on day change and after granting the permission. */
    fun refreshHealth() = viewModelScope.launch {
        val day = date.value
        val info = c.health.day(day, c.body.latestWeight()?.kg ?: DEFAULT_WEIGHT_KG)
        // Switching days quickly: a slow read for the previous day must not replace the shown day's data.
        if (date.value == day) health.value = info
    }

    fun delete(entry: DiaryEntity) = viewModelScope.launch { c.diary.delete(entry.id) }

    /** For entries logged on the wrong day or meal: the whole meal goes to [toMeal] of [to], and the journal follows it. */
    fun moveMeal(meal: Meal, to: LocalDate, toMeal: Meal) = viewModelScope.launch {
        val count = c.diary.moveMeal(date.value, meal, to, toMeal)
        if (count == 0) return@launch
        setDate(to)
        messages.tryEmit(uiText(R.string.journal_moved, count, to.full()))
    }

    /** Copies the shown day's [meal] into [toMeal] of [to] (added to what is there), and the journal follows it there. */
    fun copyMeal(meal: Meal, to: LocalDate, toMeal: Meal) = viewModelScope.launch {
        val count = c.diary.copyMeal(date.value, meal, to, toMeal)
        if (count == 0) return@launch
        setDate(to)
        messages.tryEmit(uiText(R.string.journal_copied_to, count, to.full()))
    }

    fun saveMeal(name: String, entries: List<DiaryEntity>) = viewModelScope.launch {
        c.savedMeals.saveFrom(name, entries)
        messages.tryEmit(uiText(R.string.journal_meal_saved, name))
    }

    fun addActivity(type: String, minutes: Int?, kcal: Double?) = viewModelScope.launch {
        c.activities.add(date.value, type, minutes, kcal)
    }

    fun deleteActivity(activity: ActivityEntity) = viewModelScope.launch { c.activities.delete(activity.id) }

    /** Saves "Ma journée" of the shown day (an emptied one is deleted). */
    fun saveDayNote(note: DayNoteEntity) = viewModelScope.launch { c.dayNotes.save(note.copy(date = date.value)) }

    fun addWater(deltaMl: Int) = viewModelScope.launch { c.diary.addWater(date.value, deltaMl) }
}
