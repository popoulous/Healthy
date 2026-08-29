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
import androidx.compose.ui.res.stringResource
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
 * The quick-glance row at the top: three readings, no chart, no source. This
 * is the "what is today like" answer; the detail is a scroll away.
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricIcon(descriptor.id, accent, size = 28.dp)

            val latest = summary.latest
            if (summary.state == LoadState.Loaded && latest != null) {
                val formatted = formatValue(latest.value, descriptor.unit)
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = formatted.number,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                    if (formatted.unit.isNotEmpty()) {
                        Text(
                            text = formatted.unit,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = stringResource(descriptor.titleRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            val delta = summary.trend.stats()?.deltaFromAverage
            if (delta != null) {
                val formatted = formatValue(abs(delta), descriptor.unit)
                Text(
                    text = (if (delta >= 0) "↑ " else "↓ ") + formatted.number,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

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
