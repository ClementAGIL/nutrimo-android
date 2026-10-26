// Backups on the user's PC (nutrimo-api /backups): manual push/restore and a daily automatic push.
package com.clement.nutrimo.data.sync

import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.clement.nutrimo.NutrimoApp
import com.clement.nutrimo.data.network.NutrimoApi
import com.clement.nutrimo.data.repository.BackupRepository
import com.clement.nutrimo.data.repository.BackupSummary
import com.clement.nutrimo.data.repository.SettingsRepository
import java.util.concurrent.TimeUnit

private const val AUTO_BACKUP_WORK = "auto_backup"

class ServerBackup(
    private val backup: BackupRepository,
    private val api: NutrimoApi,
    private val settings: SettingsRepository,
) {
    /** Sends the full export to the server; throws when no server is set or it cannot be reached. */
    suspend fun push() {
        val s = settings.current()
        check(s.serverUrl.isNotBlank()) { "Aucun serveur configuré (Plus → Serveur)" }
        api.uploadBackup(s.serverUrl, s.apiKey, Build.MODEL, backup.export())
        settings.setLastServerBackup(System.currentTimeMillis())
    }

    /** Imports the server's latest backup (merged into the current data). */
    suspend fun restoreLatest(): BackupSummary {
        val s = settings.current()
        check(s.serverUrl.isNotBlank()) { "Aucun serveur configuré (Plus → Serveur)" }
        val json = api.latestBackup(s.serverUrl, s.apiKey) ?: error("Aucune sauvegarde sur le serveur")
        return backup.import(json)
    }

    companion object {
        /** Daily, only on Wi-Fi (the server lives on the home network). */
        fun schedule(context: Context, enabled: Boolean) {
            val work = WorkManager.getInstance(context)
            if (!enabled) {
                work.cancelUniqueWork(AUTO_BACKUP_WORK)
                return
            }
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build())
                .build()
            work.enqueueUniquePeriodicWork(AUTO_BACKUP_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

/** Away from home the server is unreachable: retry later instead of failing for the day. */
class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        (applicationContext as NutrimoApp).container.serverBackup.push()
        Result.success()
    } catch (_: Exception) {
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
