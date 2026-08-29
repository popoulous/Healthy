package hu.galambos.healthy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.domain.sleep.SleepNight
import hu.galambos.healthy.domain.sleep.SleepStage
import hu.galambos.healthy.ui.theme.sleepStageColor
import java.time.Duration

/**
 * The night as a band per stage, laid out along real time.
 *
 * Rows are ordered awake at the top down to deep at the bottom, the way sleep
 * charts are conventionally read, so a glance shows how the night descended
 * and how often it surfaced.
 */
@Composable
fun Hypnogram(night: SleepNight, modifier: Modifier = Modifier) {
    val rows = listOf(SleepStage.Awake, SleepStage.Rem, SleepStage.Light, SleepStage.Deep)
    val colors = rows.associateWith { sleepStageColor(it) }
    val totalMillis = Duration.between(night.start, night.end).toMillis().toFloat()
    if (totalMillis <= 0f) return

    Canvas(modifier) {
        val rowHeight = size.height / rows.size
        val barHeight = rowHeight * 0.62f
        val radius = 3.dp.toPx()

        night.segments.forEach { segment ->
            // Sources that never split the night write "sleeping"; drawing it
            // on the light row keeps such a night legible instead of blank.
            val stage = if (segment.stage == SleepStage.Sleeping) SleepStage.Light else segment.stage
            val rowIndex = rows.indexOf(
                if (stage == SleepStage.AwakeInBed || stage == SleepStage.OutOfBed) {
                    SleepStage.Awake
                } else {
                    stage
                },
            )
            if (rowIndex < 0) return@forEach

            val startFraction =
                Duration.between(night.start, segment.start).toMillis() / totalMillis
            val widthFraction = segment.duration.toMillis() / totalMillis

            drawRoundRect(
                color = colors[rows[rowIndex]] ?: Color.Gray,
                topLeft = Offset(
                    x = startFraction * size.width,
                    y = rowIndex * rowHeight + (rowHeight - barHeight) / 2f,
                ),
                size = Size(
                    width = (widthFraction * size.width).coerceAtLeast(2f),
                    height = barHeight,
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )
        }
    }
}
