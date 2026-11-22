// Date axis with gaps: the periods around the data dates are kept day by day, while a long stretch without data is
// collapsed into a short break. Used by the photo timeline and the measurement charts. Pure logic, tested in GapAxisTest.
package com.clement.nutrimo.ui.common.chart

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** More than this without data (about 6 months) is drawn as a short break instead of the empty months. */
const val GAP_DAYS = 183L

/** Days kept on each side of a data date, so points don't touch a break or an edge. */
const val MARGIN_DAYS = 10L

/** Width of a break, counted in days of the axis. */
const val BREAK_DAYS = 12L

/** [dates] (data days, plus e.g. today to keep a place for it) must not be empty. */
class GapAxis(dates: List<LocalDate>) {
    /** Visible date ranges, oldest first (both ends included). */
    val segments: List<ClosedRange<LocalDate>>

    /** Axis length in days, breaks included. */
    val totalDays: Long

    /** Position (days from the left edge) of the middle of each break. */
    val breaks: List<Long>

    /** Position of each segment's first day. */
    private val offsets: List<Long>

    init {
        val anchors = dates.distinct().sorted()
        val kept = mutableListOf<ClosedRange<LocalDate>>()
        var start = anchors.first().minusDays(MARGIN_DAYS)
        anchors.zipWithNext().forEach { (a, b) ->
            if (ChronoUnit.DAYS.between(a, b) > GAP_DAYS) {
                kept += start..a.plusDays(MARGIN_DAYS)
                start = b.minusDays(MARGIN_DAYS)
            }
        }
        kept += start..anchors.last().plusDays(MARGIN_DAYS)
        segments = kept

        var position = 0L
        val starts = mutableListOf<Long>()
        val middles = mutableListOf<Long>()
        kept.forEachIndexed { i, range ->
            if (i > 0) {
                middles += position + BREAK_DAYS / 2
                position += BREAK_DAYS
            }
            starts += position
            position += ChronoUnit.DAYS.between(range.start, range.endInclusive)
        }
        offsets = starts
        breaks = middles
        totalDays = position.coerceAtLeast(1)
    }

    /** Like [position], but a date inside a collapsed stretch is placed in the middle of its break. */
    fun place(date: LocalDate): Long {
        position(date)?.let { return it }
        val next = segments.indexOfFirst { date < it.start }
        return when {
            next < 0 -> totalDays
            next == 0 -> 0
            else -> breaks[next - 1]
        }
    }

    /** Date at [position] (days from the left edge); a position inside a break gives the closer edge of the break. */
    fun dateAt(position: Long): LocalDate {
        val p = position.coerceIn(0, totalDays)
        val i = offsets.indexOfLast { it <= p }.coerceAtLeast(0)
        val length = ChronoUnit.DAYS.between(segments[i].start, segments[i].endInclusive)
        val inside = p - offsets[i]
        if (inside <= length || i == segments.lastIndex) return segments[i].start.plusDays(inside.coerceAtMost(length))
        // In the break after segment i: snap to its end or to the next segment's start.
        return if (inside - length <= BREAK_DAYS / 2) segments[i].endInclusive else segments[i + 1].start
    }

    /** Position of [date] in days from the left edge, or null when it falls in a collapsed stretch. */
    fun position(date: LocalDate): Long? {
        val i = segments.indexOfFirst { date in it }
        if (i < 0) return null
        return offsets[i] + ChronoUnit.DAYS.between(segments[i].start, date)
    }
}
