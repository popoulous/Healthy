package hu.galambos.healthy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.domain.sleep.SleepNight
import hu.galambos.healthy.domain.sleep.SleepStage
import hu.galambos.healthy.ui.theme.sleepStageColor

/**
 * The night's stages as shares of the time actually asleep — time awake is
 * left out, because a slice for "not sleeping" in a sleep breakdown reads as
 * nonsense.
 */
@Composable
fun StageDonut(
    night: SleepNight,
    centreLabel: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 160.dp,
    thickness: Dp = 22.dp,
) {
    val asleepMinutes = night.asleep.toMinutes().toDouble()
    if (asleepMinutes <= 0) return

    val order = listOf(SleepStage.Deep, SleepStage.Light, SleepStage.Rem, SleepStage.Sleeping)
    val byStage = night.byStage
    val drawn = order.mapNotNull { stage ->
        val minutes = byStage[stage]?.toMinutes()?.toDouble() ?: return@mapNotNull null
        if (minutes <= 0) null else stage to minutes
    }

    /*
     * The stages are shares of each other, not of the night.
     *
     * They need not add up to the time asleep — a source can leave stretches
     * unclassified, and last night's came to ninety-eight per cent — and
     * dividing by the night instead left the ring two per cent short. All of
     * that shortfall lands in one place, where the last slice meets the first,
     * so a single gap was five times wider than the others and looked like a
     * mistake. The ring closes on what it draws; the share of the whole night
     * is what the figures beside it are for.
     */
    val drawnMinutes = drawn.sumOf { (_, minutes) -> minutes }
    if (drawnMinutes <= 0) return
    val slices = drawn.map { (stage, minutes) -> stage to (minutes / drawnMinutes).toFloat() }

    // Stage colours come from the theme, so they must be read while still in
    // a composable — the Canvas draw scope is not one.
    val colors = slices.associate { (stage, _) -> stage to sleepStageColor(stage) }

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val strokePx = thickness.toPx()
            var startAngle = -90f
            slices.forEach { (stage, fraction) ->
                val sweep = fraction * 360f
                drawArc(
                    color = colors.getValue(stage),
                    startAngle = startAngle,
                    sweepAngle = sweep - SLICE_GAP_DEGREES,
                    useCenter = false,
                    topLeft = Offset(strokePx / 2f, strokePx / 2f),
                    size = Size(size.width - strokePx, size.height - strokePx),
                    style = Stroke(width = strokePx),
                )
                startAngle += sweep
            }
        }
        Text(text = centreLabel, style = MaterialTheme.typography.titleMedium)
    }
}

/** A hairline between slices, so neighbouring stages stay distinguishable. */
private const val SLICE_GAP_DEGREES = 1.5f
