package hu.galambos.healthy.domain.summary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TrendWindowTest {

    private val today = LocalDate.of(2026, 8, 29)

    @Test
    fun `week covers seven days ending today`() {
        val dates = TrendWindow.Week.dates(today)

        assertEquals(7, dates.size)
        assertEquals(LocalDate.of(2026, 8, 23), dates.first())
        assertEquals(today, dates.last())
    }

    @Test
    fun `month covers thirty days ending today`() {
        val dates = TrendWindow.Month.dates(today)

        assertEquals(30, dates.size)
        assertEquals(LocalDate.of(2026, 7, 31), dates.first())
        assertEquals(today, dates.last())
    }

    @Test
    fun `dates run oldest first so a sparkline reads left to right`() {
        val dates = TrendWindow.Week.dates(today)

        assertEquals(dates.sorted(), dates)
    }

    /**
     * The point of the whole exercise: a day nobody recorded must survive as a
     * hole. Dropping it would slide later days left and draw a reading on the
     * wrong date.
     */
    @Test
    fun `days without data stay in the trend as nulls`() {
        val values = mapOf(
            LocalDate.of(2026, 8, 27) to 8000.0,
            LocalDate.of(2026, 8, 29) to 9000.0,
        )

        val trend = buildTrend(TrendWindow.Week, today, values)

        assertEquals(7, trend.size)
        assertEquals(8000.0, trend[4].value)
        assertNull(trend[5].value)
        assertEquals(9000.0, trend[6].value)
    }

    @Test
    fun `values outside the window are ignored`() {
        val values = mapOf(
            LocalDate.of(2026, 8, 1) to 1.0,
            today to 2.0,
        )

        val trend = buildTrend(TrendWindow.Week, today, values)

        assertEquals(1, trend.count { it.value != null })
        assertEquals(2.0, trend.last().value)
    }

    @Test
    fun `a window with no data at all is still a full row of days`() {
        val trend = buildTrend(TrendWindow.Month, today, emptyMap())

        assertEquals(30, trend.size)
        assertEquals(30, trend.count { it.value == null })
    }
}
