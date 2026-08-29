package hu.galambos.healthy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.R
import hu.galambos.healthy.domain.metric.MetricDescriptor
import hu.galambos.healthy.domain.metric.MetricRegistry
import hu.galambos.healthy.domain.metric.MetricUnit
import hu.galambos.healthy.domain.summary.Bucket
import hu.galambos.healthy.domain.summary.DataPoint
import hu.galambos.healthy.domain.summary.FailureReason
import hu.galambos.healthy.domain.summary.LoadState
import hu.galambos.healthy.domain.summary.MetricSummary
import hu.galambos.healthy.ui.format.formatTimestamp
import hu.galambos.healthy.ui.format.formatValue
import hu.galambos.healthy.ui.theme.HealthyTheme
import hu.galambos.healthy.ui.theme.colorOf
import java.time.Instant
import java.time.LocalDate

/**
 * One card, the same shape for all fifty-odd metrics: name, the reading in
 * large type, a mini trend, then when and from where. The accent colours the
 * dot and the chart only — a card whose whole background is red would shout
 * about a heart rate that is perfectly normal.
 */
@Composable
fun MetricCard(
    descriptor: MetricDescriptor,
    summary: MetricSummary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onGrantRequested: (() -> Unit)? = null,
) {
    val accent = colorOf(descriptor.accent)

    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
                Text(
                    text = stringResource(descriptor.titleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            when (val state = summary.state) {
                LoadState.Loading -> Placeholder(stringResource(R.string.card_no_data))
                LoadState.NotGranted -> NotGranted(onGrantRequested)
                LoadState.Empty -> Placeholder(stringResource(R.string.card_no_data_detail))
                is LoadState.Failed -> Placeholder(failureText(state.reason))
                LoadState.Loaded -> Loaded(descriptor, summary)
            }
        }
    }
}

@Composable
private fun Loaded(descriptor: MetricDescriptor, summary: MetricSummary) {
    val latest = summary.latest
    if (latest == null) {
        Placeholder(stringResource(R.string.card_no_data_detail))
        return
    }
    val formatted = formatValue(latest.value, descriptor.unit)

    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = formatted.number, style = MaterialTheme.typography.displaySmall)
        if (formatted.unit.isNotEmpty()) {
            Text(
                text = formatted.unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
    }

    Sparkline(
        buckets = summary.trend,
        color = colorOf(descriptor.accent),
        style = sparklineStyleFor(descriptor.unit),
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
    )

    Footer(latest)
}

@Composable
private fun Footer(latest: DataPoint) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = formatTimestamp(latest.time),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = rememberSourceLabel(latest.sourcePackage),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NotGranted(onGrantRequested: (() -> Unit)?) {
    Column {
        Text(
            text = stringResource(R.string.card_not_granted),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onGrantRequested != null) {
            TextButton(
                onClick = onGrantRequested,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Text(stringResource(R.string.card_allow))
            }
        }
    }
}

@Composable
private fun Placeholder(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun failureText(reason: FailureReason): String = stringResource(
    when (reason) {
        FailureReason.RateLimited -> R.string.card_rate_limited
        FailureReason.BeyondHistoryLimit -> R.string.card_history_limited
        FailureReason.Unknown -> R.string.card_failed
    },
)

/** Counted-up quantities read as bars; measured ones as a line. */
fun sparklineStyleFor(unit: MetricUnit): SparklineStyle = when (unit) {
    MetricUnit.Steps,
    MetricUnit.Count,
    MetricUnit.Kilocalories,
    MetricUnit.Hours,
    MetricUnit.Minutes,
    MetricUnit.Meters,
    MetricUnit.Kilometres,
    MetricUnit.Litres,
    -> SparklineStyle.Bars

    else -> SparklineStyle.Line
}

@Preview(showBackground = true, widthDp = 200)
@Composable
private fun MetricCardPreview() {
    val descriptor = MetricRegistry[hu.galambos.healthy.domain.metric.MetricId.Steps]
    HealthyTheme {
        MetricCard(
            descriptor = descriptor,
            summary = MetricSummary(
                id = descriptor.id,
                state = LoadState.Loaded,
                latest = DataPoint(8421.0, Instant.now(), "com.xiaomi.wearable"),
                trend = listOf(6200.0, 8100.0, null, 7400.0, 9100.0, 8421.0)
                    .mapIndexed { i, v -> Bucket(LocalDate.of(2026, 8, i + 1), v) },
            ),
        )
    }
}
