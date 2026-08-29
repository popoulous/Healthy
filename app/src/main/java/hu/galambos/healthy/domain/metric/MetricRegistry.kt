package hu.galambos.healthy.domain.metric

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import hu.galambos.healthy.R
import java.time.Duration

/**
 * The single source of truth for what this app reads and shows.
 *
 * These seven are the metrics the phone is known to actually carry — the watch
 * writes six of them through Mi Fitness, the scale writes weight through Zepp
 * Life and Google Fit. The rest of Health Connect's record types join them in
 * F3; the shape of a row does not change when they do.
 */
object MetricRegistry {

    val all: List<MetricDescriptor> = listOf(
        MetricDescriptor(
            id = MetricId.Steps,
            recordType = StepsRecord::class,
            category = MetricCategory.Activity,
            titleRes = R.string.metric_steps,
            unit = MetricUnit.Steps,
            accent = MetricAccent.Steps,
            latestValue = { (it as? StepsRecord)?.count?.toDouble() },
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
            // A heart rate record is a series; the last sample in it is the
            // most recent reading the watch took.
            latestValue = { record ->
                (record as? HeartRateRecord)?.samples?.lastOrNull()?.beatsPerMinute?.toDouble()
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
            latestValue = { record ->
                (record as? SleepSessionRecord)?.let {
                    Duration.between(it.startTime, it.endTime).toMinutes() / 60.0
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
            latestValue = { (it as? OxygenSaturationRecord)?.percentage?.value },
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
            latestValue = { (it as? ActiveCaloriesBurnedRecord)?.energy?.inKilocalories },
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
            latestValue = { (it as? TotalCaloriesBurnedRecord)?.energy?.inKilocalories },
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
            latestValue = { (it as? WeightRecord)?.weight?.inKilograms },
            trend = TrendStrategy.Aggregate(
                metrics = setOf(WeightRecord.WEIGHT_AVG),
                value = { it[WeightRecord.WEIGHT_AVG]?.inKilograms },
            ),
        ),
    )

    private val byId: Map<MetricId, MetricDescriptor> = all.associateBy { it.id }

    operator fun get(id: MetricId): MetricDescriptor =
        byId.getValue(id)

    /** The metrics the overview leads with, in the order the design shows them. */
    val headline: List<MetricDescriptor> = listOf(
        get(MetricId.Steps),
        get(MetricId.Sleep),
        get(MetricId.HeartRate),
    )
}
