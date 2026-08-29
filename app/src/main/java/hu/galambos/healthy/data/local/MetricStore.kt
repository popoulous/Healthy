package hu.galambos.healthy.data.local

import hu.galambos.healthy.domain.metric.MetricId
import hu.galambos.healthy.domain.summary.Bucket
import hu.galambos.healthy.domain.summary.DataPoint
import hu.galambos.healthy.domain.summary.LoadState
import hu.galambos.healthy.domain.summary.MetricSummary
import hu.galambos.healthy.domain.summary.TrendWindow
import hu.galambos.healthy.domain.summary.buildTrend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

/**
 * What the screen reads from.
 *
 * The dashboard never waits on Health Connect: it draws whatever the store
 * holds and redraws when a sync lands. That is the difference between opening
 * to a filled screen and opening to thirty-three pending queries.
 */
class MetricStore(
    private val database: HealthyDatabase,
    private val now: () -> Instant = Instant::now,
) {

    private val metrics = database.metricDao()

    fun observeSummaries(
        window: TrendWindow,
        today: LocalDate = LocalDate.now(),
    ): Flow<Map<MetricId, MetricSummary>> {
        val from = today.minusDays((window.days - 1).toLong()).toEpochDay()
        return combine(
            metrics.observeBuckets(from),
            metrics.observeLatest(),
        ) { buckets, latest ->
            val byMetric = buckets.groupBy { it.metricId }
            val latestByMetric = latest.associateBy { it.metricId }

            MetricId.entries.associateWith { id ->
                val trend = buildTrend(
                    window = window,
                    today = today,
                    values = byMetric[id.name]
                        .orEmpty()
                        .associate { LocalDate.ofEpochDay(it.epochDay) to it.value },
                )
                val newest = latestByMetric[id.name]?.let {
                    DataPoint(
                        value = it.value,
                        time = Instant.ofEpochMilli(it.timeEpochMillis),
                        sourcePackage = it.sourcePackage,
                    )
                }
                MetricSummary(
                    id = id,
                    // Empty is a real answer here, not a missing one: the sync
                    // has run and this metric had nothing to give.
                    state = if (newest == null) LoadState.Empty else LoadState.Loaded,
                    latest = newest,
                    trend = trend,
                )
            }
        }
    }

    suspend fun putDailyValues(id: MetricId, values: Map<LocalDate, Double>) {
        metrics.upsertBuckets(
            values.map { (date, value) -> DailyBucketEntity(id.name, date.toEpochDay(), value) },
        )
    }

    /**
     * Keeps whichever reading is actually the newest, rather than whichever
     * was written last.
     *
     * Two sources can hold the same metric — the scale weighs you now, Health
     * Connect remembers a weigh-in from this morning — and the sync happens to
     * run after the scale. Without this, today's weight was being overwritten
     * by an older one every time the app resumed.
     */
    suspend fun putLatest(id: MetricId, point: DataPoint?) {
        if (point == null) return
        val existing = metrics.latestFor(id.name)
        // Newest wins, except that a stored time in the future cannot be
        // newest: it is a reading an earlier version dated by when its record
        // would end rather than when it was taken, and holding on to it would
        // lock the metric to a row nothing can ever replace.
        val stored = existing?.timeEpochMillis?.takeIf { it <= now().toEpochMilli() }
        if (stored != null && stored > point.time.toEpochMilli()) return
        metrics.upsertLatest(
            listOf(
                LatestReadingEntity(
                    metricId = id.name,
                    value = point.value,
                    timeEpochMillis = point.time.toEpochMilli(),
                    sourcePackage = point.sourcePackage,
                ),
            ),
        )
    }

    /** Used when a deletion arrives and the affected day cannot be known. */
    suspend fun clearFrom(id: MetricId, from: LocalDate) {
        metrics.clearBucketsFrom(id.name, from.toEpochDay())
    }

    /** How far back the archive reaches, which is what a window may offer. */
    fun observeEarliestDay(window: TrendWindow): Flow<LocalDate?> =
        metrics.observeBuckets(0).map { buckets ->
            buckets.minOfOrNull { it.epochDay }?.let(LocalDate::ofEpochDay)
        }

    suspend fun syncState(): SyncStateEntity? = database.syncStateDao().get()

    suspend fun putSyncState(token: String?, at: Instant) {
        database.syncStateDao().put(SyncStateEntity(SyncStateEntity.SINGLE_ROW, token, at.toEpochMilli()))
    }
}
