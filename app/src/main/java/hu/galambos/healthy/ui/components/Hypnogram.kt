package hu.galambos.healthy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.R
import hu.galambos.healthy.domain.sleep.SleepNight
import hu.galambos.healthy.domain.sleep.SleepStage
import hu.galambos.healthy.ui.theme.sleepStageColor
import java.time.Duration

/**
 * The night as one continuous line stepping between depths.
 *
 * A single trace, not a stage per block: what is interesting about a night is
 * the shape of the descent and how often it comes back up, and separate blocks
 * hide exactly that. The stage colours are already doing their work in the
 * donut below, so the line stays one colour and the rows carry the meaning.
 */
@Composable
fun Hypnogram(night: SleepNight, modifier: Modifier = Modifier) {
    val rows = listOf(
        SleepStage.Awake to R.string.sleep_stage_awake,
        SleepStage.Rem to R.string.sleep_stage_rem,
        SleepStage.Light to R.string.sleep_stage_light,
        SleepStage.Deep to R.string.sleep_stage_deep,
    )
    val line = sleepStageColor(SleepStage.Light)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val totalMillis = Duration.between(night.start, night.end).toMillis().toFloat()
    if (totalMillis <= 0f || night.segments.isEmpty()) return

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            rows.forEach { (_, label) ->
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            // The label column spreads four baselines across the full height,
            // so the trace has to use the same four positions or the line and
            // its labels drift apart.
            val step = if (rows.size > 1) size.height / (rows.size - 1) else 0f
            fun yOf(stage: SleepStage) = when (stage) {
                SleepStage.Rem -> step
                SleepStage.Light, SleepStage.Sleeping -> step * 2
                SleepStage.Deep -> step * 3
                else -> 0f
            }

            rows.forEachIndexed { index, _ ->
                val y = index * step
                drawLine(
                    color = grid,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }

            val path = Path()
            night.segments.forEachIndexed { index, segment ->
                val start = Duration.between(night.start, segment.start).toMillis() /
                    totalMillis * size.width
                val end = Duration.between(night.start, segment.end).toMillis() /
                    totalMillis * size.width
                val y = yOf(segment.stage)

                if (index == 0) path.moveTo(start, y) else path.lineTo(start, y)
                path.lineTo(end, y)
            }

            drawPath(
                path = path,
                color = line,
                style = Stroke(
                    width = 2.5f.dp.toPx(),
                    cap = StrokeCap.Butt,
                    join = StrokeJoin.Miter,
                ),
            )
        }
    }
}
