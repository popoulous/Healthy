package hu.galambos.healthy.data.fake

import hu.galambos.healthy.data.HealthRepository
import hu.galambos.healthy.domain.HealthConnectAvailability
import hu.galambos.healthy.domain.HistoryAccess
import hu.galambos.healthy.domain.metric.MetricDescriptor
import hu.galambos.healthy.domain.metric.MetricId
import hu.galambos.healthy.domain.metric.MetricUnit
import hu.galambos.healthy.domain.sleep.SleepNight
import hu.galambos.healthy.domain.sleep.SleepSegment
import hu.galambos.healthy.domain.sleep.SleepStage
import hu.galambos.healthy.domain.sleep.SleepVitals
import hu.galambos.healthy.domain.summary.Bucket
import hu.galambos.healthy.domain.summary.DataPoint
import hu.galambos.healthy.domain.summary.LoadState
import hu.galambos.healthy.domain.summary.MetricSummary
import hu.galambos.healthy.domain.summary.TrendWindow
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Plausible data with no phone attached.
 *
 * This ships in the app rather than living in the test source set, because
 * Compose previews need it too — and a preview that renders the real card
 * with real-shaped numbers is the only way to judge the design without
 * building to a device that happens to own a watch.
 */
class FakeHealthRepository(
    private val availability: HealthConnectAvailability = HealthConnectAvailability.Available,
    private val granted: Boolean = true,
    private val overrides: Map<MetricId, LoadState> = emptyMap(),
    private val today: LocalDate = LocalDate.now(),
) : HealthRepository {

    override fun availability(): HealthConnectAvailability = availability

    override val readPermissions: Set<String> = emptySet()

    override suspend fun permissionsToRequest(): Set<String> = emptySet()

    override suspend fun grantedPermissions(): Set<String> =
        if (granted) setOf("fake.permission.granted") else emptySet()

    override suspend fun historyAccess(): HistoryAccess =
        if (granted) HistoryAccess.Granted else HistoryAccess.NotGranted

    override suspend fun loadSummary(
        descriptor: MetricDescriptor,
        window: TrendWindow,
    ): MetricSummary = summaryOf(descriptor, window)

    /** The same data without suspending, so Compose previews can call it. */
    fun summaryOf(descriptor: MetricDescriptor, window: TrendWindow): MetricSummary {
        overrides[descriptor.id]?.let { state ->
            return MetricSummary(descriptor.id, state)
        }
        if (!granted) return MetricSummary(descriptor.id, LoadState.NotGranted)

        val trend = window.dates(today).mapIndexed { index, date ->
            Bucket(date, seriesValue(descriptor, index))
        }
        val latest = trend.lastOrNull { it.value != null }
        return MetricSummary(
            id = descriptor.id,
            state = LoadState.Loaded,
            latest = latest?.let {
                DataPoint(
                    value = it.value ?: 0.0,
                    time = Instant.now().minus(3, ChronoUnit.HOURS),
                    sourcePackage = sourceFor(descriptor.id),
                )
            },
            trend = trend,
        )
    }

    override suspend fun loadSleepNight(): SleepNight? = sleepNight()

    /** A night with the shape a real one has: cycles, not one block. */
    fun sleepNight(): SleepNight? {
        if (!granted) return null
        val start = today.minusDays(1).atTime(22, 30).atZone(java.time.ZoneId.systemDefault())
            .toInstant()
        val pattern = listOf(
            SleepStage.Light to 35L,
            SleepStage.Deep to 55L,
            SleepStage.Light to 40L,
            SleepStage.Rem to 25L,
            SleepStage.Light to 45L,
            SleepStage.Deep to 50L,
            SleepStage.Awake to 6L,
            SleepStage.Light to 40L,
            SleepStage.Rem to 35L,
            SleepStage.Light to 30L,
            SleepStage.Deep to 25L,
            SleepStage.Rem to 40L,
            SleepStage.Light to 46L,
        )
        var cursor = start
        val segments = pattern.map { (stage, minutes) ->
            val end = cursor.plus(java.time.Duration.ofMinutes(minutes))
            SleepSegment(stage, cursor, end).also { cursor = end }
        }
        return SleepNight(
            start = start,
            end = cursor,
            sourcePackage = "com.xiaomi.wearable",
            segments = segments,
            vitals = SleepVitals(heartRateBpm = 58.0, oxygenPercent = 97.0, respiratoryRate = 16.0),
        )
    }

    /**
     * Deterministic per metric and per day, so previews and screenshots do not
     * shuffle themselves between runs.
     */
    private fun seriesValue(descriptor: MetricDescriptor, dayIndex: Int): Double? {
        val random = Random(descriptor.id.ordinal * 1000 + dayIndex)
        // A missing day now and then, because real data has holes and the card
        // has to look right with them.
        if (random.nextInt(12) == 0) return null

        val wave = sin(dayIndex / 2.4) * 0.5 + 0.5
        val jitter = random.nextDouble(-0.08, 0.08)
        val factor = (wave + jitter).coerceIn(0.0, 1.0)

        return when (descriptor.unit) {
            MetricUnit.Steps, MetricUnit.Count -> 4200 + factor * 7000
            MetricUnit.Bpm -> 58 + factor * 26
            MetricUnit.Percent -> 95 + factor * 4
            MetricUnit.Kilograms -> 78.0 + factor * 1.4
            MetricUnit.Kilocalories -> 320 + factor * 900
            MetricUnit.Hours -> 6.0 + factor * 2.6
            MetricUnit.Minutes -> 20 + factor * 70
            MetricUnit.Meters -> 2000 + factor * 5000
            MetricUnit.Kilometres -> 2.0 + factor * 6
            MetricUnit.Litres -> 1.2 + factor * 1.4
            MetricUnit.Celsius -> 36.2 + factor * 1.0
            MetricUnit.MillimolesPerLitre -> 4.4 + factor * 1.8
            MetricUnit.MillimetresOfMercury -> 112 + factor * 18
            MetricUnit.Watts -> 90 + factor * 140
            MetricUnit.MetresPerSecond -> 1.1 + factor * 2.4
            MetricUnit.MillilitresPerMinuteKilogram -> 38 + factor * 9
            MetricUnit.BreathsPerMinute -> 13 + factor * 5
            MetricUnit.Centimetres -> 178.0
            MetricUnit.Milliseconds -> 28 + factor * 30
            MetricUnit.StepsPerMinute -> 95 + factor * 30
            MetricUnit.RevolutionsPerMinute -> 60 + factor * 30
            MetricUnit.CelsiusDelta -> -0.4 + factor * 0.9
        }.let { if (abs(it) < 0.0001) null else it }
    }

    private fun sourceFor(id: MetricId): String = when (id) {
        MetricId.Weight -> "com.xiaomi.hm.health"
        else -> "com.xiaomi.wearable"
    }
}
