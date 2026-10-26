// Physical activities: logged per day, kcal estimated from the latest weigh-in, added to the day's budget.
package com.clement.nutrimo.data.repository

import com.clement.nutrimo.data.db.ActivityEntity
import com.clement.nutrimo.data.db.DayBurn
import com.clement.nutrimo.data.db.NutrimoDatabase
import com.clement.nutrimo.domain.calc.Activities
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Used when no weigh-in exists yet (should not happen after onboarding). */
private const val FALLBACK_WEIGHT_KG = 70.0

class ActivityRepository(db: NutrimoDatabase) {
    private val dao = db.activityDao()
    private val weights = db.weightDao()

    fun day(date: LocalDate): Flow<List<ActivityEntity>> = dao.observeDay(date)

    fun burned(from: LocalDate, to: LocalDate): Flow<List<DayBurn>> = dao.observeBurned(from, to)

    /** [manualKcal] is used for types without a MET value ("Autre"); otherwise kcal come from [minutes]. */
    suspend fun add(date: LocalDate, typeKey: String, minutes: Int?, manualKcal: Double?) {
        val met = Activities.byKey(typeKey).met
        val kcal = if (met != null && minutes != null) {
            Activities.burned(met, weights.latest()?.kg ?: FALLBACK_WEIGHT_KG, minutes)
        } else {
            manualKcal ?: return
        }
        dao.upsert(ActivityEntity(date = date, type = typeKey, minutes = minutes, kcal = kcal))
    }

    suspend fun delete(id: String) = dao.delete(id)
}
