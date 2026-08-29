package hu.galambos.healthy.domain.scale

import java.time.LocalDateTime

/**
 * One broadcast from the scale.
 *
 * The scale sends continuously while someone is on it, so most of what arrives
 * is a weight still settling. Only a reading that is both [stabilised] and
 * carries an [impedanceOhms] is a finished measurement worth keeping.
 */
data class ScaleReading(
    val measuredAt: LocalDateTime,
    val weightKg: Double,
    /**
     * Null until the scale has settled enough to report it — and it never
     * arrives at all through socks. Body composition cannot be computed
     * without it, which is why weight and composition can disagree about
     * whether a measurement happened.
     */
    val impedanceOhms: Int?,
    val stabilised: Boolean,
    /** The scale reporting that the load is gone: the person stepped off. */
    val loadRemoved: Boolean,
) {
    val isComplete: Boolean get() = stabilised && impedanceOhms != null
}

/**
 * Decodes the Mi Body Composition Scale 2 advertisement.
 *
 * The scale broadcasts its measurement in plain sight — service data under the
 * standard Weight Scale UUID `0x181B` — with no pairing and no handshake. The
 * layout below is the community-documented one; body composition is absent from
 * it because the scale never computes any: it sends weight and a raw impedance,
 * and every app that shows a body fat percentage worked it out itself.
 *
 * ```
 * byte  0-1   flags, little endian
 * byte  2-3   year
 * byte  4     month        byte 5  day
 * byte  6     hour         byte 7  minute      byte 8  second
 * byte  9-10  impedance, little endian
 * byte 11-12  weight, little endian
 * ```
 */
object ScaleAdvertisement {

    const val PAYLOAD_BYTES = 13

    private const val FLAG_LBS = 1 shl 7
    private const val FLAG_LOAD_REMOVED = 1 shl 8
    private const val FLAG_CATTY = 1 shl 9
    private const val FLAG_WEIGHT_STABILISED = 1 shl 10

    private const val RAW_PER_KG = 200.0
    private const val RAW_PER_LB = 100.0
    private const val RAW_PER_CATTY = 100.0
    private const val KG_PER_LB = 0.45359237
    private const val KG_PER_CATTY = 0.5

    /**
     * Whether an impedance arrived is decided by the value, not by a flag.
     *
     * The community documentation names bit 14 as "impedance stabilised", and
     * on the actual scale that bit stays clear while a perfectly good
     * impedance sits in bytes 9 and 10 — verified against a real weigh-in:
     * `02A6EA07081D0B3915A3011A4A` carries 419 ohms with bit 14 unset. Gating
     * on the documented flag meant no measurement was ever recorded.
     *
     * The band is the gate instead. Zero is what arrives mid-measurement and
     * through socks; a human body between two bare feet lands inside this
     * range, and anything outside it is the scale still deciding.
     */
    private val PLAUSIBLE_IMPEDANCE = 100..3000

    fun parse(payload: ByteArray): ScaleReading? {
        if (payload.size < PAYLOAD_BYTES) return null

        val flags = payload.uint16(0)
        val rawWeight = payload.uint16(11)
        val rawImpedance = payload.uint16(9)

        val weightKg = when {
            flags and FLAG_LBS != 0 -> rawWeight / RAW_PER_LB * KG_PER_LB
            flags and FLAG_CATTY != 0 -> rawWeight / RAW_PER_CATTY * KG_PER_CATTY
            else -> rawWeight / RAW_PER_KG
        }

        val measuredAt = runCatching {
            LocalDateTime.of(
                payload.uint16(2),
                payload[4].toInt() and 0xFF,
                payload[5].toInt() and 0xFF,
                payload[6].toInt() and 0xFF,
                payload[7].toInt() and 0xFF,
                payload[8].toInt() and 0xFF,
            )
        }.getOrNull() ?: return null

        return ScaleReading(
            measuredAt = measuredAt,
            weightKg = weightKg,
            impedanceOhms = rawImpedance.takeIf { it in PLAUSIBLE_IMPEDANCE },
            stabilised = flags and FLAG_WEIGHT_STABILISED != 0,
            loadRemoved = flags and FLAG_LOAD_REMOVED != 0,
        )
    }

    private fun ByteArray.uint16(index: Int): Int =
        (this[index].toInt() and 0xFF) or ((this[index + 1].toInt() and 0xFF) shl 8)
}
