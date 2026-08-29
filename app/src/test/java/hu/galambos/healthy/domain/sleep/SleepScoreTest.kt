package hu.galambos.healthy.domain.sleep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class SleepScoreTest {

    private val start: Instant = Instant.parse("2026-08-28T22:30:00Z")

    /**
     * Builds a night from stage durations in minutes, laid end to end.
     */
    private fun night(vararg stages: Pair<SleepStage, Long>): SleepNight {
        var cursor = start
        val segments = stages.map { (stage, minutes) ->
            val end = cursor.plus(Duration.ofMinutes(minutes))
            SleepSegment(stage, cursor, end).also { cursor = end }
        }
        return SleepNight(
            start = start,
            end = cursor,
            sourcePackage = "com.xiaomi.wearable",
            segments = segments,
        )
    }

    /**
     * The whole point of the exercise: a source that writes only "asleep"
     * gets no score at all rather than a number invented from a duration.
     */
    @Test
    fun `a night without stage detail has no score`() {
        val plain = night(SleepStage.Sleeping to 480)

        assertNull(SleepScore.of(plain))
    }

    @Test
    fun `an ideal night scores near the top`() {
        // Eight hours, stages inside every reference range, one awakening.
        val good = night(
            SleepStage.Light to 230,
            SleepStage.Deep to 130,
            SleepStage.Rem to 120,
            SleepStage.Awake to 10,
        )

        val score = SleepScore.of(good)!!

        assertTrue("expected a high score, got $score", score >= 95)
    }

    @Test
    fun `a short night loses duration points but keeps stage points`() {
        val short = night(
            SleepStage.Light to 100,
            SleepStage.Deep to 55,
            SleepStage.Rem to 45,
        )

        val score = SleepScore.of(short)!!

        assertTrue("expected the short night to lose points, got $score", score in 60..85)
    }

    @Test
    fun `repeated waking costs points`() {
        val stages = arrayOf(
            SleepStage.Light to 230L,
            SleepStage.Deep to 130L,
            SleepStage.Rem to 120L,
        )
        val settled = night(*stages, SleepStage.Awake to 5L)
        val restless = night(
            *stages,
            SleepStage.Awake to 5L,
            SleepStage.Awake to 5L,
            SleepStage.Awake to 5L,
        )

        assertTrue(SleepScore.of(restless)!! < SleepScore.of(settled)!!)
    }

    @Test
    fun `the score never leaves the nought to hundred range`() {
        val terrible = night(
            SleepStage.Light to 20,
            SleepStage.Deep to 1,
            SleepStage.Rem to 1,
            SleepStage.Awake to 30,
            SleepStage.Awake to 30,
            SleepStage.Awake to 30,
            SleepStage.Awake to 30,
            SleepStage.Awake to 30,
            SleepStage.Awake to 30,
        )

        val score = SleepScore.of(terrible)!!

        assertTrue(score in 0..100)
    }

    @Test
    fun `time awake does not count as time asleep`() {
        val restless = night(
            SleepStage.Light to 200,
            SleepStage.Deep to 120,
            SleepStage.Rem to 100,
            SleepStage.Awake to 60,
        )

        assertEquals(Duration.ofMinutes(420), restless.asleep)
        assertEquals(Duration.ofMinutes(480), restless.total)
    }
}
