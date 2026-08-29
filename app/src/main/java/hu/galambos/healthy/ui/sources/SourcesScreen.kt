package hu.galambos.healthy.ui.sources

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import hu.galambos.healthy.R
import hu.galambos.healthy.ui.components.MessageScreen

@Composable
fun SourcesScreen(modifier: Modifier = Modifier) {
    MessageScreen(
        title = stringResource(R.string.tab_sources),
        body = stringResource(R.string.coming_soon),
        modifier = modifier,
    )
}
