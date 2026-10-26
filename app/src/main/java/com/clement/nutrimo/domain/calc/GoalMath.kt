// Goal arithmetic: pace <-> end date, and the progress figures of the weight dashboard.
package com.clement.nutrimo.domain.calc

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

data class GoalProgress(
    val startKg: Double,
    val currentKg: Double,
    val targetKg: Double,
    val lostKg: Double,
    val remainingKg: Double,
    /** 0..1 share of the weight to lose already lost. */
    val weightProgress: Double,
    /** 0..1 share of the goal period already elapsed. */
    val timeProgress: Double,
    val daysElapsed: Long,
    val daysRemaining: Long,
    val avgLossPerWeek: Double?,
    /** kg/week still needed to reach the target on the planned date; null when the date is past. */
    val requiredPace: Double?,
    val trend: TrendLine?,
    val estimatedEnd: LocalDate?,
)

object GoalMath {
    fun endDateForPace(start: LocalDate, startKg: Double, targetKg: Double, paceKgPerWeek: Double): LocalDate {
        val weeks = (startKg - targetKg) / paceKgPerWeek
        return start.plusDays(ceil(weeks * 7).toLong().coerceAtLeast(1))
    }

    fun paceForEndDate(start: LocalDate, startKg: Double, targetKg: Double, end: LocalDate): Double {
        val days = ChronoUnit.DAYS.between(start, end).coerceAtLeast(1)
        return (startKg - targetKg) / days * 7
    }

    fun progress(
        start: LocalDate,
        end: LocalDate,
        startKg: Double,
        targetKg: Double,
        weights: List<WeightPoint>,
        today: LocalDate = LocalDate.now(),
    ): GoalProgress {
        val current = weights.maxByOrNull { it.date }?.kg ?: startKg
        val toLose = startKg - targetKg
        val totalDays = ChronoUnit.DAYS.between(start, end).coerceAtLeast(1)
        val elapsed = ChronoUnit.DAYS.between(start, today).coerceIn(0, totalDays)
        val remainingDays = ChronoUnit.DAYS.between(today, end).coerceAtLeast(0)
        val trend = Trend.fit(weights)
        return GoalProgress(
            startKg = startKg,
            currentKg = current,
            targetKg = targetKg,
            lostKg = startKg - current,
            remainingKg = (current - targetKg).coerceAtLeast(0.0),
            weightProgress = if (toLose > 0) ((startKg - current) / toLose).coerceIn(0.0, 1.0) else 1.0,
            timeProgress = elapsed.toDouble() / totalDays,
            daysElapsed = elapsed,
            daysRemaining = remainingDays,
            avgLossPerWeek = if (elapsed >= 1) (startKg - current) / elapsed * 7 else null,
            requiredPace = if (remainingDays > 0) (current - targetKg).coerceAtLeast(0.0) / remainingDays * 7 else null,
            trend = trend,
            estimatedEnd = trend?.dateReaching(targetKg),
        )
    }
}
