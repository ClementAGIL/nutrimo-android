// DAOs for the profile, weigh-ins, measurements, goals and water.
package com.clement.nutrimo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.clement.nutrimo.domain.model.GoalStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun get(): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weights ORDER BY date")
    fun observeAll(): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weights ORDER BY date")
    suspend fun all(): List<WeightEntity>

    @Query("SELECT * FROM weights WHERE date = :date")
    suspend fun byDate(date: LocalDate): WeightEntity?

    @Query("SELECT * FROM weights ORDER BY date DESC LIMIT 1")
    suspend fun latest(): WeightEntity?

    @Upsert
    suspend fun upsert(weight: WeightEntity)

    /** REPLACE also drops a row with the same date (unique index), e.g. when importing a backup. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(weights: List<WeightEntity>)

    @Query("DELETE FROM weights WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY date DESC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements ORDER BY date")
    suspend fun all(): List<MeasurementEntity>

    @Query("SELECT * FROM measurements WHERE date = :date")
    suspend fun byDate(date: LocalDate): MeasurementEntity?

    @Upsert
    suspend fun upsert(measurement: MeasurementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(measurements: List<MeasurementEntity>)

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE status = :status ORDER BY startDate DESC LIMIT 1")
    fun observeWithStatus(status: GoalStatus = GoalStatus.ACTIVE): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE status = 'ACTIVE' ORDER BY startDate DESC LIMIT 1")
    suspend fun active(): GoalEntity?

    @Query("SELECT * FROM goals ORDER BY startDate DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals")
    suspend fun all(): List<GoalEntity>

    @Upsert
    suspend fun upsert(goal: GoalEntity)

    @Upsert
    suspend fun upsertAll(goals: List<GoalEntity>)
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water WHERE date = :date")
    fun observe(date: LocalDate): Flow<WaterEntity?>

    @Query("SELECT * FROM water WHERE date BETWEEN :from AND :to")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<WaterEntity>>

    @Query("SELECT * FROM water")
    suspend fun all(): List<WaterEntity>

    @Upsert
    suspend fun upsert(water: WaterEntity)

    @Upsert
    suspend fun upsertAll(water: List<WaterEntity>)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE date = :date ORDER BY updatedAt")
    fun observeDay(date: LocalDate): Flow<List<ActivityEntity>>

    @Query("SELECT date, SUM(kcal) AS kcal FROM activities WHERE date BETWEEN :from AND :to GROUP BY date")
    fun observeBurned(from: LocalDate, to: LocalDate): Flow<List<DayBurn>>

    @Query("SELECT * FROM activities")
    suspend fun all(): List<ActivityEntity>

    @Upsert
    suspend fun upsert(activity: ActivityEntity)

    @Upsert
    suspend fun upsertAll(activities: List<ActivityEntity>)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun delete(id: String)
}
