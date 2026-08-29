package hu.galambos.healthy.data.scale

import hu.galambos.healthy.data.local.HealthyDatabase
import hu.galambos.healthy.data.local.MetricStore
import hu.galambos.healthy.data.local.ScaleMeasurementEntity
import hu.galambos.healthy.data.settings.Settings
import hu.galambos.healthy.data.settings.Sex
import hu.galambos.healthy.domain.metric.MetricId
import hu.galambos.healthy.domain.scale.BodyComposition
import hu.galambos.healthy.domain.scale.BodyCompositionCalculator
import hu.galambos.healthy.domain.scale.BodyProfile
import hu.galambos.healthy.domain.scale.ScaleReading
import hu.galambos.healthy.domain.summary.DataPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** Marks a reading as this app's own work rather than another app's record. */
const val SCALE_SOURCE = "healthy.scale"

/**
 * Files a weigh-in.
 *
 * Only the raw pair — weight and impedance — is stored. Everything else is
 * derived on the way in and can be derived again: correcting a height or a
 * birth year re-runs every past measurement instead of leaving behind figures
 * computed from a profile nobody remembers.
 */
class ScaleRecorder(
    private val database: HealthyDatabase,
    private val store: MetricStore,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    suspend fun record(reading: ScaleReading, settings: Settings) {
        if (!reading.stabilised) return

        val time = reading.instant()
        database.scaleDao().upsert(
            ScaleMeasurementEntity(
                timeEpochMillis = time.toEpochMilli(),
                weightKg = reading.weightKg,
                impedanceOhms = reading.impedanceOhms,
            ),
        )
        derive(reading.weightKg, reading.impedanceOhms, time.atZone(zone).toLocalDate(), settings)
    }

    /**
     * Recomputes every stored measurement. Called when the profile changes:
     * the numbers depend on height, age and sex, so a corrected profile means
     * the whole history was computed from the wrong person.
     */
    suspend fun recomputeAll(settings: Settings) {
        database.scaleDao().all().forEach { measurement ->
            derive(
                weightKg = measurement.weightKg,
                impedance = measurement.impedanceOhms,
                date = Instant.ofEpochMilli(measurement.timeEpochMillis).atZone(zone).toLocalDate(),
                settings = settings,
            )
        }
    }

    private suspend fun derive(
        weightKg: Double,
        impedance: Int?,
        date: LocalDate,
        settings: Settings,
    ) {
        // The weight is written here too. It used to be left to Health
        // Connect, on the assumption that Zepp Life would keep putting it
        // there — but Zepp Life is what this feature replaces, and once it is
        // gone nothing else writes a weight at all. Where both do arrive, the
        // day's bucket simply takes the later write; they are measuring the
        // same scale.
        val weightOnly = mapOf(MetricId.Weight to weightKg)

        val profile = settings.toProfile()
        val composition = if (profile != null && impedance != null) {
            BodyCompositionCalculator.of(profile, weightKg, impedance)
        } else {
            null
        }

        val values = weightOnly + composition?.toValues().orEmpty()
        values.forEach { (id, value) ->
            store.putDailyValues(id, mapOf(date to value))
            store.putLatest(
                id,
                DataPoint(
                    value = value,
                    time = date.atStartOfDay(zone).toInstant(),
                    sourcePackage = SCALE_SOURCE,
                ),
            )
        }
    }
}

/**
 * The scale sends its clock in UTC, not in local time.
 *
 * Measured, not assumed: a weigh-in logged at 14:08 local arrived carrying
 * 12:08, and Budapest was two hours ahead of UTC that day. Reading it as local
 * time and converting again put every measurement two hours into the past.
 *
 * The scale's clock is set by whichever app last paired with it, and with Zepp
 * Life gone nothing will correct it again. So a timestamp that lands in the
 * future or more than a day back is not trusted, and the moment of reception
 * is used instead — a weigh-in dated by a drifting clock is worse than one
 * dated a few seconds late.
 */
private fun ScaleReading.instant(): Instant {
    val fromScale = measuredAt.toInstant(ZoneOffset.UTC)
    val now = Instant.now()
    val plausible = fromScale <= now.plusSeconds(60) &&
        fromScale >= now.minus(1, ChronoUnit.DAYS)
    return if (plausible) fromScale else now
}

private fun Settings.toProfile(): BodyProfile? {
    if (heightCm <= 0 || birthYear <= 0) return null
    return BodyProfile(
        heightCm = heightCm,
        ageYears = LocalDate.now().year - birthYear,
        female = sex == Sex.Female,
    )
}

private fun BodyComposition.toValues(): Map<MetricId, Double> = mapOf(
    MetricId.BodyFat to fatPercent,
    MetricId.BodyWaterMass to waterMassKg,
    MetricId.BoneMass to boneMassKg,
    MetricId.LeanBodyMass to muscleMassKg,
    MetricId.BasalMetabolicRate to basalRateKcal,
)
