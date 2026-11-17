// App settings in DataStore: backend server, onboarding flag, reminders (weigh-in, meals, photos, daily encouragement), backup and macro split.
package com.clement.nutrimo.data.repository

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.clement.nutrimo.data.db.ProfileEntity
import com.clement.nutrimo.data.reminders.ReminderKind
import com.clement.nutrimo.domain.calc.DEFAULT_SPORT_GOAL_KCAL
import com.clement.nutrimo.domain.calc.DEFAULT_STEP_GOAL
import com.clement.nutrimo.domain.calc.DEFAULT_WATER_GOAL_ML
import com.clement.nutrimo.domain.calc.GoalTargets
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
    val reminders: Map<ReminderKind, ReminderSettings> = ReminderKind.entries.associateWith { it.defaults },
    val autoBackup: Boolean = false,
    /** Epoch millis of the last successful server backup, 0 if none. */
    val lastServerBackup: Long = 0,
    val macroSplit: MacroSplit = MacroSplit.BALANCED,
    val stepGoal: Int = DEFAULT_STEP_GOAL,
    /** Daily calories to burn through sport (manual activities + Health Connect workouts). */
    val sportGoalKcal: Int = DEFAULT_SPORT_GOAL_KCAL,
    /** Off by default: adds Health Connect steps (beyond the baseline) and non-walking workouts to the budget. */
    val healthInBudget: Boolean = false,
) {
    fun reminder(kind: ReminderKind): ReminderSettings = reminders[kind] ?: kind.defaults
}

/** The daily goals; the water goal lives in the profile. */
fun AppSettings.goalTargets(profile: ProfileEntity?) =
    GoalTargets(stepGoal, sportGoalKcal, profile?.waterGoalMl ?: DEFAULT_WATER_GOAL_ML)

class SettingsRepository(private val context: Context) {
    /** "weigh_enabled", "weigh_hour", "weigh_minute", "weigh_day" and the same for every [ReminderKind]. */
    private class ReminderKeys(kind: ReminderKind) {
        val enabled = booleanPreferencesKey("${kind.key}_enabled")
        val hour = intPreferencesKey("${kind.key}_hour")
        val minute = intPreferencesKey("${kind.key}_minute")
        val day = intPreferencesKey("${kind.key}_day")
    }

    private val reminderKeys = ReminderKind.entries.associateWith { ReminderKeys(it) }

    private object Keys {
        val serverUrl = stringPreferencesKey("server_url")
        val apiKey = stringPreferencesKey("api_key")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val autoBackup = booleanPreferencesKey("auto_backup")
        val lastServerBackup = longPreferencesKey("last_server_backup")
        val proteinPct = intPreferencesKey("protein_pct")
        val carbsPct = intPreferencesKey("carbs_pct")
        val fatPct = intPreferencesKey("fat_pct")
        val stepGoal = intPreferencesKey("step_goal")
        val sportGoal = intPreferencesKey("sport_goal_kcal")
        val healthInBudget = booleanPreferencesKey("steps_in_budget")
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

    suspend fun setActivityGoals(steps: Int, sportKcal: Int, inBudget: Boolean) = edit {
        it[Keys.stepGoal] = steps
        it[Keys.sportGoal] = sportKcal
        it[Keys.healthInBudget] = inBudget
    }

    suspend fun setAutoBackup(enabled: Boolean) = edit { it[Keys.autoBackup] = enabled }

    suspend fun setLastServerBackup(at: Long) = edit { it[Keys.lastServerBackup] = at }

    suspend fun setReminder(kind: ReminderKind, r: ReminderSettings) = edit {
        val k = reminderKeys.getValue(kind)
        it[k.enabled] = r.enabled
        it[k.hour] = r.hour
        it[k.minute] = r.minute
        it[k.day] = r.day.value
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            serverUrl = this[Keys.serverUrl] ?: d.serverUrl,
            apiKey = this[Keys.apiKey] ?: d.apiKey,
            onboardingDone = this[Keys.onboardingDone] ?: false,
            reminders = reminderKeys.mapValues { (kind, k) ->
                ReminderSettings(
                    enabled = this[k.enabled] ?: kind.defaults.enabled,
                    hour = this[k.hour] ?: kind.defaults.hour,
                    minute = this[k.minute] ?: kind.defaults.minute,
                    day = this[k.day]?.let(DayOfWeek::of) ?: kind.defaults.day,
                )
            },
            autoBackup = this[Keys.autoBackup] ?: false,
            lastServerBackup = this[Keys.lastServerBackup] ?: 0,
            stepGoal = this[Keys.stepGoal] ?: d.stepGoal,
            sportGoalKcal = this[Keys.sportGoal] ?: d.sportGoalKcal,
            healthInBudget = this[Keys.healthInBudget] ?: false,
            macroSplit = MacroSplit(
                this[Keys.proteinPct] ?: d.macroSplit.proteinPct,
                this[Keys.carbsPct] ?: d.macroSplit.carbsPct,
                this[Keys.fatPct] ?: d.macroSplit.fatPct,
            ),
        )
    }
}
