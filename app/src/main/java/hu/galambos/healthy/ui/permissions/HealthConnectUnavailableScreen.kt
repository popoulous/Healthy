package hu.galambos.healthy.ui.permissions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import hu.galambos.healthy.R
import hu.galambos.healthy.domain.HealthConnectAvailability
import hu.galambos.healthy.ui.components.MessageScreen
import hu.galambos.healthy.ui.theme.HealthyTheme

private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

/**
 * Reachable only before Android 14, where Health Connect is an app the user
 * may not have installed or may not have updated. From Android 14 it is part
 * of the platform — but the older phone this app also runs on is exactly the
 * case this screen exists for.
 */
@Composable
fun HealthConnectUnavailableScreen(
    availability: HealthConnectAvailability,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var storeMissing by remember { mutableStateOf(false) }
    val updating = availability == HealthConnectAvailability.UpdateRequired

    MessageScreen(
        title = stringResource(
            if (updating) R.string.update_required_title else R.string.unavailable_title,
        ),
        body = stringResource(
            if (updating) R.string.update_required_body else R.string.unavailable_body,
        ),
        modifier = modifier,
    ) {
        Button(
            onClick = { storeMissing = !openHealthConnectListing(context) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text(
                stringResource(
                    if (updating) R.string.update_required_update else R.string.unavailable_install,
                ),
            )
        }
        TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.retry))
        }
        if (storeMissing) {
            Text(
                text = stringResource(R.string.unavailable_no_store),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Opens the store listing, falling back to the web listing on devices without
 * the Play app. Returns false when neither opens, so the screen can say so
 * instead of appearing to do nothing.
 */
private fun openHealthConnectListing(context: Context): Boolean {
    val targets = listOf(
        "market://details?id=$HEALTH_CONNECT_PACKAGE",
        "https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE",
    )
    for (target in targets) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, target.toUri()))
            return true
        } catch (_: ActivityNotFoundException) {
            // Try the next one.
        }
    }
    return false
}

@Preview(showBackground = true)
@Composable
private fun UnavailablePreview() {
    HealthyTheme {
        HealthConnectUnavailableScreen(HealthConnectAvailability.UpdateRequired, onRetry = {})
    }
}
