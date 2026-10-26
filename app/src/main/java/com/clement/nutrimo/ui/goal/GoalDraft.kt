// Editable goal state: pace preset or end date, resolved into a pace + end date and validated.
package com.clement.nutrimo.ui.goal

import com.clement.nutrimo.data.db.GoalEntity
import com.clement.nutrimo.domain.calc.GoalMath
import com.clement.nutrimo.domain.model.GoalMode
import com.clement.nutrimo.ui.common.toDecimal
import java.time.LocalDate

/** Pace presets in kg/week; [CUSTOM_PACE] selects the free field. */
val PACE_PRESETS = listOf(0.5, 1.0, 2.0)
const val CUSTOM_PACE = -1.0
private const val MIN_PACE = 0.05
private const val MAX_PACE = 3.0

data class GoalDraft(
    val startDate: LocalDate = LocalDate.now(),
    val startKg: String = "",
    val targetKg: String = "",
    val mode: GoalMode = GoalMode.PACE,
    val preset: Double = 0.5,
    val customPace: String = "",
    val endDate: LocalDate = LocalDate.now().plusMonths(3),
) {
    val start: Double? get() = startKg.toDecimal()?.takeIf { it in 30.0..400.0 }
    val target: Double? get() = targetKg.toDecimal()?.takeIf { it in 30.0..400.0 }

    /** kg/week, from the preset / custom field, or from the end date. */
    val pace: Double?
        get() {
            val s = start ?: return null
            val t = target ?: return null
            return when (mode) {
                GoalMode.PACE -> if (preset == CUSTOM_PACE) customPace.toDecimal() else preset
                GoalMode.DATE -> GoalMath.paceForEndDate(startDate, s, t, endDate)
            }
        }

    val resolvedEnd: LocalDate?
        get() {
            val s = start ?: return null
            val t = target ?: return null
            val p = pace?.takeIf { it > 0 } ?: return null
            return if (mode == GoalMode.DATE) endDate else GoalMath.endDateForPace(startDate, s, t, p)
        }

    val error: String?
        get() = when {
            start == null -> "Poids de départ invalide"
            target == null -> "Poids objectif invalide"
            target!! >= start!! -> "L'objectif doit être inférieur au poids de départ"
            pace == null || pace!! < MIN_PACE -> "Rythme invalide"
            pace!! > MAX_PACE -> "Rythme trop élevé (max ${MAX_PACE.toInt()} kg/semaine)"
            else -> null
        }

    fun toEntity(): GoalEntity? {
        if (error != null) return null
        return GoalEntity(
            startDate = startDate,
            startKg = start!!,
            targetKg = target!!,
            mode = mode,
            paceKgPerWeek = pace!!,
            endDate = resolvedEnd!!,
        )
    }
}
