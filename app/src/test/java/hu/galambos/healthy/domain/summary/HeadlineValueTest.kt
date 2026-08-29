package hu.galambos.healthy.domain.summary

import hu.galambos.healthy.domain.metric.MetricId
import hu.galambos.healthy.domain.metric.MetricRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class HeadlineValueTest {

    private val today = LocalDate.of(2026, 8, 29)

    private fun summary(id: MetricId, latest: Double, vararg days: Double?) = MetricSummary(
        id = id,
        state = LoadState.Loaded,
        latest = DataPoint(latest, Instant.parse("2026-08-29T16:55:00Z"), "com.xiaomi.wearable"),
        trend = days.mapIndexed { index, value ->
            Bucket(today.minusDays((days.size - 1 - index).toLong()), value)
        },
    )

    @Test
    fun `sleep leads with the day's total, not the last nap`() {
        // 9h32 last night plus a 1h15 nap: the watch app says 10h47, and so
        // should this. The nap alone is what the newest record would give.
        val summary = summary(MetricId.Sleep, latest = 1.25, 8.0, 10.783)
        assertEquals(
            10.783,
            summary.headlineValue(MetricRegistry[MetricId.Sleep])!!,
            0.001,
        )
    }

    @Test
    fun `steps lead with the day's total, not the last batch written`() {
        val summary = summary(MetricId.Steps, latest = 10.0, 5000.0, 7420.0)
        assertEquals(7420.0, summary.headlineValue(MetricRegistry[MetricId.Steps])!!, 0.001)
    }

    @Test
    fun `heart rate leads with the newest reading`() {
        val summary = summary(MetricId.HeartRate, latest = 80.0, 61.0, 64.0)
        assertEquals(80.0, summary.headlineValue(MetricRegistry[MetricId.HeartRate])!!, 0.001)
    }

    @Test
    fun `a day with nothing yet falls back to the newest reading`() {
        val summary = summary(MetricId.Steps, latest = 10.0, 5000.0, null)
        assertEquals(10.0, summary.headlineValue(MetricRegistry[MetricId.Steps])!!, 0.001)
    }
}
