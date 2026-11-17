// Merges the diary, activities, water, weigh-ins and Health Connect data into one DayRecord per day (History screen
// and the daily encouragement notification).
package com.clement.nutrimo.data.repository

import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.db.DEFAULT_WEIGHT_KG
import com.clement.nutrimo.data.db.DayNoteEntity
import com.clement.nutrimo.data.db.DayBurn
import com.clement.nutrimo.data.db.DayTotal
import com.clement.nutrimo.data.db.WaterEntity
import com.clement.nutrimo.data.db.WeightEntity
import com.clement.nutrimo.data.health.HealthRepository
import com.clement.nutrimo.data.health.ImportedActivity
import com.clement.nutrimo.data.health.healthBudgetKcal
import com.clement.nutrimo.data.health.toImported
import com.clement.nutrimo.domain.calc.Activities
import com.clement.nutrimo.domain.calc.DayRecord
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** What the phone stores locally for the loaded range. */
data class LocalHistory(
    val totals: List<DayTotal> = emptyList(),
    val burned: List<DayBurn> = emptyList(),
    val water: List<WaterEntity> = emptyList(),
    val weights: List<WeightEntity> = emptyList(),
    val notes: List<DayNoteEntity> = emptyList(),
)

/** What Health Connect returned for the loaded range (empty without permission). */
data class HealthHistory(
    val steps: Map<LocalDate, Long> = emptyMap(),
    val sessions: Map<LocalDate, List<ImportedActivity>> = emptyMap(),
    /** Null when the calorie permissions are missing: the step estimate is used instead. */
    val activeKcal: Map<LocalDate, Double>? = null,
)

/**
 * [target] is today's calorie target, used for every day (past targets are not stored).
 * [healthInBudget] mirrors the journal: Health Connect calories raise the budget only when enabled.
 */
fun buildRecords(local: LocalHistory, health: HealthHistory, target: Double?, healthInBudget: Boolean, kg: Double): Map<LocalDate, DayRecord> {
    val eaten = local.totals.associate { it.date to it.kcal }
    val manual = local.burned.associate { it.date to it.kcal }
    val water = local.water.associate { it.date to it.ml }
    val sorted = local.weights.sortedBy { it.date }
    val weights = sorted.associate { it.date to it.kg }
    val previous = sorted.zipWithNext().associate { (before, day) -> day.date to before.kg }
    val notes = local.notes.associateBy { it.date }
    val dates = eaten.keys + manual.keys + water.keys + weights.keys + health.steps.keys + health.sessions.keys + notes.keys
    return dates.associateWith { date ->
        val sessions = health.sessions[date].orEmpty()
        val steps = health.steps[date]
        val fromHealth = if (healthInBudget) healthBudgetKcal(health.activeKcal?.let { it[date] ?: 0.0 }, steps?.let { Activities.stepsKcal(it, kg) } ?: 0.0, sessions) else 0.0
        val burned = manual[date] ?: 0.0
        DayRecord(
            date = date,
            eatenKcal = eaten[date],
            budgetKcal = target?.plus(burned + fromHealth),
            sportKcal = burned + sessions.sumOf { it.kcal },
            steps = steps,
            waterMl = water[date],
            weightKg = weights[date],
            previousWeightKg = previous[date],
            mood = notes[date]?.mood,
            journaled = notes[date]?.let { it.note.isNotBlank() || it.gratitude.isNotBlank() || it.win.isNotBlank() } == true,
        )
    }
}

/** Health Connect data of [from]..[to] (empty when unavailable, e.g. no permission or read in the background). */
suspend fun HealthRepository.history(from: LocalDate, to: LocalDate, kg: Double): HealthHistory {
    val sessions = sessionsBetween(from, to).groupBy({ it.first }, { it.second.toImported(kg) })
    return HealthHistory(dailySteps(from, to), sessions, dailyActiveKcal(from, to))
}

/** One-shot read of the day records of [from]..[to], for code that can't observe flows (the reminder workers). */
suspend fun AppContainer.loadDayRecords(from: LocalDate, to: LocalDate): Map<LocalDate, DayRecord> {
    val weights = body.weightHistory.first()
    val kg = weights.lastOrNull()?.kg ?: DEFAULT_WEIGHT_KG
    val local = LocalHistory(
        diary.dailyTotals(from, to).first(), activities.burned(from, to).first(), diary.waterBetween(from, to).first(), weights,
        dayNotes.between(from, to).first(),
    )
    return buildRecords(local, health.history(from, to, kg), body.calorieTarget.first()?.target, settings.current().healthInBudget, kg)
}
