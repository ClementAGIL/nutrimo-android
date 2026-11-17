// Weight on any day, read on the curve of the weigh-ins (photos and measurements are rarely taken on a weigh-in day).
package com.clement.nutrimo.domain.calc

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Plausible weights for a typed weigh-in or goal (catches typos such as 1300 for 130,0). */
val VALID_WEIGHT_KG = 30.0..400.0

/** Weigh-in of [date], else linear between the surrounding weigh-ins, else the nearest one; null without any. Sorted by date. */
fun List<WeightPoint>.kgAt(date: LocalDate): Double? {
    if (isEmpty()) return null
    firstOrNull { it.date == date }?.let { return it.kg }
    val before = lastOrNull { it.date.isBefore(date) } ?: return first().kg
    val after = firstOrNull { it.date.isAfter(date) } ?: return before.kg
    val ratio = ChronoUnit.DAYS.between(before.date, date).toDouble() / ChronoUnit.DAYS.between(before.date, after.date)
    return before.kg + (after.kg - before.kg) * ratio
}
