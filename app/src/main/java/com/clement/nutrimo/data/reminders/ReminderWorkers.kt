// What each reminder checks before notifying: weigh-in / meals only when nothing was logged today, photos on the 1st
// when none this month, and the daily encouragement (Encouragement.pick over the last days).
package com.clement.nutrimo.data.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.clement.nutrimo.NutrimoApp
import com.clement.nutrimo.R
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.repository.goalTargets
import com.clement.nutrimo.data.repository.loadDayRecords
import com.clement.nutrimo.domain.calc.Encouragement
import com.clement.nutrimo.domain.calc.EncouragementFacts
import com.clement.nutrimo.domain.calc.WeightPoint
import com.clement.nutrimo.util.UiText
import com.clement.nutrimo.util.uiText
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Days of history the encouragement looks at (streaks, last weigh-ins). */
private const val ENCOURAGE_HISTORY_DAYS = 60L

/** A reminder job: [check] notifies when needed; a failure never retries (the next day's run will try again). */
abstract class ReminderWorker(context: Context, params: WorkerParameters, private val kind: ReminderKind) : CoroutineWorker(context, params) {
    protected val container: AppContainer get() = (applicationContext as NutrimoApp).container

    /** Title and text of the notification to post, or null when there is nothing to say. */
    protected abstract suspend fun check(today: LocalDate): Pair<UiText, UiText>?

    override suspend fun doWork(): Result {
        val res = applicationContext.resources
        check(LocalDate.now())?.let { (title, text) -> Reminders.notify(applicationContext, kind, title.resolve(res), text.resolve(res)) }
        return Result.success()
    }
}

class WeighReminderWorker(context: Context, params: WorkerParameters) : ReminderWorker(context, params, ReminderKind.WEIGH) {
    override suspend fun check(today: LocalDate) =
        (uiText(R.string.notify_weigh_title) to uiText(R.string.notify_weigh_text)).takeIf { container.body.weightOn(today) == null }
}

class MealReminderWorker(context: Context, params: WorkerParameters) : ReminderWorker(context, params, ReminderKind.MEAL) {
    override suspend fun check(today: LocalDate) =
        (uiText(R.string.notify_meal_title) to uiText(R.string.notify_meal_text)).takeIf { !container.diary.hasEntries(today) }
}

/** Runs every day (months differ in length), notifies on the 1st only. */
class PhotoReminderWorker(context: Context, params: WorkerParameters) : ReminderWorker(context, params, ReminderKind.PHOTO) {
    override suspend fun check(today: LocalDate): Pair<UiText, UiText>? {
        if (today.dayOfMonth != 1) return null
        val noPhotoThisMonth = container.photos.latestDate()?.isBefore(today.withDayOfMonth(1)) != false
        return (uiText(R.string.notify_photo_title) to uiText(R.string.notify_photo_text)).takeIf { noPhotoThisMonth }
    }
}

class EncouragementWorker(context: Context, params: WorkerParameters) : ReminderWorker(context, params, ReminderKind.ENCOURAGEMENT) {
    override suspend fun check(today: LocalDate): Pair<UiText, UiText> {
        val c = container
        val goal = c.body.activeGoal.first()
        val profile = c.body.profile.first()
        val facts = EncouragementFacts(
            today = today,
            records = c.loadDayRecords(today.minusDays(ENCOURAGE_HISTORY_DAYS), today),
            targets = c.settings.current().goalTargets(profile),
            weights = c.body.weightHistory.first().map { WeightPoint(it.date, it.kg) },
            goalStart = goal?.startDate,
            goalStartKg = goal?.startKg,
            heightCm = profile?.heightCm,
        )
        return Encouragement.pick(facts).let { it.title to it.text }
    }
}
