package hu.galambos.healthy.domain.metric

import androidx.annotation.StringRes
import androidx.health.connect.client.records.Record
import java.time.Instant
import kotlin.reflect.KClass

/** A single reading pulled out of a record: what it said, and when. */
data class Reading(val value: Double, val time: Instant)

/**
 * A reading dated no later than now.
 *
 * An interval record is timed by when it ends, which is right for something
 * finished and wrong for something still running: today's calories are one
 * record covering the whole day, so it ends at midnight tonight, and a step
 * bucket ends at the close of the quarter hour it is still filling. Reported
 * straight, that puts a measurement in the future — the dashboard said 18:59
 * at 18:55, and tomorrow's date for today's calories.
 *
 * Clamping only moves a record that has not finished yet, and moves it to the
 * last moment it can honestly claim: now.
 */
fun Reading.notInTheFuture(now: Instant): Reading =
    if (time.isAfter(now)) copy(time = now) else this

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
    val headline: Headline = Headline.Latest,
)

/**
 * Which number the card leads with.
 *
 * For a heart rate the answer is the last reading — that is what a pulse is.
 * For steps, sleep or calories it is the day's total, and showing the last
 * record instead is simply wrong: the dashboard was reporting "10 steps"
 * because ten was the size of the most recent batch the watch wrote, and one
 * hour fifteen of sleep because that was the afternoon nap rather than the
 * night before it.
 */
enum class Headline {
    /** The newest record's own value. */
    Latest,

    /** Today's bucket — the total of everything the day holds. */
    DailyTotal,
}
