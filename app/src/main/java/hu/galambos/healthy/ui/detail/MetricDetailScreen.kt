package hu.galambos.healthy.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.R
import hu.galambos.healthy.data.fake.FakeHealthRepository
import hu.galambos.healthy.domain.metric.MetricDescriptor
import hu.galambos.healthy.domain.metric.MetricId
import hu.galambos.healthy.domain.metric.MetricRegistry
import hu.galambos.healthy.domain.sleep.SleepNight
import hu.galambos.healthy.domain.summary.LoadState
import hu.galambos.healthy.domain.summary.MetricSummary
import hu.galambos.healthy.domain.summary.TrendWindow
import hu.galambos.healthy.domain.summary.stats
import hu.galambos.healthy.ui.components.MessageScreen
import hu.galambos.healthy.ui.components.Sparkline
import hu.galambos.healthy.ui.components.rememberSourceLabel
import hu.galambos.healthy.ui.components.sparklineStyleFor
import hu.galambos.healthy.ui.format.formatTimestamp
import hu.galambos.healthy.ui.format.formatValue
import hu.galambos.healthy.ui.theme.HealthyTheme
import hu.galambos.healthy.ui.theme.colorOf

/**
 * One metric, larger: the reading, how it sits against the window's average,
 * the whole window drawn, and the numbers behind it. Sleep gets its own
 * section on top of that, because a duration is the least interesting thing
 * about a night.
 */
@Composable
fun MetricDetailScreen(
    descriptor: MetricDescriptor,
    summary: MetricSummary,
    window: TrendWindow,
    onWindowChange: (TrendWindow) -> Unit,
    sleepNight: SleepNight?,
    modifier: Modifier = Modifier,
) {
    if (summary.state != LoadState.Loaded) {
        MessageScreen(
            title = stringResource(descriptor.titleRes),
            body = stringResource(
                when (summary.state) {
                    LoadState.NotGranted -> R.string.card_not_granted
                    LoadState.Loading -> R.string.loading
                    else -> R.string.card_no_data_detail
                },
            ),
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Headline(descriptor, summary, window)
        WindowSelector(window, onWindowChange)

        Sparkline(
            buckets = summary.trend,
            color = colorOf(descriptor.accent),
            style = sparklineStyleFor(descriptor.unit),
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
        )

        Statistics(descriptor, summary)

        if (descriptor.id == MetricId.Sleep && sleepNight != null) {
            SleepSection(sleepNight)
        }

        summary.latest?.let { latest ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.detail_source),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = rememberSourceLabel(latest.sourcePackage),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = formatTimestamp(latest.time),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Headline(
    descriptor: MetricDescriptor,
    summary: MetricSummary,
    window: TrendWindow,
) {
    val latest = summary.latest ?: return
    val formatted = formatValue(latest.value, descriptor.unit)
    val stats = summary.trend.stats()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = formatted.number, style = MaterialTheme.typography.displaySmall)
            if (formatted.unit.isNotEmpty()) {
                Text(
                    text = formatted.unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        val delta = stats?.deltaFromAverage
        if (delta != null) {
            val formattedDelta = formatValue(kotlin.math.abs(delta), descriptor.unit)
            Text(
                text = stringResource(
                    if (delta >= 0) R.string.detail_above_average else R.string.detail_below_average,
                    formattedDelta.number,
                    window.days,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Statistics(descriptor: MetricDescriptor, summary: MetricSummary) {
    val stats = summary.trend.stats() ?: return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatColumn(stringResource(R.string.detail_min), stats.min, descriptor)
        StatColumn(stringResource(R.string.detail_average), stats.average, descriptor)
        StatColumn(stringResource(R.string.detail_max), stats.max, descriptor)
    }
    Text(
        text = pluralStringResource(
            R.plurals.detail_days_with_data,
            stats.daysWithData,
            stats.daysWithData,
        ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatColumn(label: String, value: Double, descriptor: MetricDescriptor) {
    val formatted = formatValue(value, descriptor.unit)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = formatted.number, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun WindowSelector(selected: TrendWindow, onChange: (TrendWindow) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TrendWindow.entries.forEach { window ->
            FilterChip(
                selected = window == selected,
                onClick = { onChange(window) },
                label = {
                    Text(
                        stringResource(
                            when (window) {
                                TrendWindow.Week -> R.string.window_week
                                TrendWindow.Month -> R.string.window_month
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun MetricDetailPreview() {
    val fake = FakeHealthRepository()
    val descriptor = MetricRegistry[MetricId.Sleep]
    HealthyTheme {
        MetricDetailScreen(
            descriptor = descriptor,
            summary = fake.summaryOf(descriptor, TrendWindow.Week),
            window = TrendWindow.Week,
            onWindowChange = {},
            sleepNight = fake.sleepNight(),
        )
    }
}
