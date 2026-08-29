package hu.galambos.healthy.domain.metric

import androidx.annotation.StringRes
import androidx.health.connect.client.records.Record
import java.time.Instant
import kotlin.reflect.KClass

/** A single reading pulled out of a record: what it said, and when. */
data class Reading(val value: Double, val time: Instant)

/**
 * One row per metric, and everything else follows from it: the permission the
 * app asks for, how the data is read, and the card that shows it. Adding a
 * metric should be adding a row here and nothing else.
 *
 * This is domain code that names Health Connect types on purpose. Health
 * Connect's record classes *are* this app's vocabulary — inventing a parallel
 * one would be ceremony, and the fake repository stays testable regardless
 * because it never calls the SDK.
 */
data class MetricDescriptor(
    val id: MetricId,
    val recordType: KClass<out Record>,
    val category: MetricCategory,
    @param:StringRes val titleRes: Int,
    val unit: MetricUnit,
    val accent: MetricAccent = MetricAccent.Neutral,
    /**
     * Pulls the reading out of a raw record. Aggregates carry neither a
     * timestamp nor a source app, both of which the design shows on every
     * card, so the newest record is always fetched as well — one record is
     * cheap.
     *
     * Reading the time here rather than centrally is not an accident: Health
     * Connect keeps the interfaces that would tell a record's time apart
     * (instantaneous versus interval) internal to the library, and the row
     * that names the record type is the one place that already knows which
     * it is.
     */
    val reading: (Record) -> Reading?,
    val trend: TrendStrategy,
)
