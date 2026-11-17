// Weigh-in, meal and photo reminders: WorkManager jobs that notify only when nothing was logged (today / this month);
// and the daily encouragement, one kind message a day chosen from the user's data (Encouragement).
package com.clement.nutrimo.data.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.clement.nutrimo.MainActivity
import com.clement.nutrimo.NutrimoApp
import com.clement.nutrimo.R
import com.clement.nutrimo.data.repository.ReminderSettings
import com.clement.nutrimo.data.repository.goalTargets
import com.clement.nutrimo.data.repository.loadDayRecords
import com.clement.nutrimo.domain.calc.Encouragement
import com.clement.nutrimo.domain.calc.EncouragementFacts
import com.clement.nutrimo.domain.calc.WeightPoint
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "reminders"
private const val WEIGH_WORK = "weigh_reminder"
private const val MEAL_WORK = "meal_reminder"
private const val WEIGH_NOTIFICATION = 1
private const val MEAL_NOTIFICATION = 2
private const val PHOTO_WORK = "photo_reminder"
private const val PHOTO_NOTIFICATION = 3
private const val ENCOURAGE_CHANNEL_ID = "encouragements"
private const val ENCOURAGE_WORK = "encouragement"
private const val ENCOURAGE_NOTIFICATION = 4
/** Days of history the encouragement looks at (streaks, last weigh-ins). */
private const val ENCOURAGE_HISTORY_DAYS = 60L

object Reminders {
    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Rappels", NotificationManager.IMPORTANCE_DEFAULT)
        val encouragements = NotificationChannel(ENCOURAGE_CHANNEL_ID, "Encouragements", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Un message bienveillant par jour" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(channel, encouragements))
    }

    /** Weekly, first run on the next [ReminderSettings.day] at the chosen time. */
    fun scheduleWeigh(context: Context, r: ReminderSettings) {
        if (!r.enabled) {
            WorkManager.getInstance(context).cancelUniqueWork(WEIGH_WORK)
            return
        }
        val now = LocalDateTime.now()
        var next = now.with(TemporalAdjusters.nextOrSame(r.day)).withHour(r.hour).withMinute(r.minute).withSecond(0)
        if (next.isBefore(now)) next = next.plusWeeks(1)
        enqueue(context, WEIGH_WORK, PeriodicWorkRequestBuilder<WeighReminderWorker>(7, TimeUnit.DAYS), Duration.between(now, next))
    }

    /** Daily at the chosen time. */
    fun scheduleMeal(context: Context, r: ReminderSettings) {
        if (!r.enabled) {
            WorkManager.getInstance(context).cancelUniqueWork(MEAL_WORK)
            return
        }
        val now = LocalDateTime.now()
        var next = now.withHour(r.hour).withMinute(r.minute).withSecond(0)
        if (next.isBefore(now)) next = next.plusDays(1)
        enqueue(context, MEAL_WORK, PeriodicWorkRequestBuilder<MealReminderWorker>(1, TimeUnit.DAYS), Duration.between(now, next))
    }

    /**
     * Every day at the chosen time; the worker only notifies on the 1st of the month (months differ in length, a daily
     * job is simpler than re-planning a one-time one). [replace] = false keeps an already planned job (app start).
     */
    fun schedulePhoto(context: Context, r: ReminderSettings, replace: Boolean = true) {
        if (!r.enabled) {
            WorkManager.getInstance(context).cancelUniqueWork(PHOTO_WORK)
            return
        }
        val now = LocalDateTime.now()
        var next = now.withHour(r.hour).withMinute(r.minute).withSecond(0)
        if (next.isBefore(now)) next = next.plusDays(1)
        val policy = if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP
        enqueue(context, PHOTO_WORK, PeriodicWorkRequestBuilder<PhotoReminderWorker>(1, TimeUnit.DAYS), Duration.between(now, next), policy)
    }

    /** Daily at the chosen time; [replace] = false keeps an already planned job (app start). */
    fun scheduleEncouragement(context: Context, r: ReminderSettings, replace: Boolean = true) {
        if (!r.enabled) {
            WorkManager.getInstance(context).cancelUniqueWork(ENCOURAGE_WORK)
            return
        }
        val now = LocalDateTime.now()
        var next = now.withHour(r.hour).withMinute(r.minute).withSecond(0)
        if (next.isBefore(now)) next = next.plusDays(1)
        val policy = if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP
        enqueue(context, ENCOURAGE_WORK, PeriodicWorkRequestBuilder<EncouragementWorker>(1, TimeUnit.DAYS), Duration.between(now, next), policy)
    }

    private fun enqueue(
        context: Context, name: String, builder: PeriodicWorkRequest.Builder, delay: Duration,
        policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE,
    ) {
        val request = builder.setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(name, policy, request)
    }

    /** POST_NOTIFICATIONS only exists (and is only required) from Android 13. */
    fun canNotify(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun notify(context: Context, id: Int, title: String, text: String, channel: String = CHANNEL_ID) {
        if (!canNotify(context)) return
        val intent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            // Long messages expand instead of being cut after one line.
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}

private fun Context.container() = (applicationContext as NutrimoApp).container

class WeighReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (applicationContext.container().body.weightOn(LocalDate.now()) == null) {
            Reminders.notify(applicationContext, WEIGH_NOTIFICATION, "C'est l'heure de la pesée", "Notez votre poids du jour dans Nutrimo.")
        }
        return Result.success()
    }
}

class MealReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!applicationContext.container().diary.hasEntries(LocalDate.now())) {
            Reminders.notify(applicationContext, MEAL_NOTIFICATION, "Journal vide aujourd'hui", "Pensez à noter vos repas.")
        }
        return Result.success()
    }
}

class PhotoReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        if (today.dayOfMonth != 1) return Result.success()
        if (applicationContext.container().photos.latestDate()?.isBefore(today.withDayOfMonth(1)) != false) {
            Reminders.notify(applicationContext, PHOTO_NOTIFICATION, "Photos du mois", "Prenez vos photos de progression pour suivre l'évolution.")
        }
        return Result.success()
    }
}

class EncouragementWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container()
        val today = LocalDate.now()
        val goal = c.body.activeGoal.first()
        val facts = EncouragementFacts(
            today = today,
            records = c.loadDayRecords(today.minusDays(ENCOURAGE_HISTORY_DAYS), today),
            targets = c.settings.current().goalTargets(c.body.profile.first()),
            weights = c.body.weightHistory.first().map { WeightPoint(it.date, it.kg) },
            goalStart = goal?.startDate,
            goalStartKg = goal?.startKg,
            heightCm = c.body.profile.first()?.heightCm,
        )
        val nudge = Encouragement.pick(facts)
        Reminders.notify(applicationContext, ENCOURAGE_NOTIFICATION, nudge.title, nudge.text, ENCOURAGE_CHANNEL_ID)
        return Result.success()
    }
}
