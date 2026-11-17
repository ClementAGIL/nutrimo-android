// Horizontal axis of the photo timeline: the periods around the photos (and today) are kept day by day, while a long
// stretch without any photo is collapsed into a short break. Pure logic, tested in TimelineAxisTest.
package com.clement.nutrimo.ui.photo

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** More than this without a photo (about 6 months) is drawn as a short break instead of the empty months. */
internal const val GAP_DAYS = 183L

/** Days kept on each side of a photo day (and of today), so points don't touch a break or an edge. */
internal const val MARGIN_DAYS = 10L

/** Width of a break, counted in days of the axis. */
internal const val BREAK_DAYS = 12L

/** [photoDates] must not be empty; [today] always gets a visible place at the right end. */
internal class TimelineAxis(photoDates: List<LocalDate>, today: LocalDate) {
    /** Visible date ranges, oldest first (both ends included). */
    val segments: List<ClosedRange<LocalDate>>

    /** Axis length in days, breaks included. */
    val totalDays: Long

    /** Position (days from the left edge) of the middle of each break. */
    val breaks: List<Long>

    /** Position of each segment's first day. */
    private val offsets: List<Long>

    init {
        val anchors = (photoDates + today).distinct().sorted()
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

    /** Position of [date] in days from the left edge, or null when it falls in a collapsed stretch. */
    fun position(date: LocalDate): Long? {
        val i = segments.indexOfFirst { date in it }
        if (i < 0) return null
        return offsets[i] + ChronoUnit.DAYS.between(segments[i].start, date)
    }
}
