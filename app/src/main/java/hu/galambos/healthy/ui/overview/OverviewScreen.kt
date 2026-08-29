package hu.galambos.healthy.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.R
import hu.galambos.healthy.data.fake.FakeHealthRepository
import hu.galambos.healthy.domain.metric.MetricCategory
import hu.galambos.healthy.domain.metric.MetricDescriptor
import hu.galambos.healthy.domain.metric.MetricId
import hu.galambos.healthy.domain.metric.MetricRegistry
import hu.galambos.healthy.domain.summary.LoadState
import hu.galambos.healthy.domain.summary.TrendWindow
import hu.galambos.healthy.ui.components.MetricCard
import hu.galambos.healthy.ui.components.SummaryTile
import hu.galambos.healthy.ui.settings.LocalSettings
import hu.galambos.healthy.ui.theme.HealthyTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    state: DashboardState,
    onWindowChange: (TrendWindow) -> Unit,
    modifier: Modifier = Modifier,
    onGrantRequested: (() -> Unit)? = null,
    onMetricClick: (MetricId) -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    // Most of the thirty-odd types are empty on any given phone. Hiding them
    // keeps the dashboard the design asked for; the toggle is there because
    // "what does this phone not have" is a real question too.
    var showEmpty by rememberSaveable { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        fullWidth { Header() }
        fullWidth { WindowSelector(state.window, onWindowChange) }

        fullWidth {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricRegistry.headline.forEach { descriptor ->
                    SummaryTile(
                        descriptor = descriptor,
                        summary = state.summaryFor(descriptor.id),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        MetricCategory.entries.forEach { category ->
            val descriptors = MetricRegistry.byCategory[category]
                .orEmpty()
                .filter { showEmpty || state.summaryFor(it.id).state != LoadState.Empty }
            if (descriptors.isEmpty()) return@forEach

            fullWidth {
                Text(
                    text = stringResource(category.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(descriptors, key = { it.id.name }) { descriptor: MetricDescriptor ->
                MetricCard(
                    descriptor = descriptor,
                    summary = state.summaryFor(descriptor.id),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onMetricClick(descriptor.id) },
                    onGrantRequested = onGrantRequested,
                )
            }
        }

        fullWidth {
            TextButton(onClick = { showEmpty = !showEmpty }) {
                Text(
                    stringResource(
                        if (showEmpty) R.string.overview_hide_empty else R.string.overview_show_empty,
                    ),
                )
            }
        }
    }
    }
}

/** Header rows and section titles span both columns. */
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.fullWidth(
    content: @Composable () -> Unit,
) = item(span = { GridItemSpan(maxLineSpan) }) { content() }

@Composable
private fun Header() {
    val greeting = when {
        LocalTime.now().hour < 11 -> R.string.greeting_morning
        LocalTime.now().hour < 18 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
    val name = LocalSettings.current.name
    val line = if (name.isBlank()) {
        stringResource(greeting)
    } else {
        stringResource(R.string.greeting_named, stringResource(greeting), name)
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = line, style = MaterialTheme.typography.headlineMedium)
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

@Preview(showBackground = true, heightDp = 1200)
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
