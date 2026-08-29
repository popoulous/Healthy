package hu.galambos.healthy.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A chart, and the answer to "what is that?" placed where the question was
 * asked.
 *
 * [marker] is where along the width the reader is pointing, from zero to one,
 * or null when they are not. The chart draws its own marker line — only it
 * knows where the line belongs among its own shapes — and this puts the
 * reading beside it, at the top, clamped so it never leaves the chart. A
 * caption below the chart would leave the reader to work out which moment it
 * described.
 */
@Composable
fun ChartOverlay(
    marker: Float?,
    modifier: Modifier = Modifier,
    tooltip: @Composable (() -> Unit)? = null,
    chart: @Composable () -> Unit,
) {
    /** The tooltip's own width, so it can be kept inside the chart. */
    var tipWidth by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = constraints.maxWidth.toFloat()
        chart()

        if (marker != null && tooltip != null) {
            Box(
                Modifier
                    .offset {
                        val wanted = marker * width - tipWidth / 2f
                        IntOffset(
                            x = wanted
                                .coerceIn(0f, (width - tipWidth).coerceAtLeast(0f))
                                .roundToInt(),
                            y = 0,
                        )
                    }
                    .onSizeChanged { tipWidth = it.width },
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp,
                ) {
                    Box(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        tooltip()
                    }
                }
            }
        }
    }
}
