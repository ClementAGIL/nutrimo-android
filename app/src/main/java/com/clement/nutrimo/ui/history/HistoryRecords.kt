// Merges the diary, activities, water, weigh-ins and Health Connect data into one DayRecord per day.
package com.clement.nutrimo.ui.history

import com.clement.nutrimo.data.db.DayBurn
import com.clement.nutrimo.data.db.DayTotal
import com.clement.nutrimo.data.db.WaterEntity
import com.clement.nutrimo.data.db.WeightEntity
import com.clement.nutrimo.data.health.ImportedActivity
import com.clement.nutrimo.data.health.healthBudgetKcal
import com.clement.nutrimo.domain.calc.Activities
import com.clement.nutrimo.domain.calc.DayRecord
import java.time.LocalDate

/** What the phone stores locally for the loaded range. */
data class LocalHistory(
    val totals: List<DayTotal> = emptyList(),
    val burned: List<DayBurn> = emptyList(),
    val water: List<WaterEntity> = emptyList(),
    val weights: List<WeightEntity> = emptyList(),
)

/** What Health Connect returned for the loaded range (empty without permission). */
data class HealthHistory(
    val steps: Map<LocalDate, Long> = emptyMap(),
    val sessions: Map<LocalDate, List<ImportedActivity>> = emptyMap(),
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
    val dates = eaten.keys + manual.keys + water.keys + weights.keys + health.steps.keys + health.sessions.keys
    return dates.associateWith { date ->
        val sessions = health.sessions[date].orEmpty()
        val steps = health.steps[date]
        val fromHealth = if (healthInBudget) healthBudgetKcal(steps?.let { Activities.stepsKcal(it, kg) } ?: 0.0, sessions) else 0.0
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
        )
    }
}
