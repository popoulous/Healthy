package hu.galambos.healthy.data.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import hu.galambos.healthy.domain.sleep.SleepNight
import hu.galambos.healthy.domain.sleep.SleepSegment
import hu.galambos.healthy.domain.sleep.SleepStage
import hu.galambos.healthy.domain.sleep.SleepVitals
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * The most recent night, with its stages and the vitals recorded during it.
 *
 * The vitals are read over the night's own window rather than the day's: an
 * average heart rate across a day says nothing about how the night went, and
 * the whole point of the screen is the night.
 */
internal class SleepReader(private val client: HealthConnectClient) {

    suspend fun readLatestNight(withinDays: Long = 7): SleepNight? {
        val sessions = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.after(
                    Instant.now().minus(withinDays, ChronoUnit.DAYS),
                ),
                ascendingOrder = false,
                pageSize = 1,
            ),
        )
        val session = sessions.records.firstOrNull() ?: return null

        return SleepNight(
            start = session.startTime,
            end = session.endTime,
            sourcePackage = session.metadata.dataOrigin.packageName,
            segments = session.stages.map { stage ->
                SleepSegment(stage.stage.toSleepStage(), stage.startTime, stage.endTime)
            },
            vitals = readVitals(session.startTime, session.endTime),
        )
    }

    private suspend fun readVitals(start: Instant, end: Instant): SleepVitals {
        val filter = TimeRangeFilter.between(start, end)

        val heartRate = runCatching {
            client.aggregate(AggregateRequest(setOf(HeartRateRecord.BPM_AVG), filter))[
                HeartRateRecord.BPM_AVG,
            ]?.toDouble()
        }.getOrNull()

        // Neither of these has an aggregate, so they are averaged here. A
        // night's worth of occasional readings is a small enough list to hold.
        val oxygen = runCatching {
            client.readRecords(
                ReadRecordsRequest(OxygenSaturationRecord::class, filter),
            ).records.map { it.percentage.value }.averageOrNull()
        }.getOrNull()

        val respiratory = runCatching {
            client.readRecords(
                ReadRecordsRequest(RespiratoryRateRecord::class, filter),
            ).records.map { it.rate }.averageOrNull()
        }.getOrNull()

        return SleepVitals(heartRate, oxygen, respiratory)
    }
}

private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

private fun Int.toSleepStage(): SleepStage = when (this) {
    SleepSessionRecord.STAGE_TYPE_AWAKE -> SleepStage.Awake
    SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> SleepStage.AwakeInBed
    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> SleepStage.OutOfBed
    SleepSessionRecord.STAGE_TYPE_SLEEPING -> SleepStage.Sleeping
    SleepSessionRecord.STAGE_TYPE_LIGHT -> SleepStage.Light
    SleepSessionRecord.STAGE_TYPE_DEEP -> SleepStage.Deep
    SleepSessionRecord.STAGE_TYPE_REM -> SleepStage.Rem
    else -> SleepStage.Unknown
}
