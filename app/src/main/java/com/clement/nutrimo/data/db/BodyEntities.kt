// Entities about the user: profile, weigh-ins, body measurements, goals and water.
// Ids are UUID strings and rows carry updatedAt so a future backup/sync API can merge them.
@file:UseSerializers(LocalDateSerializer::class)

package com.clement.nutrimo.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.clement.nutrimo.domain.model.ActivityLevel
import com.clement.nutrimo.domain.model.GoalMode
import com.clement.nutrimo.domain.model.GoalStatus
import com.clement.nutrimo.domain.model.Sex
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()
fun now(): Long = System.currentTimeMillis()

/** Single row (id = 1). */
@Serializable
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val sex: Sex,
    val birthDate: LocalDate,
    val heightCm: Double,
    val activity: ActivityLevel,
    val waterGoalMl: Int = 2000,
    val updatedAt: Long = now(),
)

/** One weigh-in per day (unique date). */
@Serializable
@Entity(tableName = "weights", indices = [Index(value = ["date"], unique = true)])
data class WeightEntity(
    @PrimaryKey val id: String = newId(),
    val date: LocalDate,
    val kg: Double,
    val updatedAt: Long = now(),
)

/** Body measurements in cm, one row per day, every field optional. */
@Serializable
@Entity(tableName = "measurements", indices = [Index(value = ["date"], unique = true)])
data class MeasurementEntity(
    @PrimaryKey val id: String = newId(),
    val date: LocalDate,
    val waist: Double? = null,
    val hips: Double? = null,
    val chest: Double? = null,
    val arm: Double? = null,
    val thigh: Double? = null,
    val updatedAt: Long = now(),
)

@Serializable
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String = newId(),
    val startDate: LocalDate,
    val startKg: Double,
    val targetKg: Double,
    val mode: GoalMode,
    val paceKgPerWeek: Double,
    val endDate: LocalDate,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val closedDate: LocalDate? = null,
    val updatedAt: Long = now(),
)

@Serializable
@Entity(tableName = "water")
data class WaterEntity(
    @PrimaryKey val date: LocalDate,
    val ml: Int,
    val updatedAt: Long = now(),
)

/** A physical activity of a day; [kcal] is computed when added (MET × weight × time) or typed by the user. */
@Serializable
@Entity(tableName = "activities", indices = [Index("date")])
data class ActivityEntity(
    @PrimaryKey val id: String = newId(),
    val date: LocalDate,
    /** Key of Activities.TYPES. */
    val type: String,
    val minutes: Int? = null,
    val kcal: Double,
    val updatedAt: Long = now(),
)

/** kcal burned on one day (calorie history). */
data class DayBurn(val date: LocalDate, val kcal: Double)
