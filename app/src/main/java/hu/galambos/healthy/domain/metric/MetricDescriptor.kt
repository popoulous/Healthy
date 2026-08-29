package hu.galambos.healthy.domain.metric

import androidx.annotation.StringRes
import androidx.health.connect.client.records.Record
import kotlin.reflect.KClass

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
     * The headline number, read from the newest raw record. Aggregates carry
     * neither a timestamp nor a source app, which the design shows on every
     * card, so the newest record is always fetched as well — one record is
     * cheap.
     */
    val latestValue: (Record) -> Double?,
    val trend: TrendStrategy,
)
