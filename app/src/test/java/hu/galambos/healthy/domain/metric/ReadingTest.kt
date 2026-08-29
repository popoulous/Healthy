package hu.galambos.healthy.domain.metric

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ReadingTest {

    private val now: Instant = Instant.parse("2026-08-29T16:55:00Z")

    @Test
    fun `a finished record keeps the time it finished`() {
        val reading = Reading(80.0, Instant.parse("2026-08-29T12:51:00Z"))
        assertEquals(reading, reading.notInTheFuture(now))
    }

    @Test
    fun `a record still running is dated now, not when it will end`() {
        // Today's calories: one record covering the whole day, so it ends at
        // midnight tonight and would otherwise be reported as tomorrow.
        val reading = Reading(1799.25, Instant.parse("2026-08-29T22:00:00Z"))
        assertEquals(Reading(1799.25, now), reading.notInTheFuture(now))
    }

    @Test
    fun `a record ending exactly now is not moved`() {
        val reading = Reading(17.0, now)
        assertEquals(reading, reading.notInTheFuture(now))
    }
}
