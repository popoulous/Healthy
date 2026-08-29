package hu.galambos.healthy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.domain.metric.MetricDescriptor
import hu.galambos.healthy.domain.metric.MetricId
import hu.galambos.healthy.domain.metric.MetricRegistry
import hu.galambos.healthy.domain.summary.Bucket
import hu.galambos.healthy.domain.summary.DataPoint
import hu.galambos.healthy.domain.summary.LoadState
import hu.galambos.healthy.domain.summary.MetricSummary
import hu.galambos.healthy.domain.summary.stats
import hu.galambos.healthy.ui.format.formatValue
import hu.galambos.healthy.ui.theme.HealthyTheme
import hu.galambos.healthy.ui.theme.colorOf
import java.time.Instant
import java.time.LocalDate
import kotlin.math.abs

/**
 * The quick-glance row: three readings, centred, with no chart and no source.
 *
 * The second line is the unit rather than the metric's name — the design puts
 * "lépés" under the number, not "Lépésszám", and with the mark above it the
 * name would only be saying the same thing a third time.
 */
@Composable
fun SummaryTile(
    descriptor: MetricDescriptor,
    summary: MetricSummary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val accent = colorOf(descriptor.accent)

    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MetricIcon(descriptor.id, accent, size = 22.dp)

            val latest = summary.latest
            if (summary.state == LoadState.Loaded && latest != null) {
                val formatted = formatValue(latest.value, descriptor.unit)
                Text(
                    text = formatted.number,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    text = formatted.unit.ifEmpty { stringResource(descriptor.titleRes) },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Delta(descriptor, summary)
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(descriptor.titleRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Coloured the way the design colours it — up in green, down in red. This is
 * the one place in the app that passes judgement on a number, and it is worth
 * knowing that it does: on a step count, down is red because the design says
 * so, not because a quiet day is a failure.
 */
@Composable
private fun Delta(descriptor: MetricDescriptor, summary: MetricSummary) {
    val delta = summary.trend.stats()?.deltaFromAverage ?: return
    val formatted = formatValue(abs(delta), descriptor.unit)
    val rising = delta >= 0
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = (if (rising) "+" else "−") + formatted.number,
            style = MaterialTheme.typography.labelSmall,
            color = if (rising) DeltaUp else DeltaDown,
            maxLines = 1,
        )
    }
}

private val DeltaUp = Color(0xFF2E7D32)
private val DeltaDown = Color(0xFFC62828)

@Preview(showBackground = true, widthDp = 130)
@Composable
private fun SummaryTilePreview() {
    val descriptor = MetricRegistry[MetricId.Steps]
    HealthyTheme {
        SummaryTile(
            descriptor = descriptor,
            summary = MetricSummary(
                id = descriptor.id,
                state = LoadState.Loaded,
                latest = DataPoint(8421.0, Instant.now(), "com.xiaomi.wearable"),
                trend = listOf(6200.0, 8100.0, 7400.0, 8421.0)
                    .mapIndexed { i, v -> Bucket(LocalDate.of(2026, 8, i + 1), v) },
            ),
        )
    }
}
