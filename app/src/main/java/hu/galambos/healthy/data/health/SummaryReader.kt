package hu.galambos.healthy.data.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import hu.galambos.healthy.domain.metric.MetricDescriptor
import hu.galambos.healthy.domain.metric.notInTheFuture
import hu.galambos.healthy.domain.metric.TrendStrategy
import hu.galambos.healthy.domain.summary.DataPoint
import java.time.Instant
import java.time.LocalDate
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
    private val now: () -> Instant = Instant::now,
) {

    /** The newest record, for the value, the timestamp and the source app. */
    suspend fun readLatest(descriptor: MetricDescriptor): DataPoint? {
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = descriptor.recordType,
                // Everything: the newest record may be older than any
                // window the dashboard happens to be showing.
                timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH),
                ascendingOrder = false,
                pageSize = 1,
            ),
        )
        val record = response.records.firstOrNull() ?: return null
        val reading = descriptor.reading(record)?.notInTheFuture(now()) ?: return null
        return DataPoint(
            value = reading.value,
            time = reading.time,
            sourcePackage = record.metadata.dataOrigin.packageName,
        )
    }

    /** One value per day that has data, from [from] to [to] inclusive. */
    suspend fun readDailyValues(
        descriptor: MetricDescriptor,
        from: LocalDate,
        to: LocalDate,
    ): Map<LocalDate, Double> = when (val strategy = descriptor.trend) {
        is TrendStrategy.Aggregate -> aggregateByDay(strategy, from, to)
        TrendStrategy.Samples -> sampleByDay(descriptor, from, to)
        TrendStrategy.SessionsByEnd -> sessionTotalsByEndDay(descriptor, from, to)
    }

    private suspend fun aggregateByDay(
        strategy: TrendStrategy.Aggregate,
        from: LocalDate,
        to: LocalDate,
    ): Map<LocalDate, Double> {
        val buckets = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = strategy.metrics,
                timeRangeFilter = TimeRangeFilter.between(
                    from.atStartOfDay(),
                    to.plusDays(1).atStartOfDay(),
                ),
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
     * day, not a stream — and the range is bounded.
     */
    private suspend fun sampleByDay(
        descriptor: MetricDescriptor,
        from: LocalDate,
        to: LocalDate,
    ): Map<LocalDate, Double> = readAllRecords(descriptor, from, to)
        .mapNotNull { record ->
            descriptor.reading(record)?.notInTheFuture(now())?.let { reading ->
                // Without the clamp a record still being filled would count
                // towards tomorrow, and tomorrow would have data before it
                // happened.
                reading.time.atZone(zone).toLocalDate() to reading.value
            }
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, values) -> values.average() }

    /**
     * Sessions totalled onto the day they ended.
     *
     * The read starts a day early on purpose: a night belonging to [from]
     * began the evening before, and asking only for [from] onwards would
     * either miss it or return it clipped, depending on how the range filter
     * treats a record that straddles the boundary.
     */
    private suspend fun sessionTotalsByEndDay(
        descriptor: MetricDescriptor,
        from: LocalDate,
        to: LocalDate,
    ): Map<LocalDate, Double> = readAllRecords(descriptor, from.minusDays(1), to)
        .mapNotNull { record -> descriptor.reading(record)?.notInTheFuture(now()) }
        .groupBy { it.time.atZone(zone).toLocalDate() }
        .filterKeys { it >= from && it <= to }
        .mapValues { (_, readings) -> readings.sumOf { it.value } }

    private suspend fun readAllRecords(
        descriptor: MetricDescriptor,
        from: LocalDate,
        to: LocalDate,
    ): List<Record> {
        val filter = TimeRangeFilter.between(
            from.atStartOfDay(zone).toInstant(),
            to.plusDays(1).atStartOfDay(zone).toInstant(),
        )
        val all = mutableListOf<Record>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = descriptor.recordType,
                    timeRangeFilter = filter,
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
}
