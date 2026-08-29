package hu.galambos.healthy.ui.components

import androidx.compose.foundation.Canvas
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxWithConstraints
import java.time.format.FormatStyle
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.R
import hu.galambos.healthy.domain.sleep.SleepNight
import hu.galambos.healthy.domain.sleep.SleepStage
import hu.galambos.healthy.ui.theme.sleepStageColor
import java.time.Duration
import java.time.Instant

/**
 * The night as one unbroken ribbon stepping between four levels.
 *
 * A stage holds its level for exactly as long as it lasted, drawn as a wide
 * horizontal run; the change to the next level is a thin vertical. That is
 * what makes the ribbon appear to widen and narrow — the width is time, and
 * a level change takes none.
 *
 * Each level carries its own colour, and the link between two levels fades
 * from the colour being left into the colour being entered.
 */
@Composable
fun Hypnogram(
    night: SleepNight,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
) {
    val colors = mapOf(
        SleepStage.Awake to sleepStageColor(SleepStage.Awake),
        SleepStage.Rem to sleepStageColor(SleepStage.Rem),
        SleepStage.Light to sleepStageColor(SleepStage.Light),
        SleepStage.Deep to sleepStageColor(SleepStage.Deep),
    )
    val totalMillis = Duration.between(night.start, night.end).toMillis().toFloat()
    if (totalMillis <= 0f || night.segments.isEmpty()) return

    // Always all four, whether or not the night used them: a chart whose scale
    // changes with the data cannot be compared with last night's, and a stage
    // that never happened is worth seeing as an absence.
    val levels = listOf(SleepStage.Awake, SleepStage.Rem, SleepStage.Light, SleepStage.Deep)

    // Neighbours of the same stage are one stretch, so a stage that held for
    // an hour is one step rather than a row of them.
    val blocks = remember(night) { mergeAdjacent(night) }

    /** Where along the night the reader last pointed, if they have. */
    var marker by remember(night) { mutableStateOf<Float?>(null) }
    val picked = marker?.let { at ->
        val instant = night.start.plusMillis((totalMillis * at).toLong())
        blocks.firstOrNull { !instant.isBefore(it.start) && instant.isBefore(it.end) }
            ?: blocks.lastOrNull()
    }

    val gridColour = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    val markerColour = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

    /** The tooltip's own width, so it can be kept inside the chart. */
    var tipWidth by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val chartWidth = constraints.maxWidth.toFloat()

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(if (expanded) EXPANDED_HEIGHT else CHART_HEIGHT)
                .pointerInput(night) {
                    detectTapGestures { offset ->
                        marker = (offset.x / size.width).coerceIn(0f, 1f)
                    }
                },
        ) {
            val run = RUN_THICKNESS.toPx()
            val link = LINK_THICKNESS.toPx()
            val usable = size.height - run

            fun yOf(stage: SleepStage): Float {
                val index = levels.indexOf(normalise(stage)).coerceAtLeast(0)
                return run / 2f + usable * index / (levels.size - 1).toFloat()
            }

            fun xOf(at: Instant) =
                Duration.between(night.start, at).toMillis() / totalMillis * size.width

            fun colourOf(stage: SleepStage) =
                colors[normalise(stage)] ?: colors.getValue(SleepStage.Light)

            levels.forEach { stage ->
                val y = yOf(stage)
                drawLine(
                    color = gridColour,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            /*
             * The changes of level go down first and the steps cover where
             * they land, so the ends of a change are never seen and the trace
             * reads as one line: broad while a stage holds, a quarter of that
             * where it moves.
             */
            blocks.zipWithNext { before, after ->
                val fromY = yOf(before.stage)
                val toY = yOf(after.stage)
                if (fromY == toY) return@zipWithNext
                val fromColour = colourOf(before.stage)
                val toColour = colourOf(after.stage)
                drawLine(
                    // A vertical gradient always runs top to bottom, so the
                    // colours have to be ordered by where they sit rather than
                    // by which came first — otherwise every climb out of deep
                    // sleep fades the wrong way.
                    brush = Brush.verticalGradient(
                        colors = if (fromY < toY) {
                            listOf(fromColour, toColour)
                        } else {
                            listOf(toColour, fromColour)
                        },
                        startY = minOf(fromY, toY),
                        endY = maxOf(fromY, toY),
                    ),
                    start = Offset(xOf(after.start), fromY),
                    end = Offset(xOf(after.start), toY),
                    strokeWidth = link,
                    cap = StrokeCap.Round,
                )
            }

            blocks.forEachIndexed { index, block ->
                val y = yOf(block.stage)
                val previousY = blocks.getOrNull(index - 1)?.let { yOf(it.stage) }
                val nextY = blocks.getOrNull(index + 1)?.let { yOf(it.stage) }

                /*
                 * How far the step is allowed to reach. A change of level is a
                 * quarter as wide as the step is thick, so the step may run
                 * out to the outside of the change either side and no further
                 * — that is what makes the step end exactly where the change
                 * ends rather than overhanging it with a lip.
                 */
                val left = xOf(block.start) - if (previousY == null) 0f else link / 2f
                val right = xOf(block.end) + if (nextY == null) 0f else link / 2f
                val radius = minOf(run, right - left) / 2f

                /*
                 * The corner a change of level uses is left square.
                 *
                 * A rounded corner curves away before the edge is reached, so
                 * a change leaving a rounded bottom would appear to start just
                 * short of the step and hang off it. Square on the side the
                 * change happens, round on the side it does not, and the step
                 * runs into the change with nothing between them — while the
                 * free corners stay soft.
                 */
                fun corner(neighbour: Float?, below: Boolean) = when {
                    neighbour == null -> radius
                    below -> if (neighbour > y) 0f else radius
                    else -> if (neighbour < y) 0f else radius
                }

                val step = Path()
                step.addRoundRect(
                    RoundRect(
                        rect = Rect(left, y - run / 2f, right, y + run / 2f),
                        topLeft = CornerRadius(corner(previousY, below = false)),
                        topRight = CornerRadius(corner(nextY, below = false)),
                        bottomRight = CornerRadius(corner(nextY, below = true)),
                        bottomLeft = CornerRadius(corner(previousY, below = true)),
                    ),
                )
                drawPath(step, color = colourOf(block.stage))
            }

            marker?.let { at ->
                val x = at * size.width
                drawLine(
                    color = markerColour,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
        }

        // Beside the marker rather than under the chart: the reading belongs
        // to the moment being pointed at, and a caption below leaves the eye
        // to work out which moment that was.
        picked?.let { block ->
            Box(
                Modifier
                    .offset {
                        val wanted = marker!! * chartWidth - tipWidth / 2f
                        IntOffset(
                            x = wanted.coerceIn(0f, (chartWidth - tipWidth).coerceAtLeast(0f))
                                .roundToInt(),
                            y = 0,
                        )
                    }
                    .onSizeChanged { tipWidth = it.width },
            ) {
                Tooltip(block, colors)
            }
        }
    }
}

@Composable
private fun Tooltip(block: Block, colors: Map<SleepStage, Color>) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
    ) {
        Box(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Readout(block, colors)
        }
    }
}

@Composable
private fun Readout(block: Block, colors: Map<SleepStage, Color>) {
    val minutes = Duration.between(block.start, block.end).toMinutes()
    val zone = remember { ZoneId.systemDefault() }
    val clock = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors[block.stage] ?: colors.getValue(SleepStage.Light)),
            )
            Text(
                text = stringResource(nameOf(block.stage)) + "  ·  " + stringResource(
                    R.string.duration_hours_minutes,
                    (minutes / 60).toInt(),
                    (minutes % 60).toInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = "${clock.format(block.start.atZone(zone))} – " +
                clock.format(block.end.atZone(zone)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@StringRes
private fun nameOf(stage: SleepStage): Int = when (normalise(stage)) {
    SleepStage.Awake -> R.string.sleep_stage_awake
    SleepStage.Rem -> R.string.sleep_stage_rem
    SleepStage.Deep -> R.string.sleep_stage_deep
    else -> R.string.sleep_stage_light
}

private val CHART_HEIGHT = 224.dp

/** Full screen, where the same night finally has room for its short stages. */
private val EXPANDED_HEIGHT = 420.dp

/**
 * Measured off the reference: the step is about a fifth as thick as the gap
 * between levels, and a change of level is a quarter of that again.
 * Drawn evenly thick the trace stops reading as time held versus time moving.
 */
private val RUN_THICKNESS = 13.dp
private val LINK_THICKNESS = 3.5.dp

/** Between the level's name and where the trace may start. */
private val LABEL_GAP = 10.dp

private data class Block(
    val stage: SleepStage,
    val start: Instant,
    val end: Instant,
)

private fun mergeAdjacent(night: SleepNight): List<Block> {
    val blocks = mutableListOf<Block>()
    night.segments.forEach { segment ->
        val stage = normalise(segment.stage)
        val last = blocks.lastOrNull()
        if (last != null && last.stage == stage) {
            blocks[blocks.lastIndex] = last.copy(end = segment.end)
        } else {
            blocks += Block(stage, segment.start, segment.end)
        }
    }
    return blocks
}

/** A source that never split the night writes "sleeping"; treat it as light. */
private fun normalise(stage: SleepStage): SleepStage = when (stage) {
    SleepStage.Sleeping -> SleepStage.Light
    SleepStage.AwakeInBed, SleepStage.OutOfBed, SleepStage.Unknown -> SleepStage.Awake
    else -> stage
}
