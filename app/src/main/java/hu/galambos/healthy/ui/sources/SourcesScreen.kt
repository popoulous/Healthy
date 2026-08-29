package hu.galambos.healthy.ui.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.R
import hu.galambos.healthy.data.fake.FakeHealthRepository
import hu.galambos.healthy.domain.metric.MetricRegistry
import hu.galambos.healthy.domain.summary.LoadState
import hu.galambos.healthy.domain.summary.TrendWindow
import hu.galambos.healthy.ui.components.MessageScreen
import hu.galambos.healthy.ui.components.rememberSourceLabel
import hu.galambos.healthy.ui.format.formatTimestamp
import hu.galambos.healthy.ui.overview.DashboardState
import hu.galambos.healthy.ui.theme.HealthyTheme
import hu.galambos.healthy.ui.theme.colorOf
import java.time.Instant

/**
 * Which app wrote what.
 *
 * This is the screen that answers the question the whole app exists around:
 * why is a metric missing? Not because Healthy cannot read it, but because
 * whichever app owns that sensor never shared it. The answer can only be built
 * from what was actually read — Health Connect will not say in advance which
 * app writes which type.
 */
@Composable
fun SourcesScreen(
    state: DashboardState,
    modifier: Modifier = Modifier,
) {
    val bySource = MetricRegistry.all
        .mapNotNull { descriptor ->
            val summary = state.summaryFor(descriptor.id)
            val latest = summary.latest?.takeIf { summary.state == LoadState.Loaded }
            latest?.let { descriptor to it }
        }
        .groupBy({ it.second.sourcePackage }, { it })

    if (bySource.isEmpty()) {
        MessageScreen(
            title = stringResource(R.string.tab_sources),
            body = stringResource(R.string.sources_empty),
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.tab_sources),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.sources_explanation),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        bySource.forEach { (packageName, entries) ->
            item(key = packageName) {
                SourceCard(packageName, entries.map { it.first to it.second.time })
            }
        }
    }
}

@Composable
private fun SourceCard(
    packageName: String,
    entries: List<Pair<hu.galambos.healthy.domain.metric.MetricDescriptor, Instant>>,
) {
    val newest = entries.maxOf { it.second }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = rememberSourceLabel(packageName),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.sources_last_write, formatTimestamp(newest)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                entries.sortedBy { it.first.id.ordinal }.forEach { (descriptor, _) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colorOf(descriptor.accent)),
                        )
                        Text(
                            text = stringResource(descriptor.titleRes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SourcesPreview() {
    val fake = FakeHealthRepository()
    HealthyTheme {
        SourcesScreen(
            state = DashboardState(
                summaries = MetricRegistry.all.associate {
                    it.id to fake.summaryOf(it, TrendWindow.Week)
                },
            ),
        )
    }
}
