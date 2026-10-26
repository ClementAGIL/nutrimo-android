// Health Connect workouts turned into journal activities: label, calories (measured or estimated), budget rule.
package com.clement.nutrimo.data.health

import androidx.health.connect.client.records.ExerciseSessionRecord as E
import com.clement.nutrimo.domain.calc.Activities

/** Health Connect exercise type → key of Activities.TYPES (for the label and the MET estimate). */
private val TYPE_TO_ACTIVITY = mapOf(
    E.EXERCISE_TYPE_WALKING to "walk",
    E.EXERCISE_TYPE_HIKING to "brisk_walk",
    E.EXERCISE_TYPE_RUNNING to "run",
    E.EXERCISE_TYPE_RUNNING_TREADMILL to "run",
    E.EXERCISE_TYPE_BIKING to "bike",
    E.EXERCISE_TYPE_BIKING_STATIONARY to "bike",
    E.EXERCISE_TYPE_SWIMMING_POOL to "swim",
    E.EXERCISE_TYPE_SWIMMING_OPEN_WATER to "swim",
    E.EXERCISE_TYPE_STRENGTH_TRAINING to "strength",
    E.EXERCISE_TYPE_WEIGHTLIFTING to "strength",
    E.EXERCISE_TYPE_YOGA to "yoga",
    E.EXERCISE_TYPE_PILATES to "yoga",
    E.EXERCISE_TYPE_STRETCHING to "yoga",
    E.EXERCISE_TYPE_SOCCER to "football",
    E.EXERCISE_TYPE_TENNIS to "tennis",
    E.EXERCISE_TYPE_DANCING to "dance",
)

/** Workouts made of steps: their energy already counts through the steps, so they never count twice. */
private val STEP_BASED = setOf(E.EXERCISE_TYPE_WALKING, E.EXERCISE_TYPE_HIKING, E.EXERCISE_TYPE_RUNNING, E.EXERCISE_TYPE_RUNNING_TREADMILL)

/** Unknown sports: a moderate effort. */
private const val DEFAULT_MET = 5.0

data class ImportedActivity(
    val label: String,
    val minutes: Int,
    val kcal: Double,
    /** True when the calories come from a device (watch, band), false when estimated from the sport. */
    val measured: Boolean,
    val countsInBudget: Boolean,
)

fun HealthSession.toImported(weightKg: Double): ImportedActivity {
    val type = TYPE_TO_ACTIVITY[exerciseType]?.let(Activities::byKey)
    val estimate = Activities.burned(type?.met ?: DEFAULT_MET, weightKg, minutes)
    return ImportedActivity(
        label = title?.takeIf { it.isNotBlank() } ?: type?.label ?: "Séance",
        minutes = minutes,
        kcal = measuredKcal ?: estimate,
        measured = measuredKcal != null,
        countsInBudget = exerciseType !in STEP_BASED,
    )
}
