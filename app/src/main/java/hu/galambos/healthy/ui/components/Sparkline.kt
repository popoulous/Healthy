package hu.galambos.healthy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.domain.summary.Bucket
import hu.galambos.healthy.ui.theme.AccentColors
import hu.galambos.healthy.ui.theme.HealthyTheme
import java.time.LocalDate

/**
 * Two shapes for two kinds of number. Something counted up over a day —
 * steps, calories, hours slept — reads as a bar per day. Something measured
 * at a moment — weight, heart rate — reads as a line, because the space
 * between two readings is a trajectory rather than an absence.
 */
enum class SparklineStyle { Bars, Line }

/**
 * Drawn by hand rather than with a chart library. Six bars and a polyline do
 * not justify a dependency, and this way the missing days stay missing: a gap
 * is drawn as a gap, not smoothed over into a reading nobody took.
 */
@Composable
fun Sparkline(
    buckets: List<Bucket>,
    color: Color,
    style: SparklineStyle,
    modifier: Modifier = Modifier,
) {
    val values = buckets.map { it.value }
    if (values.none { it != null }) return

    val present = values.filterNotNull()
    val min = present.min()
    val max = present.max()
    // A flat series would divide by zero; give it a band so it draws level.
    val span = (max - min).takeIf { it > 0.0001 } ?: (if (max == 0.0) 1.0 else max * 0.1)
    val floor = if (style == SparklineStyle.Bars) minOf(min, 0.0) else min - span * 0.15
    val ceiling = max + span * 0.15

    Canvas(modifier) {
        val fraction = { value: Double ->
            ((value - floor) / (ceiling - floor)).coerceIn(0.0, 1.0).toFloat()
        }
        when (style) {
            SparklineStyle.Bars -> drawBars(values, color, fraction)
            SparklineStyle.Line -> drawSeries(values, color, fraction)
        }
    }
}

private fun DrawScope.drawBars(
    values: List<Double?>,
    color: Color,
    fraction: (Double) -> Float,
) {
    val slot = size.width / values.size
    val barWidth = (slot * 0.62f).coerceAtLeast(1.5f)
    values.forEachIndexed { index, value ->
        if (value == null) return@forEachIndexed
        val height = (size.height * fraction(value)).coerceAtLeast(1.5f)
        drawRect(
            color = color,
            topLeft = Offset(
                x = index * slot + (slot - barWidth) / 2f,
                y = size.height - height,
            ),
            size = Size(barWidth, height),
        )
    }
}

private fun DrawScope.drawSeries(
    values: List<Double?>,
    color: Color,
    fraction: (Double) -> Float,
) {
    val slot = if (values.size > 1) size.width / (values.size - 1) else size.width
    val path = Path()
    var started = false
    values.forEachIndexed { index, value ->
        if (value == null) {
            // Break the line rather than bridging a day with no reading.
            started = false
            return@forEachIndexed
        }
        val x = index * slot
        val y = size.height * (1f - fraction(value))
        if (started) path.lineTo(x, y) else path.moveTo(x, y)
        started = true
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
    )
}

@Preview(showBackground = true, widthDp = 160, heightDp = 48)
@Composable
private fun SparklinePreview() {
    HealthyTheme {
        Sparkline(
            buckets = listOf(3.0, 5.0, null, 8.0, 6.0, 9.0, 7.0)
                .mapIndexed { index, v -> Bucket(LocalDate.of(2026, 8, index + 1), v) },
            color = AccentColors.Steps,
            style = SparklineStyle.Bars,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
