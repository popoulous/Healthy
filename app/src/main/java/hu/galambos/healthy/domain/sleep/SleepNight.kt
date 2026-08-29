package hu.galambos.healthy.domain.sleep

import java.time.Duration
import java.time.Instant

/**
 * Health Connect's sleep stages. Vendors write different subsets — a watch may
 * record only "asleep" without ever breaking it into light, deep and REM —
 * which is why nothing downstream may assume the detailed ones are present.
 */
enum class SleepStage {
    Awake,
    AwakeInBed,
    OutOfBed,
    Sleeping,
    Light,
    Deep,
    Rem,
    Unknown,
    ;

    /** Whether this stage counts as being asleep rather than awake in bed. */
    val isAsleep: Boolean
        get() = this == Sleeping || this == Light || this == Deep || this == Rem
}

data class SleepSegment(val stage: SleepStage, val start: Instant, val end: Instant) {
    val duration: Duration get() = Duration.between(start, end)
}

/** Averages taken over the night's window rather than over the whole day. */
data class SleepVitals(
    val heartRateBpm: Double? = null,
    val oxygenPercent: Double? = null,
    val respiratoryRate: Double? = null,
)

data class SleepNight(
    val start: Instant,
    val end: Instant,
    val sourcePackage: String,
    val segments: List<SleepSegment>,
    val vitals: SleepVitals = SleepVitals(),
) {
    val total: Duration get() = Duration.between(start, end)

    /** Time in each stage, only for the stages this night actually carries. */
    val byStage: Map<SleepStage, Duration>
        get() = segments
            .groupBy { it.stage }
            .mapValues { (_, segments) ->
                segments.fold(Duration.ZERO) { acc, segment -> acc + segment.duration }
            }

    /** Total time actually asleep, which is not the same as time in bed. */
    val asleep: Duration
        get() = segments
            .filter { it.stage.isAsleep }
            .fold(Duration.ZERO) { acc, segment -> acc + segment.duration }

    /**
     * Whether the source broke the night into light, deep and REM. Without
     * that there is no hypnogram, no breakdown and no score — only a duration.
     */
    val hasStageDetail: Boolean
        get() = segments.any { it.stage == SleepStage.Light || it.stage == SleepStage.Deep || it.stage == SleepStage.Rem }

    /** Waking up mid-night, which the score counts against the night. */
    val awakenings: Int
        get() = segments.count { it.stage == SleepStage.Awake || it.stage == SleepStage.AwakeInBed }
}
