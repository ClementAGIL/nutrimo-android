// Read-only access to Health Connect: steps and exercise sessions written by the phone, Google Fit, Mi Fitness,
// Samsung Health, Strava… Nutrimo never writes there and never counts steps itself.
package com.clement.nutrimo.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.clement.nutrimo.data.nullOnError
import java.time.Duration
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import kotlin.reflect.KClass

enum class HealthStatus { AVAILABLE, NEEDS_UPDATE, UNAVAILABLE }

val STEPS_PERMISSION: String = HealthPermission.getReadPermission(StepsRecord::class)

/** With both, Health Connect's calories replace our step estimate in the budget. */
val KCAL_PERMISSIONS: Set<String> = setOf(
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
)

/** Asked together in one Health Connect screen; steps are the minimum, the rest is optional. */
val HEALTH_PERMISSIONS: Set<String> = setOf(STEPS_PERMISSION) + KCAL_PERMISSIONS

const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

/** A workout recorded by another app; [measuredKcal] is null when no active calories were recorded. */
data class HealthSession(val title: String?, val exerciseType: Int, val minutes: Int, val measuredKcal: Double?)

class HealthRepository(private val context: Context) {
    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    fun status(): HealthStatus = when (HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthStatus.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthStatus.NEEDS_UPDATE
        else -> HealthStatus.UNAVAILABLE
    }

    suspend fun granted(): Set<String> =
        if (status() != HealthStatus.AVAILABLE) emptySet() else nullOnError { client.permissionController.getGrantedPermissions() }.orEmpty()

    suspend fun canReadSteps(): Boolean = STEPS_PERMISSION in granted()

    /** Steps of one day (deduplicated across sources by Health Connect), or null when unavailable. */
    suspend fun stepsOn(date: LocalDate): Long? = nullOnError {
        client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), dayRange(date, date)))[StepsRecord.COUNT_TOTAL] ?: 0
    }

    /** Steps per day between [from] and [to] included; empty when unavailable. */
    suspend fun dailySteps(from: LocalDate, to: LocalDate): Map<LocalDate, Long> {
        if (!canReadSteps()) return emptyMap()
        val request = AggregateGroupByPeriodRequest(setOf(StepsRecord.COUNT_TOTAL), dayRange(from, to), Period.ofDays(1))
        return nullOnError {
            client.aggregateGroupByPeriod(request).associate { it.startTime.toLocalDate() to (it.result[StepsRecord.COUNT_TOTAL] ?: 0) }
        }.orEmpty()
    }

    /** Active calories of one day measured by the user's apps (0 when nothing recorded); null without [KCAL_PERMISSIONS]. */
    suspend fun activeKcalOn(date: LocalDate): Double? {
        if (!granted().containsAll(KCAL_PERMISSIONS)) return null
        val metric = ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
        return nullOnError { client.aggregate(AggregateRequest(setOf(metric), dayRange(date, date)))[metric]?.inKilocalories ?: 0.0 }
    }

    /** Active calories per day between [from] and [to] included (days with nothing recorded are absent); null without [KCAL_PERMISSIONS]. */
    suspend fun dailyActiveKcal(from: LocalDate, to: LocalDate): Map<LocalDate, Double>? {
        if (!granted().containsAll(KCAL_PERMISSIONS)) return null
        val metric = ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
        val request = AggregateGroupByPeriodRequest(setOf(metric), dayRange(from, to), Period.ofDays(1))
        return nullOnError {
            client.aggregateGroupByPeriod(request).mapNotNull { day ->
                day.result[metric]?.inKilocalories?.let { day.startTime.toLocalDate() to it }
            }.toMap()
        }
    }

    /** Workouts of the day, each with the active calories recorded during it (when any). */
    suspend fun sessionsOn(date: LocalDate): List<HealthSession> = sessionsBetween(date, date).map { it.second }

    /** Workouts between [from] and [to] included, dated by their start. Two reads in total, whatever the range. */
    suspend fun sessionsBetween(from: LocalDate, to: LocalDate): List<Pair<LocalDate, HealthSession>> {
        val range = dayRange(from, to)
        val sessions = readAll(ExerciseSessionRecord::class, range).orEmpty()
        val calories = if (sessions.isEmpty()) emptyList() else readAll(ActiveCaloriesBurnedRecord::class, range).orEmpty()
        return sessions.map { s ->
            // Same app only: a watch and its phone app often both record the workout's calories.
            val kcal = calories
                .filter { it.metadata.dataOrigin == s.metadata.dataOrigin && it.startTime < s.endTime && it.endTime > s.startTime }
                .sumOf { it.energy.inKilocalories }
            val minutes = Duration.between(s.startTime, s.endTime).toMinutes().toInt()
            s.startTime.atZone(ZoneId.systemDefault()).toLocalDate() to HealthSession(s.title, s.exerciseType, minutes, kcal.takeIf { it > 0 })
        }
    }

    /** Every record of [type] in [range], following the pages; null when unavailable. */
    private suspend fun <T : Record> readAll(type: KClass<T>, range: TimeRangeFilter): List<T>? = nullOnError {
        buildList {
            var token: String? = null
            do {
                val page = client.readRecords(ReadRecordsRequest(type, range, pageToken = token))
                addAll(page.records)
                token = page.pageToken
            } while (token != null)
        }
    }

    private fun dayRange(from: LocalDate, to: LocalDate) =
        TimeRangeFilter.between(from.atStartOfDay(), to.plusDays(1).atStartOfDay())
}
