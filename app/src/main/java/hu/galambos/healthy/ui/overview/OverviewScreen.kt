package hu.galambos.healthy.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.R
import hu.galambos.healthy.data.fake.FakeHealthRepository
import hu.galambos.healthy.domain.metric.MetricDescriptor
import hu.galambos.healthy.domain.metric.MetricRegistry
import hu.galambos.healthy.domain.summary.TrendWindow
import hu.galambos.healthy.ui.components.MetricCard
import hu.galambos.healthy.ui.theme.HealthyTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun OverviewScreen(
    state: DashboardState,
    onWindowChange: (TrendWindow) -> Unit,
    modifier: Modifier = Modifier,
    onGrantRequested: (() -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Header()
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            WindowSelector(state.window, onWindowChange)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.overview_metrics),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        items(MetricRegistry.all, key = { it.id.name }) { descriptor: MetricDescriptor ->
            MetricCard(
                descriptor = descriptor,
                summary = state.summaryFor(descriptor.id),
                modifier = Modifier.fillMaxWidth(),
                onGrantRequested = onGrantRequested,
            )
        }
    }
}

@Composable
private fun Header() {
    val now = LocalTime.now()
    val greeting = when {
        now.hour < 11 -> R.string.greeting_morning
        now.hour < 18 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(greeting),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(LocalDate.now()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun OverviewPreview() {
    val fake = FakeHealthRepository()
    val state = DashboardState(
        summaries = MetricRegistry.all.associate {
            it.id to fake.summaryOf(it, TrendWindow.Week)
        },
    )
    HealthyTheme {
        OverviewScreen(state = state, onWindowChange = {})
    }
}
