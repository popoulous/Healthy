package hu.galambos.healthy.ui.trends

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.R
import hu.galambos.healthy.data.fake.FakeHealthRepository
import hu.galambos.healthy.domain.metric.MetricDescriptor
import hu.galambos.healthy.domain.metric.MetricId
import hu.galambos.healthy.domain.metric.MetricRegistry
import hu.galambos.healthy.domain.summary.LoadState
import hu.galambos.healthy.domain.summary.MetricSummary
import hu.galambos.healthy.domain.summary.TrendWindow
import hu.galambos.healthy.domain.summary.stats
import hu.galambos.healthy.ui.components.MessageScreen
import hu.galambos.healthy.ui.components.Sparkline
import hu.galambos.healthy.ui.components.sparklineStyleFor
import hu.galambos.healthy.ui.format.formatValue
import hu.galambos.healthy.ui.overview.DashboardState
import hu.galambos.healthy.ui.theme.HealthyTheme
import hu.galambos.healthy.ui.theme.colorOf
import kotlin.math.abs

/**
 * Every metric that has data, one per row, so they can be compared at a
 * glance. Not deep analysis — the detail screen is one tap away for that.
 */
@Composable
fun TrendsScreen(
    state: DashboardState,
    modifier: Modifier = Modifier,
    onMetricClick: (MetricId) -> Unit = {},
) {
    val withData = MetricRegistry.all.filter {
        state.summaryFor(it.id).state == LoadState.Loaded
    }

    if (withData.isEmpty()) {
        MessageScreen(
            title = stringResource(R.string.trends_title),
            body = stringResource(R.string.trends_empty),
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.trends_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        items(withData, key = { it.id.name }) { descriptor: MetricDescriptor ->
            TrendRow(
                descriptor = descriptor,
                summary = state.summaryFor(descriptor.id),
                onClick = { onMetricClick(descriptor.id) },
            )
        }
    }
}

@Composable
private fun TrendRow(
    descriptor: MetricDescriptor,
    summary: MetricSummary,
    onClick: () -> Unit,
) {
    val stats = summary.trend.stats()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(descriptor.titleRes),
                    style = MaterialTheme.typography.bodyMedium,
                )
                summary.latest?.let { latest ->
                    val formatted = formatValue(latest.value, descriptor.unit)
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = formatted.number,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (formatted.unit.isNotEmpty()) {
                            Text(
                                text = formatted.unit,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                stats?.deltaFromAverage?.let { delta ->
                    val formatted = formatValue(abs(delta), descriptor.unit)
                    Text(
                        text = (if (delta >= 0) "↑ " else "↓ ") + formatted.number,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Sparkline(
                buckets = summary.trend,
                color = colorOf(descriptor.accent),
                style = sparklineStyleFor(descriptor.unit),
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun TrendsPreview() {
    val fake = FakeHealthRepository()
    HealthyTheme {
        TrendsScreen(
            state = DashboardState(
                summaries = MetricRegistry.all.associate {
                    it.id to fake.summaryOf(it, TrendWindow.Week)
                },
            ),
        )
    }
}
