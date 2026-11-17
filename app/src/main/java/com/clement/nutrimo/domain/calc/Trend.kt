// Weight trend: least-squares line through the weigh-ins, and the date it reaches the target.
package com.clement.nutrimo.domain.calc

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class WeightPoint(val date: LocalDate, val kg: Double)

/** weight(date) = intercept + slope * days since [origin]; slope in kg/day. */
data class TrendLine(val origin: LocalDate, val intercept: Double, val slopePerDay: Double) {
    fun weightAt(date: LocalDate): Double = intercept + slopePerDay * ChronoUnit.DAYS.between(origin, date)

    /** Date the line crosses [targetKg], or null when it does not go down. */
    fun dateReaching(targetKg: Double): LocalDate? {
        if (slopePerDay >= 0) return null
        val days = (targetKg - intercept) / slopePerDay
        return if (days.isFinite() && days < MAX_PROJECTION_DAYS) origin.plusDays(kotlin.math.ceil(days).toLong()) else null
    }
}

/** Beyond ~10 years the projection is meaningless. */
private const val MAX_PROJECTION_DAYS = 3650.0
const val MIN_TREND_POINTS = 3
/** The trend follows the weigh-ins of the last 30 days (counted back from the latest one). */
const val TREND_WINDOW_DAYS = 30L
/** When the last 30 days hold too few weigh-ins, older ones are borrowed, up to this far back. */
private const val TREND_FALLBACK_DAYS = 90L

object Trend {
    /**
     * The weigh-ins the trend is fitted on: those of the last [TREND_WINDOW_DAYS] before the latest one, whatever the
     * goal's start date; if fewer than [MIN_TREND_POINTS], the latest [MIN_TREND_POINTS] within [TREND_FALLBACK_DAYS].
     */
    fun recent(points: List<WeightPoint>): List<WeightPoint> {
        val sorted = points.sortedBy { it.date }
        val last = sorted.lastOrNull()?.date ?: return emptyList()
        val window = sorted.filter { !it.date.isBefore(last.minusDays(TREND_WINDOW_DAYS)) }
        if (window.size >= MIN_TREND_POINTS) return window
        return sorted.filter { !it.date.isBefore(last.minusDays(TREND_FALLBACK_DAYS)) }.takeLast(MIN_TREND_POINTS)
    }

    /** Returns null with fewer than [MIN_TREND_POINTS] points or when all points share one date. */
    fun fit(points: List<WeightPoint>): TrendLine? {
        if (points.size < MIN_TREND_POINTS) return null
        val origin = points.minOf { it.date }
        val xs = points.map { ChronoUnit.DAYS.between(origin, it.date).toDouble() }
        val ys = points.map { it.kg }
        val mx = xs.average()
        val my = ys.average()
        val sxx = xs.sumOf { (it - mx) * (it - mx) }
        if (sxx == 0.0) return null
        val sxy = xs.indices.sumOf { (xs[it] - mx) * (ys[it] - my) }
        val slope = sxy / sxx
        return TrendLine(origin, my - slope * mx, slope)
    }
}
