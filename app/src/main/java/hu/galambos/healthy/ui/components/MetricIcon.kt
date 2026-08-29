package hu.galambos.healthy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.R
import hu.galambos.healthy.domain.metric.MetricId

/**
 * Which mark a metric wears.
 *
 * Chosen in the UI rather than stored in the registry: an icon is a
 * presentation decision, and several metrics sensibly share one — a resting
 * heart rate and a heart rate variability are both the heart.
 */
@DrawableRes
fun metricIconRes(id: MetricId): Int = when (id) {
    MetricId.Steps, MetricId.StepsCadence -> R.drawable.ic_metric_steps

    MetricId.HeartRate, MetricId.RestingHeartRate, MetricId.HeartRateVariability ->
        R.drawable.ic_metric_heart

    MetricId.Sleep -> R.drawable.ic_metric_sleep
    MetricId.OxygenSaturation -> R.drawable.ic_metric_oxygen

    MetricId.ActiveCalories, MetricId.TotalCalories, MetricId.BasalMetabolicRate ->
        R.drawable.ic_metric_calories

    MetricId.Weight -> R.drawable.ic_metric_weight

    MetricId.BodyFat, MetricId.BodyWaterMass, MetricId.BoneMass,
    MetricId.LeanBodyMass, MetricId.Height,
    -> R.drawable.ic_metric_body

    MetricId.Distance, MetricId.Speed, MetricId.ElevationGained ->
        R.drawable.ic_metric_distance

    MetricId.Exercise, MetricId.Power, MetricId.CyclingPedalingCadence,
    MetricId.FloorsClimbed, MetricId.WheelchairPushes, MetricId.Vo2Max,
    -> R.drawable.ic_metric_exercise

    MetricId.Hydration, MetricId.Nutrition -> R.drawable.ic_metric_nutrition

    MetricId.BloodPressure, MetricId.BloodGlucose, MetricId.BodyTemperature,
    MetricId.SkinTemperature, MetricId.RespiratoryRate,
    -> R.drawable.ic_metric_vitals

    MetricId.Mindfulness -> R.drawable.ic_metric_generic
}

/**
 * The mark itself, tinted with the metric's accent and nothing else behind it.
 *
 * The design draws these as bare glyphs. A tinted plate under each one turns a
 * quiet row of cards into a row of badges, and the accent is already doing its
 * work in the chart below.
 */
@Composable
fun MetricIcon(
    id: MetricId,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    Icon(
        painter = painterResource(metricIconRes(id)),
        contentDescription = null,
        tint = accent,
        modifier = modifier.size(size),
    )
}
