package hu.galambos.healthy.ui.components

import androidx.compose.foundation.Canvas
import kotlin.math.roundToInt
import java.time.format.FormatStyle
import java.time.format.DateTimeFormatter
import hu.galambos.healthy.ui.format.formatValue
import hu.galambos.healthy.domain.metric.MetricUnit
import hu.galambos.healthy.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
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
    unit: MetricUnit? = null,
) {
    val values = buckets.map { it.value }
    if (values.none { it != null }) return

    /*
     * Naming the unit is what makes the chart answerable.
     *
     * Without it a tap could mark a day but not say what the day held, so the
     * chart stays a picture and does not take the touch — which is also what
     * the overview wants, where the card itself is the target and a second one
     * inside it would compete for the same finger.
     */
    var picked by remember(buckets) { mutableStateOf<Int?>(null) }
    val at = picked?.let { positionOf(it, values.size, style) }

    val present = values.filterNotNull()
    val min = present.min()
    val max = present.max()
    // A flat series would divide by zero; give it a band so it draws level.
    val span = (max - min).takeIf { it > 0.0001 } ?: (if (max == 0.0) 1.0 else max * 0.1)
    val floor = if (style == SparklineStyle.Bars) minOf(min, 0.0) else min - span * 0.15
    val ceiling = max + span * 0.15

    val markerColour = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

    ChartOverlay(
        marker = at,
        tooltip = if (unit == null) null else picked?.let { index ->
            buckets.getOrNull(index)?.let { bucket -> { DayReadout(bucket, unit, color) } }
        },
    ) {
        Canvas(
            if (unit == null) {
                modifier
            } else {
                modifier.pointerInput(buckets) {
                    detectTapGestures { offset ->
                        picked = indexAt(offset.x / size.width, values.size, style)
                    }
                }
            },
        ) {
            val fraction = { value: Double ->
                ((value - floor) / (ceiling - floor)).coerceIn(0.0, 1.0).toFloat()
            }
            when (style) {
                SparklineStyle.Bars -> drawBars(values, color, fraction)
                SparklineStyle.Line -> drawSeries(values, color, fraction)
            }
            at?.let { position ->
                val x = position * size.width
                drawLine(
                    color = markerColour,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
        }
    }
}

/**
 * Which day a touch landed on, and where that day sits across the width.
 *
 * A bar owns a slot, so it is picked anywhere within it and marked down its
 * middle. A point on a line owns no width at all, so the nearest one is picked
 * and marked exactly where it is drawn — the marker has to land on the reading
 * it names, not near it.
 */
private fun indexAt(fraction: Float, count: Int, style: SparklineStyle): Int? {
    if (count <= 0) return null
    val at = fraction.coerceIn(0f, 1f)
    return when {
        style == SparklineStyle.Bars || count == 1 ->
            (at * count).toInt().coerceIn(0, count - 1)

        else -> (at * (count - 1)).roundToInt().coerceIn(0, count - 1)
    }
}

private fun positionOf(index: Int, count: Int, style: SparklineStyle): Float = when {
    style == SparklineStyle.Bars || count <= 1 -> (index + 0.5f) / count
    else -> index.toFloat() / (count - 1)
}

@Composable
private fun DayReadout(bucket: Bucket, unit: MetricUnit, color: Color) {
    val day = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val value = bucket.value

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = if (value == null) {
                    stringResource(R.string.card_no_data)
                } else {
                    val formatted = formatValue(value, unit)
                    if (formatted.unit.isEmpty()) {
                        formatted.number
                    } else {
                        "${formatted.number} ${formatted.unit}"
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = day.format(bucket.date),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
