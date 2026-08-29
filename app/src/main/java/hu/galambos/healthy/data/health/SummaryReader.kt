package hu.galambos.healthy.data.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import hu.galambos.healthy.domain.metric.MetricDescriptor
import hu.galambos.healthy.domain.metric.TrendStrategy
import hu.galambos.healthy.domain.summary.Bucket
import hu.galambos.healthy.domain.summary.DataPoint
import hu.galambos.healthy.domain.summary.TrendWindow
import hu.galambos.healthy.domain.summary.buildTrend
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId

/**
 * The reads themselves, kept apart from the repository's permission and
 * availability concerns.
 *
 * Two rules shape everything here. Aggregation happens on Health Connect's
 * side wherever it can, because a year of heart rate is hundreds of thousands
 * of raw samples behind a thousand-record page. And the local time zone
 * decides what "a day" means: the aggregate API takes local times, the raw
 * reads take instants, and mixing them up silently shifts every bucket.
 */
internal class SummaryReader(
    private val client: HealthConnectClient,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    /** The newest record, for the value, the timestamp and the source app. */
    suspend fun readLatest(descriptor: MetricDescriptor, window: TrendWindow): DataPoint? {
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = descriptor.recordType,
                timeRangeFilter = instantFilter(window),
                ascendingOrder = false,
                pageSize = 1,
            ),
        )
        val record = response.records.firstOrNull() ?: return null
        val reading = descriptor.reading(record) ?: return null
        return DataPoint(
            value = reading.value,
            time = reading.time,
            sourcePackage = record.metadata.dataOrigin.packageName,
        )
    }

    suspend fun readTrend(descriptor: MetricDescriptor, window: TrendWindow): List<Bucket> {
        val today = LocalDate.now(zone)
        val values = when (val strategy = descriptor.trend) {
            is TrendStrategy.Aggregate -> aggregateByDay(strategy, window)
            TrendStrategy.Samples -> sampleByDay(descriptor, window)
        }
        return buildTrend(window, today, values)
    }

    private suspend fun aggregateByDay(
        strategy: TrendStrategy.Aggregate,
        window: TrendWindow,
    ): Map<LocalDate, Double> {
        val buckets = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = strategy.metrics,
                timeRangeFilter = localFilter(window),
                timeRangeSlicer = Period.ofDays(1),
            ),
        )
        return buckets.mapNotNull { bucket ->
            strategy.value(bucket.result)?.let { bucket.startTime.toLocalDate() to it }
        }.toMap()
    }

    /**
     * For types Health Connect will not aggregate. Affordable only because
     * these are measured occasionally — a blood oxygen reading is a handful a
     * day, not a stream — and the window is bounded.
     */
    private suspend fun sampleByDay(
        descriptor: MetricDescriptor,
        window: TrendWindow,
    ): Map<LocalDate, Double> {
        val records = readAllRecords(descriptor, window)
        return records
            .mapNotNull { record ->
                descriptor.reading(record)?.let { reading ->
                    reading.time.atZone(zone).toLocalDate() to reading.value
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.average() }
    }

    private suspend fun readAllRecords(
        descriptor: MetricDescriptor,
        window: TrendWindow,
    ): List<Record> {
        val all = mutableListOf<Record>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = descriptor.recordType,
                    timeRangeFilter = instantFilter(window),
                    pageToken = pageToken,
                ),
            )
            all += response.records
            // Depending on the Health Connect version the final token comes
            // back as an empty string rather than null, and checking only for
            // null loops forever.
            pageToken = response.pageToken
        } while (!pageToken.isNullOrEmpty())
        return all
    }

    private fun instantFilter(window: TrendWindow): TimeRangeFilter {
        val start = LocalDate.now(zone)
            .minusDays((window.days - 1).toLong())
            .atStartOfDay(zone)
            .toInstant()
        return TimeRangeFilter.after(start)
    }

    private fun localFilter(window: TrendWindow): TimeRangeFilter {
        val start = LocalDate.now(zone)
            .minusDays((window.days - 1).toLong())
            .atStartOfDay()
        val end = LocalDateTime.now(zone).plusSeconds(1)
        return TimeRangeFilter.between(start, end)
    }
}
