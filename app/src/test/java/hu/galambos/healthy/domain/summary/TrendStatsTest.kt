package hu.galambos.healthy.domain.summary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TrendStatsTest {

    private fun buckets(vararg values: Double?): List<Bucket> =
        values.mapIndexed { index, value ->
            Bucket(LocalDate.of(2026, 8, index + 1), value)
        }

    @Test
    fun `an empty window has no stats`() {
        assertNull(buckets(null, null, null).stats())
    }

    /**
     * The one that matters: a day the watch was on the charger must not be
     * averaged in as a zero, or a week off turns into a terrible week.
     */
    @Test
    fun `missing days are skipped rather than counted as zero`() {
        val stats = buckets(8000.0, null, 10000.0).stats()!!

        assertEquals(9000.0, stats.average, 0.001)
        assertEquals(2, stats.daysWithData)
        assertEquals(8000.0, stats.min, 0.001)
        assertEquals(10000.0, stats.max, 0.001)
    }

    @Test
    fun `latest is the newest day that has data, not the last day of the window`() {
        val stats = buckets(5.0, 9.0, null).stats()!!

        assertEquals(9.0, stats.latest)
    }

    @Test
    fun `the delta compares the latest reading with the window average`() {
        val stats = buckets(6.0, 8.0, 10.0).stats()!!

        assertEquals(8.0, stats.average, 0.001)
        assertEquals(2.0, stats.deltaFromAverage!!, 0.001)
    }

    @Test
    fun `a single day is its own min, max and average`() {
        val stats = buckets(null, 72.0).stats()!!

        assertEquals(72.0, stats.min, 0.001)
        assertEquals(72.0, stats.max, 0.001)
        assertEquals(72.0, stats.average, 0.001)
    }
}
