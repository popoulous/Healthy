package hu.galambos.healthy.domain.metric

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The registry drives the permission set, the reads and the cards at once, so
 * a duplicate or a missing accent is not a cosmetic slip — it silently drops
 * a metric from one of the three.
 */
class MetricRegistryTest {

    @Test
    fun `every metric appears once`() {
        val ids = MetricRegistry.all.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every metric maps to a distinct record type`() {
        val types = MetricRegistry.all.map { it.recordType }

        assertEquals(types.size, types.toSet().size)
    }

    @Test
    fun `lookup by id returns the same descriptor the list holds`() {
        MetricRegistry.all.forEach { descriptor ->
            assertEquals(descriptor, MetricRegistry[descriptor.id])
        }
    }

    @Test
    fun `the headline metrics are part of the registry`() {
        assertTrue(MetricRegistry.all.containsAll(MetricRegistry.headline))
    }

    @Test
    fun `blood oxygen is read as samples because Health Connect will not aggregate it`() {
        val oxygen = MetricRegistry[MetricId.OxygenSaturation]

        assertEquals(TrendStrategy.Samples, oxygen.trend)
    }

    @Test
    fun `steps are aggregated rather than read raw`() {
        val steps = MetricRegistry[MetricId.Steps]

        assertTrue(steps.trend is TrendStrategy.Aggregate)
    }
}
