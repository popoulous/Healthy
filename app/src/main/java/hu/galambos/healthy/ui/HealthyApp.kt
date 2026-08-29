package hu.galambos.healthy.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import hu.galambos.healthy.ui.navigation.Destination
import hu.galambos.healthy.ui.overview.OverviewScreen
import hu.galambos.healthy.ui.settings.SettingsScreen
import hu.galambos.healthy.ui.sources.SourcesScreen
import hu.galambos.healthy.ui.theme.HealthyTheme
import hu.galambos.healthy.ui.trends.TrendsScreen

@Composable
fun HealthyApp() {
    var selected by rememberSaveable { mutableStateOf(Destination.Overview) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    val label = stringResource(destination.labelRes)
                    NavigationBarItem(
                        selected = destination == selected,
                        onClick = { selected = destination },
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
        when (selected) {
            Destination.Overview -> OverviewScreen(contentModifier)
            Destination.Trends -> TrendsScreen(contentModifier)
            Destination.Sources -> SourcesScreen(contentModifier)
            Destination.Settings -> SettingsScreen(contentModifier)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HealthyAppPreview() {
    HealthyTheme { HealthyApp() }
}
