// The app's reminders, described once: settings keys, WorkManager job, notification id and channel, defaults, period.
package com.clement.nutrimo.data.reminders

import com.clement.nutrimo.data.repository.ReminderSettings
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/**
 * [key] prefixes the DataStore keys ("weigh_enabled", "weigh_hour"…), kept as they were so saved settings survive.
 * [weekly] jobs run on [ReminderSettings.day]; the others every day (the photo worker only notifies on the 1st).
 */
enum class ReminderKind(
    val key: String,
    val workName: String,
    val notificationId: Int,
    val defaults: ReminderSettings,
    val weekly: Boolean = false,
    val channel: String = REMINDERS_CHANNEL,
) {
    WEIGH("weigh", "weigh_reminder", 1, ReminderSettings(false, 8, 0), weekly = true),
    MEAL("meal", "meal_reminder", 2, ReminderSettings(false, 20, 0)),
    /** On by default, early evening so an open streak can still be kept. */
    ENCOURAGEMENT("encourage", "encouragement", 4, ReminderSettings(true, 19, 0), channel = ENCOURAGEMENTS_CHANNEL),
    /** On by default: progress photos are only useful when taken regularly. */
    PHOTO("photo", "photo_reminder", 3, ReminderSettings(true, 10, 0)),
}

const val REMINDERS_CHANNEL = "reminders"
const val ENCOURAGEMENTS_CHANNEL = "encouragements"

/** Next run of a reminder at its time (on its day when [weekly]), strictly after [now]. */
fun nextRun(now: LocalDateTime, r: ReminderSettings, weekly: Boolean): LocalDateTime {
    var next = now.withHour(r.hour).withMinute(r.minute).withSecond(0).withNano(0)
    if (weekly) next = next.with(TemporalAdjusters.nextOrSame(r.day))
    if (!next.isAfter(now)) next = if (weekly) next.plusWeeks(1) else next.plusDays(1)
    return next
}
