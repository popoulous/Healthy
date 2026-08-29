package hu.galambos.healthy.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.R
import hu.galambos.healthy.data.fake.FakeHealthRepository
import hu.galambos.healthy.domain.metric.MetricId
import hu.galambos.healthy.domain.metric.MetricRegistry
import hu.galambos.healthy.domain.summary.TrendWindow
import hu.galambos.healthy.ui.detail.MetricDetailScreen
import hu.galambos.healthy.ui.navigation.Destination
import hu.galambos.healthy.ui.overview.DashboardState
import hu.galambos.healthy.ui.overview.OverviewScreen
import hu.galambos.healthy.ui.settings.SettingsScreen
import hu.galambos.healthy.ui.sources.SourcesScreen
import hu.galambos.healthy.ui.theme.HealthyTheme
import hu.galambos.healthy.ui.trends.TrendsScreen

@Composable
fun HealthyApp(
    dashboard: DashboardState,
    onWindowChange: (TrendWindow) -> Unit,
    modifier: Modifier = Modifier,
    onGrantRequested: (() -> Unit)? = null,
    onDetailOpened: (MetricId) -> Unit = {},
) {
    var selected by rememberSaveable { mutableStateOf(Destination.Overview) }

    /**
     * One level of detail over four flat tabs does not need a navigation
     * graph. A nullable id and the system back button cover it; the graph
     * would arrive with the second level, if there ever is one.
     */
    var detail by rememberSaveable { mutableStateOf<MetricId?>(null) }

    BackHandler(enabled = detail != null) { detail = null }

    LaunchedEffect(detail) {
        detail?.let(onDetailOpened)
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    val label = stringResource(destination.labelRes)
                    NavigationBarItem(
                        selected = destination == selected,
                        onClick = {
                            detail = null
                            selected = destination
                        },
                        icon = {
                            Icon(
                                painter = painterResource(destination.iconRes),
                                contentDescription = null,
                            )
                        },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        val openDetail: (MetricId) -> Unit = { detail = it }

        val current = detail
        if (current != null) {
            Column(contentModifier.fillMaxSize()) {
                TextButton(
                    onClick = { detail = null },
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text("← " + stringResource(R.string.back))
                }
                MetricDetailScreen(
                    descriptor = MetricRegistry[current],
                    summary = dashboard.summaryFor(current),
                    window = dashboard.window,
                    onWindowChange = onWindowChange,
                    sleepNight = dashboard.sleepNight,
                )
            }
            return@Scaffold
        }

        when (selected) {
            Destination.Overview -> OverviewScreen(
                state = dashboard,
                onWindowChange = onWindowChange,
                modifier = contentModifier,
                onGrantRequested = onGrantRequested,
                onMetricClick = openDetail,
            )

            Destination.Trends -> TrendsScreen(
                state = dashboard,
                modifier = contentModifier,
                onMetricClick = openDetail,
            )

            Destination.Sources -> SourcesScreen(contentModifier)
            Destination.Settings -> SettingsScreen(contentModifier)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HealthyAppPreview() {
    val fake = FakeHealthRepository()
    HealthyTheme {
        HealthyApp(
            dashboard = DashboardState(
                summaries = MetricRegistry.all.associate {
                    it.id to fake.summaryOf(it, TrendWindow.Week)
                },
                sleepNight = fake.sleepNight(),
            ),
            onWindowChange = {},
        )
    }
}
