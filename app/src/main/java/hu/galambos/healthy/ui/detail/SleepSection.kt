package hu.galambos.healthy.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.R
import hu.galambos.healthy.domain.sleep.SleepNight
import hu.galambos.healthy.domain.sleep.SleepScore
import hu.galambos.healthy.domain.sleep.SleepStage
import hu.galambos.healthy.ui.components.Hypnogram
import hu.galambos.healthy.ui.components.StageDonut
import hu.galambos.healthy.ui.format.formatTimestamp
import hu.galambos.healthy.ui.theme.sleepStageColor
import java.time.Duration
import kotlin.math.roundToInt

/**
 * The night itself: when it started and ended, how it was shaped, how the
 * stages divided it, and what the body was doing meanwhile.
 */
@Composable
fun SleepSection(night: SleepNight, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Labelled(stringResource(R.string.sleep_fell_asleep), formatTimestamp(night.start))
            Labelled(stringResource(R.string.sleep_woke_up), formatTimestamp(night.end))
        }

        if (!night.hasStageDetail) {
            // A source that writes only "asleep" gets a duration and nothing
            // else. Drawing an empty hypnogram would suggest the detail exists.
            Text(
                text = stringResource(R.string.sleep_no_stages),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        Score(night)

        Hypnogram(
            night = night,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StageDonut(night = night, centreLabel = night.asleep.asHoursMinutes())
            StageBreakdown(night, Modifier.weight(1f))
        }

        Vitals(night)
    }
}

@Composable
private fun Score(night: SleepNight) {
    val score = SleepScore.of(night) ?: return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = score.toString(), style = MaterialTheme.typography.displaySmall)
            Text(
                text = stringResource(R.string.sleep_score_points),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        // Whose number this is matters. Mi Fitness shows its own and does not
        // write it to Health Connect, so this one is labelled as ours.
        Text(
            text = stringResource(R.string.sleep_score_explanation),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StageBreakdown(night: SleepNight, modifier: Modifier = Modifier) {
    val asleepMinutes = night.asleep.toMinutes().toDouble()
    if (asleepMinutes <= 0) return
    val byStage = night.byStage

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            Triple(SleepStage.Rem, R.string.sleep_stage_rem, R.string.sleep_reference_rem),
            Triple(SleepStage.Light, R.string.sleep_stage_light, R.string.sleep_reference_light),
            Triple(SleepStage.Deep, R.string.sleep_stage_deep, R.string.sleep_reference_deep),
        ).forEach { (stage, nameRes, referenceRes) ->
            val minutes = byStage[stage]?.toMinutes() ?: return@forEach
            val percent = (minutes / asleepMinutes * 100).roundToInt()
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(sleepStageColor(stage)),
                    )
                    Text(
                        text = stringResource(nameRes),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = "$percent%  ·  ${Duration.ofMinutes(minutes).asHoursMinutes()}",
                    style = MaterialTheme.typography.titleMedium,
                )
                // The reference band is clinical guidance, not this night's
                // data; it is labelled so nobody reads it as a measurement.
                Text(
                    text = stringResource(referenceRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Vitals(night: SleepNight) {
    val vitals = night.vitals
    if (vitals.heartRateBpm == null && vitals.oxygenPercent == null &&
        vitals.respiratoryRate == null
    ) {
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.sleep_vitals),
            style = MaterialTheme.typography.titleMedium,
        )
        vitals.heartRateBpm?.let {
            Labelled(
                stringResource(R.string.sleep_average_heart_rate),
                "${it.roundToInt()} ${stringResource(R.string.unit_bpm)}",
            )
        }
        vitals.oxygenPercent?.let {
            Labelled(
                stringResource(R.string.sleep_average_oxygen),
                "${it.roundToInt()}${stringResource(R.string.unit_percent)}",
            )
        }
        vitals.respiratoryRate?.let {
            Labelled(
                stringResource(R.string.sleep_average_respiratory),
                "${it.roundToInt()} ${stringResource(R.string.unit_breaths)}",
            )
        }
    }
}

@Composable
private fun Labelled(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Duration.asHoursMinutes(): String =
    stringResource(R.string.duration_hours_minutes, toHours().toInt(), (toMinutes() % 60).toInt())
