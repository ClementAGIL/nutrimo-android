// Profile, weigh-ins, measurements and goals, plus the derived daily calorie target.
package com.clement.nutrimo.data.repository

import com.clement.nutrimo.data.db.GoalEntity
import com.clement.nutrimo.data.db.MeasurementEntity
import com.clement.nutrimo.data.db.NutrimoDatabase
import com.clement.nutrimo.data.db.ProfileEntity
import com.clement.nutrimo.data.db.WeightEntity
import com.clement.nutrimo.data.db.now
import com.clement.nutrimo.domain.calc.CalorieTarget
import com.clement.nutrimo.domain.calc.Energy
import com.clement.nutrimo.domain.model.GoalStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class BodyRepository(private val db: NutrimoDatabase) {
    private val profiles = db.profileDao()
    private val weights = db.weightDao()
    private val measurements = db.measurementDao()
    private val goals = db.goalDao()

    val profile: Flow<ProfileEntity?> = profiles.observe()
    val weightHistory: Flow<List<WeightEntity>> = weights.observeAll()
    val measurementHistory: Flow<List<MeasurementEntity>> = measurements.observeAll()
    val activeGoal: Flow<GoalEntity?> = goals.observeWithStatus(GoalStatus.ACTIVE)
    val allGoals: Flow<List<GoalEntity>> = goals.observeAll()

    /** Recomputed whenever the profile, the goal pace or the latest weight changes. */
    val calorieTarget: Flow<CalorieTarget?> = combine(profile, activeGoal, weightHistory) { p, goal, history ->
        val weight = history.lastOrNull()?.kg ?: goal?.startKg
        if (p == null || weight == null) null
        else Energy.target(p.sex, weight, p.heightCm, Energy.age(p.birthDate), p.activity, goal?.paceKgPerWeek ?: 0.0)
    }

    suspend fun saveProfile(profile: ProfileEntity) = profiles.upsert(profile.copy(updatedAt = now()))

    suspend fun weightOn(date: LocalDate): WeightEntity? = weights.byDate(date)

    suspend fun latestWeight(): WeightEntity? = weights.latest()

    /** One weigh-in per day: an existing entry for [date] is replaced (keeping its id). */
    suspend fun saveWeight(date: LocalDate, kg: Double) {
        val existing = weights.byDate(date)
        weights.upsert(existing?.copy(kg = kg, updatedAt = now()) ?: WeightEntity(date = date, kg = kg))
    }

    suspend fun deleteWeight(id: String) = weights.delete(id)

    suspend fun measurementOn(date: LocalDate): MeasurementEntity? = measurements.byDate(date)

    suspend fun saveMeasurement(m: MeasurementEntity) {
        val existing = measurements.byDate(m.date)
        measurements.upsert(m.copy(id = existing?.id ?: m.id, updatedAt = now()))
    }

    suspend fun deleteMeasurement(id: String) = measurements.delete(id)

    /** Starting a new goal archives the current one. */
    suspend fun startGoal(goal: GoalEntity) {
        goals.active()?.let { goals.upsert(it.copy(status = GoalStatus.ARCHIVED, closedDate = LocalDate.now(), updatedAt = now())) }
        goals.upsert(goal)
    }

    suspend fun updateGoal(goal: GoalEntity) = goals.upsert(goal.copy(updatedAt = now()))

    suspend fun closeActiveGoal(status: GoalStatus) {
        goals.active()?.let { goals.upsert(it.copy(status = status, closedDate = LocalDate.now(), updatedAt = now())) }
    }
}
