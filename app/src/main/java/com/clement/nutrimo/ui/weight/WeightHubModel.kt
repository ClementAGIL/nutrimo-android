// What the weight hub chart shows, without drawing it: the dates of a period chip, the span of the trend line, and
// the day under a tap (weigh-in, measurements, photos). Pure functions, tested in WeightHubModelTest.
package com.clement.nutrimo.ui.weight

import com.clement.nutrimo.data.db.MeasurementEntity
import com.clement.nutrimo.data.db.PhotoEntity
import com.clement.nutrimo.data.db.WeightEntity
import com.clement.nutrimo.data.db.points
import com.clement.nutrimo.domain.calc.TREND_WINDOW_DAYS
import com.clement.nutrimo.domain.calc.TrendLine
import com.clement.nutrimo.domain.calc.kgAt
import com.clement.nutrimo.ui.common.chart.ChartPeriod
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToLong

/** With a chip, the last weigh-in sits at 3/4 of the width: the last quarter shows the goal and trend ahead. */
internal const val PRESET_PAST_SHARE = 0.75
/** Past shown by a chip when the history is a single day. */
internal const val MIN_PRESET_DAYS = 7L

/** Everything recorded on one day, shown in the bubble. */
data class HubDay(val date: LocalDate, val kg: Double?, val weighed: Boolean, val measurement: MeasurementEntity?, val photos: List<PhotoEntity>)

/**
 * Dates of a chip: [period] back from the last weigh-in, which then sits at [PRESET_PAST_SHARE] of the width; the
 * rest is the future, where the goal and trend lines continue.
 */
internal fun presetRange(period: ChartPeriod, firstWeighIn: LocalDate, lastWeighIn: LocalDate): Pair<LocalDate, LocalDate> {
    val start = maxOf(period.start(lastWeighIn) ?: firstWeighIn, firstWeighIn).let { if (it < lastWeighIn) it else lastWeighIn.minusDays(MIN_PRESET_DAYS) }
    val past = ChronoUnit.DAYS.between(start, lastWeighIn)
    return start to lastWeighIn.plusDays((past * (1 - PRESET_PAST_SHARE) / PRESET_PAST_SHARE).roundToLong())
}

/**
 * Dates of the trend line: from the first weigh-in it is fitted on (Trend.recent, may predate the goal) to the
 * estimated arrival; when the trend doesn't go down there is none, so it still runs ahead (to the planned end, at
 * least [TREND_WINDOW_DAYS] past the last weigh-in) to show where it leads.
 */
internal fun trendLineDates(trend: TrendLine, estimatedEnd: LocalDate?, goalEnd: LocalDate, lastWeighIn: LocalDate): Pair<LocalDate, LocalDate> =
    trend.origin to (estimatedEnd ?: maxOf(goalEnd, lastWeighIn.plusDays(TREND_WINDOW_DAYS)))

/** The closest day with data to [tapped], within [tolerance] days; null when none is close enough. */
internal fun nearestDay(
    tapped: LocalDate, tolerance: Long, weights: List<WeightEntity>, measurements: List<MeasurementEntity>, photos: List<PhotoEntity>,
): HubDay? {
    val dates = (weights.map { it.date } + measurements.map { it.date } + photos.map { it.date }).distinct()
    val date = dates.minByOrNull { abs(ChronoUnit.DAYS.between(tapped, it)) }
        ?.takeIf { abs(ChronoUnit.DAYS.between(tapped, it)) <= tolerance } ?: return null
    return HubDay(
        date = date,
        kg = weights.points().kgAt(date),
        weighed = weights.any { it.date == date },
        measurement = measurements.firstOrNull { it.date == date },
        photos = photos.filter { it.date == date },
    )
}
