package hu.galambos.healthy.domain.metric

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult

/**
 * How a metric's daily trend is produced.
 *
 * Reading raw records over long ranges is not viable: heart rate alone writes
 * several samples a minute, and a year of it is hundreds of thousands of
 * records behind a thousand-record page size. Where Health Connect can
 * aggregate, it does the summing on its side.
 */
sealed interface TrendStrategy {

    /**
     * Daily buckets from the aggregate API. [value] pulls the one number the
     * card shows out of the bucket, which keeps the extraction type-safe at
     * the point where the metric is declared.
     */
    data class Aggregate(
        val metrics: Set<AggregateMetric<*>>,
        val value: (AggregationResult) -> Double?,
    ) : TrendStrategy

    /**
     * No aggregate exists for this record type — blood oxygen and blood
     * pressure among them. The raw records are read over a bounded window
     * instead, which is affordable because these are measured occasionally
     * rather than continuously.
     */
    data object Samples : TrendStrategy

    /**
     * Sessions that belong to the day they *end* on, added up.
     *
     * A night is the case this exists for. Health Connect's aggregate slices
     * by calendar day, so a night from 22:30 to 08:02 is reported as an hour
     * and a half yesterday and eight hours today, which is not how anyone
     * reads their own sleep — and not what the watch app shows either. Dating
     * a session by when it ended puts the whole night on the morning you woke
     * up, and an afternoon nap adds to that same day.
     */
    data object SessionsByEnd : TrendStrategy
}
