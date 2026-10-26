// Read-only access to Health Connect: steps and exercise sessions written by the phone, Google Fit, Mi Fitness,
// Samsung Health, Strava… Nutrimo never writes there and never counts steps itself.
package com.clement.nutrimo.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CancellationException
import java.time.Duration
import java.time.LocalDate
import java.time.Period

enum class HealthStatus { AVAILABLE, NEEDS_UPDATE, UNAVAILABLE }

private val STEPS = HealthPermission.getReadPermission(StepsRecord::class)

/** Asked together in one Health Connect screen; steps are the minimum, the rest is optional. */
val HEALTH_PERMISSIONS: Set<String> = setOf(
    STEPS,
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
)

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
        if (status() != HealthStatus.AVAILABLE) emptySet() else safely { client.permissionController.getGrantedPermissions() }.orEmpty()

    suspend fun canReadSteps(): Boolean = STEPS in granted()

    /** Steps of one day (deduplicated across sources by Health Connect), or null when unavailable. */
    suspend fun stepsOn(date: LocalDate): Long? = safely {
        client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), dayRange(date, date)))[StepsRecord.COUNT_TOTAL] ?: 0
    }

    /** Steps per day between [from] and [to] included; empty when unavailable. */
    suspend fun dailySteps(from: LocalDate, to: LocalDate): Map<LocalDate, Long> {
        if (!canReadSteps()) return emptyMap()
        val request = AggregateGroupByPeriodRequest(setOf(StepsRecord.COUNT_TOTAL), dayRange(from, to), Period.ofDays(1))
        return safely {
            client.aggregateGroupByPeriod(request).associate { it.startTime.toLocalDate() to (it.result[StepsRecord.COUNT_TOTAL] ?: 0) }
        }.orEmpty()
    }

    /** Workouts of the day, each with the active calories recorded during it (when any). */
    suspend fun sessionsOn(date: LocalDate): List<HealthSession> {
        val records = safely { client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, dayRange(date, date))).records }
        return records.orEmpty().map { s ->
            val kcal = safely {
                client.aggregate(
                    AggregateRequest(setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL), TimeRangeFilter.between(s.startTime, s.endTime)),
                )[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
            }
            HealthSession(s.title, s.exerciseType, Duration.between(s.startTime, s.endTime).toMinutes().toInt(), kcal?.takeIf { it > 0 })
        }
    }

    private fun dayRange(from: LocalDate, to: LocalDate) =
        TimeRangeFilter.between(from.atStartOfDay(), to.plusDays(1).atStartOfDay())
}

/** Health Connect can throw (permission revoked, provider killed, rate limit): treat it as "no data". */
private suspend fun <T> safely(block: suspend () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}
