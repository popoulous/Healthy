package hu.galambos.healthy.domain.scale

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * The scale is not here, so the radio cannot be tested — but the bytes it sends
 * are documented, and everything that happens after they arrive is arithmetic.
 * These are the cases that decide whether a measurement is believed.
 */
class ScaleAdvertisementTest {

    private fun payload(
        flags: Int,
        impedance: Int = 0,
        rawWeight: Int = 0,
        year: Int = 2026,
        month: Int = 8,
        day: Int = 29,
        hour: Int = 7,
        minute: Int = 45,
        second: Int = 12,
    ): ByteArray = byteArrayOf(
        (flags and 0xFF).toByte(),
        ((flags shr 8) and 0xFF).toByte(),
        (year and 0xFF).toByte(),
        ((year shr 8) and 0xFF).toByte(),
        month.toByte(),
        day.toByte(),
        hour.toByte(),
        minute.toByte(),
        second.toByte(),
        (impedance and 0xFF).toByte(),
        ((impedance shr 8) and 0xFF).toByte(),
        (rawWeight and 0xFF).toByte(),
        ((rawWeight shr 8) and 0xFF).toByte(),
    )

    private val stabilised = 1 shl 10
    private val lbs = 1 shl 7
    private val catty = 1 shl 9
    private val loadRemoved = 1 shl 8

    @Test
    fun `kilograms come in units of five grams`() {
        // 79.2 kg = 15840 raw
        val reading = ScaleAdvertisement.parse(payload(stabilised, rawWeight = 15840))!!

        assertEquals(79.2, reading.weightKg, 0.001)
    }

    @Test
    fun `pounds are converted, not shown as kilograms`() {
        // 174.6 lb = 17460 raw, which is 79.197 kg.
        val reading = ScaleAdvertisement.parse(payload(stabilised or lbs, rawWeight = 17460))!!

        assertEquals(79.19, reading.weightKg, 0.01)
    }

    @Test
    fun `catty is half a kilogram`() {
        // 158.4 catty = 15840 raw = 79.2 kg
        val reading = ScaleAdvertisement.parse(payload(stabilised or catty, rawWeight = 15840))!!

        assertEquals(79.2, reading.weightKg, 0.001)
    }

    @Test
    fun `the measurement time is read from the advertisement`() {
        val reading = ScaleAdvertisement.parse(payload(stabilised, rawWeight = 15840))!!

        assertEquals(LocalDateTime.of(2026, 8, 29, 7, 45, 12), reading.measuredAt)
    }

    /**
     * The scale broadcasts throughout a weigh-in. Everything before it settles
     * is a number on its way somewhere, and storing it would fill the history
     * with the moment someone put one foot on.
     */
    @Test
    fun `an unstabilised reading is not a measurement`() {
        val reading = ScaleAdvertisement.parse(payload(flags = 0, rawWeight = 9000))!!

        assertFalse(reading.stabilised)
        assertFalse(reading.isComplete)
    }

    /**
     * Standing on the scale in socks gives a weight and no impedance at all.
     * That is a valid weigh-in with no body composition, not a broken one.
     */
    @Test
    fun `zero impedance is not a body`() {
        val reading = ScaleAdvertisement.parse(
            payload(stabilised, impedance = 0, rawWeight = 15840),
        )!!

        assertNull(reading.impedanceOhms)
        assertEquals(79.2, reading.weightKg, 0.001)
    }

    @Test
    fun `an implausible impedance is not a body either`() {
        val reading = ScaleAdvertisement.parse(
            payload(stabilised, impedance = 9000, rawWeight = 15840),
        )!!

        assertNull(reading.impedanceOhms)
    }

    @Test
    fun `a settled measurement carries both weight and impedance`() {
        val reading = ScaleAdvertisement.parse(
            payload(stabilised, impedance = 512, rawWeight = 15840),
        )!!

        assertTrue(reading.isComplete)
        assertEquals(512, reading.impedanceOhms)
        assertEquals(79.2, reading.weightKg, 0.001)
    }

    /**
     * Captured from the actual scale. The documentation says bit 14 marks the
     * impedance as ready; this advertisement carries 419 ohms with that bit
     * clear, which is why the app recorded nothing until the flag stopped
     * being the gate. Kept verbatim so the layout cannot drift back.
     */
    @Test
    fun `a real advertisement from the scale decodes completely`() {
        val captured = "02A6EA07081D0B3915A3011A4A".hexToBytes()

        val reading = ScaleAdvertisement.parse(captured)!!

        assertEquals(LocalDateTime.of(2026, 8, 29, 11, 57, 21), reading.measuredAt)
        assertEquals(94.85, reading.weightKg, 0.001)
        assertEquals(419, reading.impedanceOhms)
        assertTrue(reading.stabilised)
        assertFalse(reading.loadRemoved)
        assertTrue(reading.isComplete)
    }

    private fun String.hexToBytes(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun `stepping off is reported and is not a measurement`() {
        val reading = ScaleAdvertisement.parse(payload(loadRemoved, rawWeight = 0))!!

        assertTrue(reading.loadRemoved)
        assertFalse(reading.isComplete)
    }

    @Test
    fun `a short payload is refused rather than half-read`() {
        assertNull(ScaleAdvertisement.parse(ByteArray(8)))
    }

    @Test
    fun `an impossible date is refused rather than crashing`() {
        assertNull(ScaleAdvertisement.parse(payload(stabilised, month = 13, rawWeight = 15840)))
    }
}
