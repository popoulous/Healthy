package hu.galambos.healthy.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import hu.galambos.healthy.ui.components.PlaceholderScreen

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Settings",
        description = "Theme, units, refresh, and a way into the Health Connect " +
            "permission screen. Arrives in F5.",
        modifier = modifier,
    )
}
