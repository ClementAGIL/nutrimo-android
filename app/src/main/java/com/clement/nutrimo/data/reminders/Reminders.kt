// Reminder plumbing: notification channels, WorkManager scheduling of every ReminderKind, and posting a notification.
// What each reminder checks before notifying lives in ReminderWorkers.
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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.clement.nutrimo.MainActivity
import com.clement.nutrimo.R
import com.clement.nutrimo.data.repository.AppSettings
import com.clement.nutrimo.data.repository.ReminderSettings
import com.clement.nutrimo.data.repository.SettingsRepository
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object Reminders {
    fun createChannels(context: Context) {
        val reminders = NotificationChannel(REMINDERS_CHANNEL, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT)
        val encouragements = NotificationChannel(ENCOURAGEMENTS_CHANNEL, context.getString(R.string.channel_encouragements), NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = context.getString(R.string.channel_encouragements_hint) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(reminders, encouragements))
    }

    /**
     * Plans [kind] at its next run, or cancels it when disabled. [replace] = false keeps a job already planned (app
     * start), true re-plans it (the user changed the time).
     */
    fun schedule(context: Context, kind: ReminderKind, r: ReminderSettings, replace: Boolean = true) {
        val work = WorkManager.getInstance(context)
        if (!r.enabled) {
            work.cancelUniqueWork(kind.workName)
            return
        }
        val now = LocalDateTime.now()
        val request = PeriodicWorkRequest.Builder(kind.workerClass, if (kind.weekly) 7L else 1L, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, nextRun(now, r, kind.weekly)).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        val policy = if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP
        work.enqueueUniquePeriodicWork(kind.workName, policy, request)
    }

    /** At app start: every enabled reminder is planned (also after a restore or a reinstall), planned ones are kept. */
    fun scheduleAll(context: Context, settings: AppSettings) {
        ReminderKind.entries.forEach { schedule(context, it, settings.reminder(it), replace = false) }
    }

    /** POST_NOTIFICATIONS only exists (and is only required) from Android 13. */
    fun canNotify(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun notify(context: Context, kind: ReminderKind, title: String, text: String) {
        if (!canNotify(context)) return
        val intent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, kind.channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            // Long messages expand instead of being cut after one line.
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        // canNotify() checked above; the permission can still be revoked in between, which must not crash a worker.
        try {
            NotificationManagerCompat.from(context).notify(kind.notificationId, notification)
        } catch (_: SecurityException) {
        }
    }
}

private val ReminderKind.workerClass: Class<out ListenableWorker>
    get() = when (this) {
        ReminderKind.WEIGH -> WeighReminderWorker::class.java
        ReminderKind.MEAL -> MealReminderWorker::class.java
        ReminderKind.ENCOURAGEMENT -> EncouragementWorker::class.java
        ReminderKind.PHOTO -> PhotoReminderWorker::class.java
    }

/** Saves a reminder's settings and (re)plans it; owned by the container so it outlives the screen that changed it. */
class ReminderScheduler(private val context: Context, private val settings: SettingsRepository) {
    suspend fun update(kind: ReminderKind, r: ReminderSettings) {
        settings.setReminder(kind, r)
        Reminders.schedule(context, kind, r)
    }

    fun canNotify(): Boolean = Reminders.canNotify(context)
}
