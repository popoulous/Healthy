package hu.galambos.healthy.domain.sleep

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A sleep score computed here, from the stages Health Connect holds.
 *
 * Mi Fitness shows a number and a percentile against an age group. Neither is
 * written to Health Connect, and Xiaomi's formula is not published — so this
 * does not try to reproduce it. Guessing at someone else's number and printing
 * it as if it were theirs would be worse than having none.
 *
 * What this does instead is arithmetic anyone can check: how close the night
 * was to a sensible length, how close each stage was to its reference share,
 * and how often it was interrupted. It is comparable with itself over time,
 * which is what a trend needs, and the UI says whose calculation it is.
 *
 * The reference shares are the ones clinical guidance commonly quotes and that
 * Mi Fitness also displays: REM 10–30%, light 20–60%, deep 20–40%.
 */
object SleepScore {

    private const val IDEAL_MIN_HOURS = 7.0
    private const val IDEAL_MAX_HOURS = 9.0

    private val REM_RANGE = 0.10..0.30
    private val LIGHT_RANGE = 0.20..0.60
    private val DEEP_RANGE = 0.20..0.40

    private const val DURATION_WEIGHT = 40.0
    private const val STAGE_WEIGHT = 20.0
    private const val AWAKENING_PENALTY = 3
    private const val MAX_AWAKENING_PENALTY = 15

    /**
     * Null when the source did not break the night into stages — a duration
     * alone cannot say whether the sleep was any good, and inventing a number
     * from it would be the exact dishonesty this whole thing avoids.
     */
    fun of(night: SleepNight): Int? {
        if (!night.hasStageDetail) return null

        val asleepMinutes = night.asleep.toMinutes().toDouble()
        if (asleepMinutes <= 0) return null
        val hours = asleepMinutes / 60.0

        val duration = DURATION_WEIGHT * durationFactor(hours)

        val byStage = night.byStage
        val share = { stage: SleepStage ->
            (byStage[stage]?.toMinutes()?.toDouble() ?: 0.0) / asleepMinutes
        }
        val stages = STAGE_WEIGHT * rangeFactor(share(SleepStage.Rem), REM_RANGE) +
            STAGE_WEIGHT * rangeFactor(share(SleepStage.Light), LIGHT_RANGE) +
            STAGE_WEIGHT * rangeFactor(share(SleepStage.Deep), DEEP_RANGE)

        val penalty = (night.awakenings - 1).coerceAtLeast(0) * AWAKENING_PENALTY
        val total = duration + stages - penalty.coerceAtMost(MAX_AWAKENING_PENALTY)

        return total.roundToInt().coerceIn(0, 100)
    }

    /** Full marks inside the ideal band, falling off by an hour either side. */
    private fun durationFactor(hours: Double): Double = when {
        hours in IDEAL_MIN_HOURS..IDEAL_MAX_HOURS -> 1.0
        hours < IDEAL_MIN_HOURS -> (1.0 - (IDEAL_MIN_HOURS - hours) / 3.0).coerceAtLeast(0.0)
        else -> (1.0 - (hours - IDEAL_MAX_HOURS) / 3.0).coerceAtLeast(0.0)
    }

    /**
     * Full marks inside the reference share, falling off in proportion to how
     * far outside it landed, measured against the width of the range itself.
     */
    private fun rangeFactor(value: Double, range: ClosedFloatingPointRange<Double>): Double {
        if (value in range) return 1.0
        val distance = if (value < range.start) range.start - value else value - range.endInclusive
        val width = range.endInclusive - range.start
        return (1.0 - abs(distance) / width).coerceAtLeast(0.0)
    }
}
