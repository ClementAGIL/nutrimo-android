// App settings in DataStore: backend server, onboarding flag, reminders, backup and macro split.
package com.clement.nutrimo.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.clement.nutrimo.domain.calc.MacroSplit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

private val Context.dataStore by preferencesDataStore("settings")

data class ReminderSettings(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    /** Only used by the weekly weigh-in reminder. */
    val day: DayOfWeek = DayOfWeek.MONDAY,
)

data class AppSettings(
    val serverUrl: String = "",
    val apiKey: String = "",
    val onboardingDone: Boolean = false,
    val weighReminder: ReminderSettings = ReminderSettings(false, 8, 0, DayOfWeek.MONDAY),
    val mealReminder: ReminderSettings = ReminderSettings(false, 20, 0),
    val autoBackup: Boolean = false,
    /** Epoch millis of the last successful server backup, 0 if none. */
    val lastServerBackup: Long = 0,
    val macroSplit: MacroSplit = MacroSplit.BALANCED,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val serverUrl = stringPreferencesKey("server_url")
        val apiKey = stringPreferencesKey("api_key")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val weighEnabled = booleanPreferencesKey("weigh_enabled")
        val weighHour = intPreferencesKey("weigh_hour")
        val weighMinute = intPreferencesKey("weigh_minute")
        val weighDay = intPreferencesKey("weigh_day")
        val mealEnabled = booleanPreferencesKey("meal_enabled")
        val mealHour = intPreferencesKey("meal_hour")
        val mealMinute = intPreferencesKey("meal_minute")
        val autoBackup = booleanPreferencesKey("auto_backup")
        val lastServerBackup = longPreferencesKey("last_server_backup")
        val proteinPct = intPreferencesKey("protein_pct")
        val carbsPct = intPreferencesKey("carbs_pct")
        val fatPct = intPreferencesKey("fat_pct")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setServer(url: String, apiKey: String) = edit {
        it[Keys.serverUrl] = url.trim()
        it[Keys.apiKey] = apiKey.trim()
    }

    suspend fun setOnboardingDone(done: Boolean = true) = edit { it[Keys.onboardingDone] = done }

    suspend fun setMacroSplit(split: MacroSplit) = edit {
        it[Keys.proteinPct] = split.proteinPct
        it[Keys.carbsPct] = split.carbsPct
        it[Keys.fatPct] = split.fatPct
    }

    suspend fun setAutoBackup(enabled: Boolean) = edit { it[Keys.autoBackup] = enabled }

    suspend fun setLastServerBackup(at: Long) = edit { it[Keys.lastServerBackup] = at }

    suspend fun setWeighReminder(r: ReminderSettings) = edit {
        it[Keys.weighEnabled] = r.enabled
        it[Keys.weighHour] = r.hour
        it[Keys.weighMinute] = r.minute
        it[Keys.weighDay] = r.day.value
    }

    suspend fun setMealReminder(r: ReminderSettings) = edit {
        it[Keys.mealEnabled] = r.enabled
        it[Keys.mealHour] = r.hour
        it[Keys.mealMinute] = r.minute
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            serverUrl = this[Keys.serverUrl] ?: d.serverUrl,
            apiKey = this[Keys.apiKey] ?: d.apiKey,
            onboardingDone = this[Keys.onboardingDone] ?: false,
            weighReminder = ReminderSettings(
                enabled = this[Keys.weighEnabled] ?: d.weighReminder.enabled,
                hour = this[Keys.weighHour] ?: d.weighReminder.hour,
                minute = this[Keys.weighMinute] ?: d.weighReminder.minute,
                day = DayOfWeek.of(this[Keys.weighDay] ?: d.weighReminder.day.value),
            ),
            mealReminder = ReminderSettings(
                enabled = this[Keys.mealEnabled] ?: d.mealReminder.enabled,
                hour = this[Keys.mealHour] ?: d.mealReminder.hour,
                minute = this[Keys.mealMinute] ?: d.mealReminder.minute,
            ),
            autoBackup = this[Keys.autoBackup] ?: false,
            lastServerBackup = this[Keys.lastServerBackup] ?: 0,
            macroSplit = MacroSplit(
                this[Keys.proteinPct] ?: d.macroSplit.proteinPct,
                this[Keys.carbsPct] ?: d.macroSplit.carbsPct,
                this[Keys.fatPct] ?: d.macroSplit.fatPct,
            ),
        )
    }
}
