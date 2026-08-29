package hu.galambos.healthy.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.R
import hu.galambos.healthy.domain.HistoryAccess
import hu.galambos.healthy.domain.metric.MetricAccent
import hu.galambos.healthy.domain.metric.MetricRegistry
import hu.galambos.healthy.ui.theme.HealthyTheme
import hu.galambos.healthy.ui.theme.colorOf

/**
 * Two screens before the permission dialog.
 *
 * This is not decoration. Health Connect's own screen lists a stack of
 * permissions and explains none of them, and an app that asks to read
 * everything owes the user a sentence about why before it does.
 */
@Composable
fun OnboardingScreen(
    historyAccess: HistoryAccess,
    onGrantRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPermissions by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (showPermissions) {
            PermissionsPage(
                historyAccess = historyAccess,
                onGrant = onGrantRequested,
                onBack = { showPermissions = false },
            )
        } else {
            IntroPage(onContinue = { showPermissions = true })
        }
    }
}

@Composable
private fun IntroPage(onContinue: () -> Unit) {
    Text(
        text = stringResource(R.string.onboarding_intro_title),
        style = MaterialTheme.typography.displaySmall,
    )
    Text(
        text = stringResource(R.string.onboarding_intro_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.size(8.dp))
    Promise(
        accent = MetricAccent.Steps,
        title = stringResource(R.string.onboarding_point_private),
        detail = stringResource(R.string.onboarding_point_private_detail),
    )
    Promise(
        accent = MetricAccent.Weight,
        title = stringResource(R.string.onboarding_point_device),
        detail = stringResource(R.string.onboarding_point_device_detail),
    )
    Promise(
        accent = MetricAccent.Sleep,
        title = stringResource(R.string.onboarding_point_read_only),
        detail = stringResource(R.string.onboarding_point_read_only_detail),
    )
    Spacer(Modifier.size(8.dp))
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.onboarding_continue))
    }
}

@Composable
private fun PermissionsPage(
    historyAccess: HistoryAccess,
    onGrant: () -> Unit,
    onBack: () -> Unit,
) {
    Text(
        text = stringResource(R.string.onboarding_permissions_title),
        style = MaterialTheme.typography.displaySmall,
    )
    Text(
        text = stringResource(R.string.onboarding_permissions_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    MetricChecklist()
    Text(
        text = stringResource(
            if (historyAccess == HistoryAccess.Unsupported) {
                R.string.onboarding_permissions_history_unsupported
            } else {
                R.string.onboarding_permissions_history
            },
        ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.size(8.dp))
    Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.onboarding_grant))
    }
    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.onboarding_back))
    }
}

@Composable
private fun MetricChecklist() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MetricRegistry.all.forEach { descriptor ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Dot(descriptor.accent)
                Text(
                    text = stringResource(descriptor.titleRes),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun Promise(accent: MetricAccent, title: String, detail: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Dot(accent, size = 12.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Dot(accent: MetricAccent, size: Dp = 8.dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = colorOf(accent),
    ) {}
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    HealthyTheme {
        OnboardingScreen(historyAccess = HistoryAccess.NotGranted, onGrantRequested = {})
    }
}
