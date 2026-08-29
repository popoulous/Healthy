package hu.galambos.healthy.ui.trends

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import hu.galambos.healthy.R
import hu.galambos.healthy.ui.components.MessageScreen

@Composable
fun TrendsScreen(modifier: Modifier = Modifier) {
    MessageScreen(
        title = stringResource(R.string.tab_trends),
        body = stringResource(R.string.coming_soon),
        modifier = modifier,
    )
}
