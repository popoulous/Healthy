package hu.galambos.healthy.domain.summary

import hu.galambos.healthy.domain.metric.Headline
import hu.galambos.healthy.domain.metric.MetricDescriptor
import hu.galambos.healthy.domain.metric.MetricId
import java.time.Instant
import java.time.LocalDate

/** Everything one card needs to draw itself. */
data class MetricSummary(
    val id: MetricId,
    val state: LoadState,
    /** The newest reading, with the timestamp and source app the design shows. */
    val latest: DataPoint? = null,
    /** One entry per day in the window, oldest first. Days without data carry null. */
    val trend: List<Bucket> = emptyList(),
) {
    /** Today's bucket, which is the last one the window holds. */
    val today: Double? get() = trend.lastOrNull()?.value
}

data class DataPoint(
    val value: Double,
    val time: Instant,
    /** The package that wrote the record. Resolving it to an app name is the UI's job. */
    val sourcePackage: String,
)

data class Bucket(val date: LocalDate, val value: Double?)

/**
 * The number the card leads with, per what the metric means.
 *
 * A day with no data yet falls back to the newest reading rather than showing
 * nothing: a card that has a value from this morning should say so, not go
 * blank because today's bucket has not been written.
 */
fun MetricSummary.headlineValue(descriptor: MetricDescriptor): Double? =
    when (descriptor.headline) {
        Headline.DailyTotal -> today ?: latest?.value
        Headline.Latest -> latest?.value
    }

/**
 * "Not granted" and "granted but empty" are deliberately different states.
 * They say different things to the user — one is a permission to give, the
 * other is a source that never wrote the data — and collapsing them is the
 * easiest mistake this app could make.
 */
sealed interface LoadState {
    data object Loading : LoadState
    data object NotGranted : LoadState
    data object Empty : LoadState
    data object Loaded : LoadState
    data class Failed(val reason: FailureReason) : LoadState
}

enum class FailureReason {
    /** Health Connect rate-limited the read; backing off and retrying is the answer. */
    RateLimited,

    /** The data is older than this app is allowed to see without the history permission. */
    BeyondHistoryLimit,
    Unknown,
}
