package hu.galambos.healthy.ui.scale

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.R
import hu.galambos.healthy.data.scale.BLUETOOTH_SCAN_PERMISSION
import hu.galambos.healthy.data.scale.ScaleAvailability
import hu.galambos.healthy.domain.metric.MetricUnit
import hu.galambos.healthy.ui.format.formatValue
import hu.galambos.healthy.ui.theme.HealthyTheme

/**
 * The scale, in the settings screen.
 *
 * It reads as a deliberate act — press, step on, done — rather than as
 * something the app is quietly doing in the background, which is exactly what
 * it is.
 */
@Composable
fun ScaleSection(
    state: ScaleState,
    profileComplete: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onPermissionGranted() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (state.availability) {
            ScaleAvailability.NeedsAndroid12 -> Explanation(R.string.scale_needs_android12)
            ScaleAvailability.NoBluetoothHardware -> Explanation(R.string.scale_no_bluetooth)
            ScaleAvailability.BluetoothOff -> Explanation(R.string.scale_bluetooth_off)

            ScaleAvailability.PermissionMissing -> {
                Explanation(R.string.scale_permission)
                OutlinedButton(
                    onClick = { permissionLauncher.launch(BLUETOOTH_SCAN_PERMISSION) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.scale_grant))
                }
            }

            ScaleAvailability.Ready -> Ready(state, profileComplete, onStart, onStop)
        }
    }
}

@Composable
private fun Ready(
    state: ScaleState,
    profileComplete: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    if (!profileComplete) {
        // Without a height and a birth year there is no formula, only a
        // weight. Saying so beats showing a body fat figure derived from a
        // guess.
        Explanation(R.string.scale_profile_missing)
    }

    when {
        state.listening -> {
            Text(
                text = stringResource(R.string.scale_step_on),
                style = MaterialTheme.typography.bodyMedium,
            )
            state.live?.let { live ->
                val weight = formatValue(live.weightKg, MetricUnit.Kilograms)
                Text(
                    text = "${weight.number} ${weight.unit}",
                    style = MaterialTheme.typography.displaySmall,
                )
            }
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.scale_stop))
            }
        }

        state.recorded != null -> {
            val weight = formatValue(state.recorded.weightKg, MetricUnit.Kilograms)
            Text(
                text = "${weight.number} ${weight.unit}",
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = stringResource(R.string.scale_recorded),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.scale_start))
            }
        }

        else -> OutlinedButton(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.scale_start))
        }
    }
}

@Composable
private fun Explanation(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ScaleSectionPreview() {
    HealthyTheme {
        ScaleSection(
            state = ScaleState(availability = ScaleAvailability.Ready),
            profileComplete = true,
            onStart = {},
            onStop = {},
            onPermissionGranted = {},
        )
    }
}
