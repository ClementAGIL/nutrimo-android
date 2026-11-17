// What each reminder checks before notifying: weigh-in / meals only when nothing was logged today, photos on the 1st
// when none this month, and the daily encouragement (Encouragement.pick over the last days).
package com.clement.nutrimo.data.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.clement.nutrimo.NutrimoApp
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.repository.goalTargets
import com.clement.nutrimo.data.repository.loadDayRecords
import com.clement.nutrimo.domain.calc.Encouragement
import com.clement.nutrimo.domain.calc.EncouragementFacts
import com.clement.nutrimo.domain.calc.WeightPoint
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Days of history the encouragement looks at (streaks, last weigh-ins). */
private const val ENCOURAGE_HISTORY_DAYS = 60L

/** A reminder job: [check] notifies when needed; a failure never retries (the next day's run will try again). */
abstract class ReminderWorker(context: Context, params: WorkerParameters, private val kind: ReminderKind) : CoroutineWorker(context, params) {
    protected val container: AppContainer get() = (applicationContext as NutrimoApp).container

    protected abstract suspend fun check(today: LocalDate): Pair<String, String>?

    override suspend fun doWork(): Result {
        check(LocalDate.now())?.let { (title, text) -> Reminders.notify(applicationContext, kind, title, text) }
        return Result.success()
    }
}

class WeighReminderWorker(context: Context, params: WorkerParameters) : ReminderWorker(context, params, ReminderKind.WEIGH) {
    override suspend fun check(today: LocalDate) =
        ("C'est l'heure de la pesée" to "Notez votre poids du jour dans Nutrimo.").takeIf { container.body.weightOn(today) == null }
}

class MealReminderWorker(context: Context, params: WorkerParameters) : ReminderWorker(context, params, ReminderKind.MEAL) {
    override suspend fun check(today: LocalDate) =
        ("Journal vide aujourd'hui" to "Pensez à noter vos repas.").takeIf { !container.diary.hasEntries(today) }
}

/** Runs every day (months differ in length), notifies on the 1st only. */
class PhotoReminderWorker(context: Context, params: WorkerParameters) : ReminderWorker(context, params, ReminderKind.PHOTO) {
    override suspend fun check(today: LocalDate): Pair<String, String>? {
        if (today.dayOfMonth != 1) return null
        val noPhotoThisMonth = container.photos.latestDate()?.isBefore(today.withDayOfMonth(1)) != false
        return ("Photos du mois" to "Prenez vos photos de progression pour suivre l'évolution.").takeIf { noPhotoThisMonth }
    }
}

class EncouragementWorker(context: Context, params: WorkerParameters) : ReminderWorker(context, params, ReminderKind.ENCOURAGEMENT) {
    override suspend fun check(today: LocalDate): Pair<String, String> {
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
