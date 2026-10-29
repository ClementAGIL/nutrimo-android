// One day of Health Connect data as the journal shows it: access state, steps and workouts with their calories.
package com.clement.nutrimo.data.health

import com.clement.nutrimo.domain.calc.Activities
import java.time.LocalDate

/**
 * Health Connect state and data of one day; [stepsKcal] is our estimate (steps beyond the baseline), used only
 * when [activeKcal] (measured by the user's apps) is null, i.e. without the sport and calorie permissions.
 */
data class HealthInfo(
    val status: HealthStatus,
    val canReadSteps: Boolean,
    /** False when only some permissions were granted (e.g. steps but not workouts). */
    val allGranted: Boolean,
    val steps: Long?,
    val stepsKcal: Double,
    val activeKcal: Double?,
    val sessions: List<ImportedActivity>,
) {
    val budgetKcal: Double get() = healthBudgetKcal(activeKcal, stepsKcal, sessions)
}

/** Reads [date]; calories are computed for a person of [kg]. */
suspend fun HealthRepository.day(date: LocalDate, kg: Double): HealthInfo {
    val granted = granted()
    val canReadSteps = STEPS_PERMISSION in granted
    val count = if (canReadSteps) stepsOn(date) else null
    return HealthInfo(
        status = status(),
        canReadSteps = canReadSteps,
        allGranted = granted.containsAll(HEALTH_PERMISSIONS),
        steps = count,
        stepsKcal = count?.let { Activities.stepsKcal(it, kg) } ?: 0.0,
        activeKcal = activeKcalOn(date),
        sessions = if (canReadSteps) sessionsOn(date).map { it.toImported(kg) } else emptyList(),
    )
}
