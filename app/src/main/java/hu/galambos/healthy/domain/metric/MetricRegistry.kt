package hu.galambos.healthy.domain.metric

import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import hu.galambos.healthy.R
import java.time.Duration

/**
 * The single source of truth for what this app reads and shows.
 *
 * The seven at the top are the metrics this phone is known to actually carry:
 * the watch writes six through Mi Fitness, the scale writes weight through
 * Zepp Life and Google Fit. The rest follow, grouped by category, and most
 * will be empty on any given phone — which is itself worth knowing, and is why
 * empty cards are hidden behind a toggle rather than left out of the registry.
 *
 * Cycle tracking and medical records are deliberately absent. Neither has a
 * source here, and both would lengthen the permission screen with types
 * nothing will ever write.
 */
object MetricRegistry {

    // Mindfulness is still an experimental Health Connect API. It is included
    // because the app's claim is to show everything the phone holds; if the
    // API changes, this one row is what breaks.
    @OptIn(ExperimentalMindfulnessSessionApi::class)
    val all: List<MetricDescriptor> = listOf(
        // --- The headline seven ---------------------------------------------
        MetricDescriptor(
            id = MetricId.Steps,
            recordType = StepsRecord::class,
            category = MetricCategory.Activity,
            titleRes = R.string.metric_steps,
            unit = MetricUnit.Steps,
            accent = MetricAccent.Steps,
            reading = { r -> (r as? StepsRecord)?.let { Reading(it.count.toDouble(), it.endTime) } },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                value = { it[StepsRecord.COUNT_TOTAL]?.toDouble() },
            ),
        ),
        MetricDescriptor(
            id = MetricId.HeartRate,
            recordType = HeartRateRecord::class,
            category = MetricCategory.Vitals,
            titleRes = R.string.metric_heart_rate,
            unit = MetricUnit.Bpm,
            accent = MetricAccent.Heart,
            // A heart rate record is a series; its last sample is the most
            // recent reading the watch took.
            reading = { record ->
                (record as? HeartRateRecord)?.samples?.lastOrNull()?.let { sample ->
                    Reading(sample.beatsPerMinute.toDouble(), sample.time)
                }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(HeartRateRecord.BPM_AVG),
                value = { it[HeartRateRecord.BPM_AVG]?.toDouble() },
            ),
        ),
        MetricDescriptor(
            id = MetricId.Sleep,
            recordType = SleepSessionRecord::class,
            category = MetricCategory.Sleep,
            titleRes = R.string.metric_sleep,
            unit = MetricUnit.Hours,
            accent = MetricAccent.Sleep,
            reading = { record ->
                (record as? SleepSessionRecord)?.let {
                    Reading(
                        Duration.between(it.startTime, it.endTime).toMinutes() / 60.0,
                        it.endTime,
                    )
                }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                value = { result ->
                    result[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.let { it.toMinutes() / 60.0 }
                },
            ),
        ),
        MetricDescriptor(
            id = MetricId.OxygenSaturation,
            recordType = OxygenSaturationRecord::class,
            category = MetricCategory.Vitals,
            titleRes = R.string.metric_oxygen_saturation,
            unit = MetricUnit.Percent,
            accent = MetricAccent.Oxygen,
            reading = { r ->
                (r as? OxygenSaturationRecord)?.let { Reading(it.percentage.value, it.time) }
            },
            // Health Connect offers no aggregate for blood oxygen.
            trend = TrendStrategy.Samples,
        ),
        MetricDescriptor(
            id = MetricId.ActiveCalories,
            recordType = ActiveCaloriesBurnedRecord::class,
            category = MetricCategory.Activity,
            titleRes = R.string.metric_active_calories,
            unit = MetricUnit.Kilocalories,
            accent = MetricAccent.Calories,
            reading = { r ->
                (r as? ActiveCaloriesBurnedRecord)?.let {
                    Reading(it.energy.inKilocalories, it.endTime)
                }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                value = { it[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories },
            ),
        ),
        MetricDescriptor(
            id = MetricId.TotalCalories,
            recordType = TotalCaloriesBurnedRecord::class,
            category = MetricCategory.Activity,
            titleRes = R.string.metric_total_calories,
            unit = MetricUnit.Kilocalories,
            accent = MetricAccent.Calories,
            reading = { r ->
                (r as? TotalCaloriesBurnedRecord)?.let {
                    Reading(it.energy.inKilocalories, it.endTime)
                }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                value = { it[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories },
            ),
        ),
        MetricDescriptor(
            id = MetricId.Weight,
            recordType = WeightRecord::class,
            category = MetricCategory.Body,
            titleRes = R.string.metric_weight,
            unit = MetricUnit.Kilograms,
            accent = MetricAccent.Weight,
            reading = { r -> (r as? WeightRecord)?.let { Reading(it.weight.inKilograms, it.time) } },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(WeightRecord.WEIGHT_AVG),
                value = { it[WeightRecord.WEIGHT_AVG]?.inKilograms },
            ),
        ),

        // --- Activity -------------------------------------------------------
        MetricDescriptor(
            id = MetricId.Distance,
            recordType = DistanceRecord::class,
            category = MetricCategory.Activity,
            titleRes = R.string.metric_distance,
            unit = MetricUnit.Kilometres,
            reading = { r ->
                (r as? DistanceRecord)?.let { Reading(it.distance.inKilometers, it.endTime) }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                value = { it[DistanceRecord.DISTANCE_TOTAL]?.inKilometers },
            ),
        ),
        MetricDescriptor(
            id = MetricId.ElevationGained,
            recordType = ElevationGainedRecord::class,
            category = MetricCategory.Activity,
            titleRes = R.string.metric_elevation,
            unit = MetricUnit.Meters,
            reading = { r ->
                (r as? ElevationGainedRecord)?.let { Reading(it.elevation.inMeters, it.endTime) }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(ElevationGainedRecord.ELEVATION_GAINED_TOTAL),
                value = { it[ElevationGainedRecord.ELEVATION_GAINED_TOTAL]?.inMeters },
            ),
        ),
        MetricDescriptor(
            id = MetricId.Exercise,
            recordType = ExerciseSessionRecord::class,
            category = MetricCategory.Activity,
            titleRes = R.string.metric_exercise,
            unit = MetricUnit.Minutes,
            reading = { r ->
                (r as? ExerciseSessionRecord)?.let {
                    Reading(
                        Duration.between(it.startTime, it.endTime).toMinutes().toDouble(),
                        it.endTime,
                    )
                }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL),
                value = { it[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes()?.toDouble() },
            ),
        ),
        MetricDescriptor(
            id = MetricId.FloorsClimbed,
            recordType = FloorsClimbedRecord::class,
            category = MetricCategory.Activity,
            titleRes = R.string.metric_floors,
            unit = MetricUnit.Count,
            reading = { r -> (r as? FloorsClimbedRecord)?.let { Reading(it.floors, it.endTime) } },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL),
                value = { it[FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL] },
            ),
        ),
        MetricDescriptor(
            id = MetricId.Power,
            recordType = PowerRecord::class,
            category = MetricCategory.Activity,
            titleRes = R.string.metric_power,
            unit = MetricUnit.Watts,
            reading = { r ->
                (r as? PowerRecord)?.samples?.lastOrNull()?.let { Reading(it.power.inWatts, it.time) }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(PowerRecord.POWER_AVG),
                value = { it[PowerRecord.POWER_AVG]?.inWatts },
            ),
        ),
        MetricDescriptor(
            id = MetricId.Speed,
            recordType = SpeedRecord::class,
            category = MetricCategory.Activity,
            titleRes = R.string.metric_speed,
            unit = MetricUnit.MetresPerSecond,
            reading = { r ->
                (r as? SpeedRecord)?.samples?.lastOrNull()?.let {
                    Reading(it.speed.inMetersPerSecond, it.time)
                }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(SpeedRecord.SPEED_AVG),
                value = { it[SpeedRecord.SPEED_AVG]?.inMetersPerSecond },
            ),
        ),
        MetricDescriptor(
            id = MetricId.StepsCadence,
            recordType = StepsCadenceRecord::class,
            category = MetricCategory.Activity,
            titleRes = R.string.metric_steps_cadence,
            unit = MetricUnit.StepsPerMinute,
            reading = { r ->
                (r as? StepsCadenceRecord)?.samples?.lastOrNull()?.let { Reading(it.rate, it.time) }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(StepsCadenceRecord.RATE_AVG),
                value = { it[StepsCadenceRecord.RATE_AVG] },
            ),
        ),
        MetricDescriptor(
            id = MetricId.CyclingPedalingCadence,
            recordType = CyclingPedalingCadenceRecord::class,
            category = MetricCategory.Activity,
            titleRes = R.string.metric_cycling_cadence,
            unit = MetricUnit.RevolutionsPerMinute,
            reading = { r ->
                (r as? CyclingPedalingCadenceRecord)?.samples?.lastOrNull()?.let {
                    Reading(it.revolutionsPerMinute, it.time)
                }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(CyclingPedalingCadenceRecord.RPM_AVG),
                value = { it[CyclingPedalingCadenceRecord.RPM_AVG] },
            ),
        ),
        MetricDescriptor(
            id = MetricId.Vo2Max,
            recordType = Vo2MaxRecord::class,
            category = MetricCategory.Activity,
            titleRes = R.string.metric_vo2max,
            unit = MetricUnit.MillilitresPerMinuteKilogram,
            reading = { r ->
                (r as? Vo2MaxRecord)?.let { Reading(it.vo2MillilitersPerMinuteKilogram, it.time) }
            },
            trend = TrendStrategy.Samples,
        ),
        MetricDescriptor(
            id = MetricId.WheelchairPushes,
            recordType = WheelchairPushesRecord::class,
            category = MetricCategory.Activity,
            titleRes = R.string.metric_wheelchair_pushes,
            unit = MetricUnit.Count,
            reading = { r ->
                (r as? WheelchairPushesRecord)?.let { Reading(it.count.toDouble(), it.endTime) }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(WheelchairPushesRecord.COUNT_TOTAL),
                value = { it[WheelchairPushesRecord.COUNT_TOTAL]?.toDouble() },
            ),
        ),

        // --- Body -----------------------------------------------------------
        MetricDescriptor(
            id = MetricId.BodyFat,
            recordType = BodyFatRecord::class,
            category = MetricCategory.Body,
            titleRes = R.string.metric_body_fat,
            unit = MetricUnit.Percent,
            reading = { r -> (r as? BodyFatRecord)?.let { Reading(it.percentage.value, it.time) } },
            trend = TrendStrategy.Samples,
        ),
        MetricDescriptor(
            id = MetricId.BodyWaterMass,
            recordType = BodyWaterMassRecord::class,
            category = MetricCategory.Body,
            titleRes = R.string.metric_body_water,
            unit = MetricUnit.Kilograms,
            reading = { r -> (r as? BodyWaterMassRecord)?.let { Reading(it.mass.inKilograms, it.time) } },
            trend = TrendStrategy.Samples,
        ),
        MetricDescriptor(
            id = MetricId.BoneMass,
            recordType = BoneMassRecord::class,
            category = MetricCategory.Body,
            titleRes = R.string.metric_bone_mass,
            unit = MetricUnit.Kilograms,
            reading = { r -> (r as? BoneMassRecord)?.let { Reading(it.mass.inKilograms, it.time) } },
            trend = TrendStrategy.Samples,
        ),
        MetricDescriptor(
            id = MetricId.LeanBodyMass,
            recordType = LeanBodyMassRecord::class,
            category = MetricCategory.Body,
            titleRes = R.string.metric_lean_mass,
            unit = MetricUnit.Kilograms,
            reading = { r -> (r as? LeanBodyMassRecord)?.let { Reading(it.mass.inKilograms, it.time) } },
            trend = TrendStrategy.Samples,
        ),
        MetricDescriptor(
            id = MetricId.Height,
            recordType = HeightRecord::class,
            category = MetricCategory.Body,
            titleRes = R.string.metric_height,
            unit = MetricUnit.Centimetres,
            reading = { r -> (r as? HeightRecord)?.let { Reading(it.height.inMeters * 100, it.time) } },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(HeightRecord.HEIGHT_AVG),
                value = { it[HeightRecord.HEIGHT_AVG]?.inMeters?.times(100) },
            ),
        ),
        MetricDescriptor(
            id = MetricId.BasalMetabolicRate,
            recordType = BasalMetabolicRateRecord::class,
            category = MetricCategory.Body,
            titleRes = R.string.metric_basal_rate,
            unit = MetricUnit.Kilocalories,
            reading = { r ->
                (r as? BasalMetabolicRateRecord)?.let {
                    Reading(it.basalMetabolicRate.inKilocaloriesPerDay, it.time)
                }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL),
                value = { it[BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL]?.inKilocalories },
            ),
        ),

        // --- Vitals ---------------------------------------------------------
        MetricDescriptor(
            id = MetricId.RestingHeartRate,
            recordType = RestingHeartRateRecord::class,
            category = MetricCategory.Vitals,
            titleRes = R.string.metric_resting_heart_rate,
            unit = MetricUnit.Bpm,
            accent = MetricAccent.Heart,
            reading = { r ->
                (r as? RestingHeartRateRecord)?.let { Reading(it.beatsPerMinute.toDouble(), it.time) }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(RestingHeartRateRecord.BPM_AVG),
                value = { it[RestingHeartRateRecord.BPM_AVG]?.toDouble() },
            ),
        ),
        MetricDescriptor(
            id = MetricId.HeartRateVariability,
            recordType = HeartRateVariabilityRmssdRecord::class,
            category = MetricCategory.Vitals,
            titleRes = R.string.metric_hrv,
            unit = MetricUnit.Milliseconds,
            accent = MetricAccent.Heart,
            reading = { r ->
                (r as? HeartRateVariabilityRmssdRecord)?.let {
                    Reading(it.heartRateVariabilityMillis, it.time)
                }
            },
            trend = TrendStrategy.Samples,
        ),
        MetricDescriptor(
            id = MetricId.BloodPressure,
            recordType = BloodPressureRecord::class,
            category = MetricCategory.Vitals,
            titleRes = R.string.metric_blood_pressure,
            unit = MetricUnit.MillimetresOfMercury,
            // The card carries systolic; both numbers belong on a detail view.
            reading = { r ->
                (r as? BloodPressureRecord)?.let {
                    Reading(it.systolic.inMillimetersOfMercury, it.time)
                }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(BloodPressureRecord.SYSTOLIC_AVG),
                value = { it[BloodPressureRecord.SYSTOLIC_AVG]?.inMillimetersOfMercury },
            ),
        ),
        MetricDescriptor(
            id = MetricId.BloodGlucose,
            recordType = BloodGlucoseRecord::class,
            category = MetricCategory.Vitals,
            titleRes = R.string.metric_blood_glucose,
            unit = MetricUnit.MillimolesPerLitre,
            reading = { r ->
                (r as? BloodGlucoseRecord)?.let { Reading(it.level.inMillimolesPerLiter, it.time) }
            },
            trend = TrendStrategy.Samples,
        ),
        MetricDescriptor(
            id = MetricId.BodyTemperature,
            recordType = BodyTemperatureRecord::class,
            category = MetricCategory.Vitals,
            titleRes = R.string.metric_body_temperature,
            unit = MetricUnit.Celsius,
            reading = { r ->
                (r as? BodyTemperatureRecord)?.let { Reading(it.temperature.inCelsius, it.time) }
            },
            trend = TrendStrategy.Samples,
        ),
        MetricDescriptor(
            id = MetricId.SkinTemperature,
            recordType = SkinTemperatureRecord::class,
            category = MetricCategory.Vitals,
            titleRes = R.string.metric_skin_temperature,
            // Health Connect stores skin temperature as a change from a
            // baseline rather than an absolute reading.
            unit = MetricUnit.CelsiusDelta,
            reading = { r ->
                (r as? SkinTemperatureRecord)?.deltas?.lastOrNull()?.let {
                    Reading(it.delta.inCelsius, it.time)
                }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(SkinTemperatureRecord.TEMPERATURE_DELTA_AVG),
                value = { it[SkinTemperatureRecord.TEMPERATURE_DELTA_AVG]?.inCelsius },
            ),
        ),
        MetricDescriptor(
            id = MetricId.RespiratoryRate,
            recordType = RespiratoryRateRecord::class,
            category = MetricCategory.Vitals,
            titleRes = R.string.metric_respiratory_rate,
            unit = MetricUnit.BreathsPerMinute,
            reading = { r -> (r as? RespiratoryRateRecord)?.let { Reading(it.rate, it.time) } },
            trend = TrendStrategy.Samples,
        ),

        // --- Nutrition ------------------------------------------------------
        MetricDescriptor(
            id = MetricId.Hydration,
            recordType = HydrationRecord::class,
            category = MetricCategory.Nutrition,
            titleRes = R.string.metric_hydration,
            unit = MetricUnit.Litres,
            reading = { r -> (r as? HydrationRecord)?.let { Reading(it.volume.inLiters, it.endTime) } },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                value = { it[HydrationRecord.VOLUME_TOTAL]?.inLiters },
            ),
        ),
        MetricDescriptor(
            id = MetricId.Nutrition,
            recordType = NutritionRecord::class,
            category = MetricCategory.Nutrition,
            titleRes = R.string.metric_nutrition,
            unit = MetricUnit.Kilocalories,
            // A nutrition record carries dozens of nutrients; energy is the one
            // number that means anything on a card this size.
            reading = { r ->
                (r as? NutritionRecord)?.let { record ->
                    record.energy?.let { Reading(it.inKilocalories, record.endTime) }
                }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(NutritionRecord.ENERGY_TOTAL),
                value = { it[NutritionRecord.ENERGY_TOTAL]?.inKilocalories },
            ),
        ),

        // --- Wellness -------------------------------------------------------
        MetricDescriptor(
            id = MetricId.Mindfulness,
            recordType = MindfulnessSessionRecord::class,
            category = MetricCategory.Wellness,
            titleRes = R.string.metric_mindfulness,
            unit = MetricUnit.Minutes,
            reading = { r ->
                (r as? MindfulnessSessionRecord)?.let {
                    Reading(
                        Duration.between(it.startTime, it.endTime).toMinutes().toDouble(),
                        it.endTime,
                    )
                }
            },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(MindfulnessSessionRecord.MINDFULNESS_DURATION_TOTAL),
                value = {
                    it[MindfulnessSessionRecord.MINDFULNESS_DURATION_TOTAL]?.toMinutes()?.toDouble()
                },
            ),
        ),
    )

    private val byId: Map<MetricId, MetricDescriptor> = all.associateBy { it.id }

    operator fun get(id: MetricId): MetricDescriptor = byId.getValue(id)

    /** The metrics the overview leads with, in the order the design shows them. */
    val headline: List<MetricDescriptor> = listOf(
        get(MetricId.Steps),
        get(MetricId.Sleep),
        get(MetricId.HeartRate),
    )

    /** Everything below the headline, grouped the way the dashboard sections it. */
    val byCategory: Map<MetricCategory, List<MetricDescriptor>> =
        all.groupBy { it.category }
}
