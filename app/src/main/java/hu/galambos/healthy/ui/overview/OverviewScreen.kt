package hu.galambos.healthy.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.ui.components.PlaceholderScreen
import hu.galambos.healthy.ui.theme.HealthyTheme
import hu.galambos.healthy.ui.theme.AccentColors

@Composable
fun OverviewScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Overview",
        description = "The dashboard lands here in F3. Until then these are the " +
            "six metric accents, so the palette can be checked on the real screen.",
        modifier = modifier,
    ) {
        AccentStrip()
    }
}

/** The six metric accents, shown to verify the palette in both themes. */
@Composable
private fun AccentStrip() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Metric accents", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                AccentColors.Steps,
                AccentColors.Heart,
                AccentColors.Sleep,
                AccentColors.Oxygen,
                AccentColors.Calories,
                AccentColors.Weight,
            ).forEach { accent ->
                Column(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent),
                ) {}
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OverviewScreenPreview() {
    HealthyTheme { OverviewScreen() }
}
