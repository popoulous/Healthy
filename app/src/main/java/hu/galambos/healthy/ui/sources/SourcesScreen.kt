package hu.galambos.healthy.ui.sources

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import hu.galambos.healthy.ui.components.PlaceholderScreen

@Composable
fun SourcesScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Sources",
        description = "Which app wrote what into Health Connect — the screen " +
            "that answers why a metric is missing. Arrives in F5.",
        modifier = modifier,
    )
}
