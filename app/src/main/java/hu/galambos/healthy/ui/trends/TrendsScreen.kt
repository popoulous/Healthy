package hu.galambos.healthy.ui.trends

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import hu.galambos.healthy.ui.components.PlaceholderScreen

@Composable
fun TrendsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Trends",
        description = "Every metric side by side over 7 or 30 days, for quick " +
            "comparison rather than deep analysis. Arrives in F4.",
        modifier = modifier,
    )
}
