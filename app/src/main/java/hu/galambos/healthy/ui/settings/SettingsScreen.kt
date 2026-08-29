package hu.galambos.healthy.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.galambos.healthy.BuildConfig
import hu.galambos.healthy.R
import hu.galambos.healthy.data.settings.DistanceUnit
import hu.galambos.healthy.data.settings.MassUnit
import hu.galambos.healthy.data.settings.Settings
import hu.galambos.healthy.data.settings.ThemeChoice
import hu.galambos.healthy.domain.HistoryAccess
import hu.galambos.healthy.ui.theme.HealthyTheme

@Composable
fun SettingsScreen(
    settings: Settings,
    historyAccess: HistoryAccess,
    onThemeChange: (ThemeChoice) -> Unit,
    onMassChange: (MassUnit) -> Unit,
    onDistanceChange: (DistanceUnit) -> Unit,
    onNameChange: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.tab_settings),
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        item {
            Section(stringResource(R.string.settings_appearance)) {
                ChipRow(
                    options = ThemeChoice.entries,
                    selected = settings.theme,
                    label = {
                        stringResource(
                            when (it) {
                                ThemeChoice.System -> R.string.settings_theme_system
                                ThemeChoice.Light -> R.string.settings_theme_light
                                ThemeChoice.Dark -> R.string.settings_theme_dark
                            },
                        )
                    },
                    onSelect = onThemeChange,
                )
            }
        }

        item {
            Section(stringResource(R.string.settings_units)) {
                ChipRow(
                    options = MassUnit.entries,
                    selected = settings.mass,
                    label = {
                        stringResource(
                            when (it) {
                                MassUnit.Kilograms -> R.string.unit_kg
                                MassUnit.Pounds -> R.string.settings_unit_lb
                            },
                        )
                    },
                    onSelect = onMassChange,
                )
                ChipRow(
                    options = DistanceUnit.entries,
                    selected = settings.distance,
                    label = {
                        stringResource(
                            when (it) {
                                DistanceUnit.Kilometres -> R.string.unit_km
                                DistanceUnit.Miles -> R.string.settings_unit_mi
                            },
                        )
                    },
                    onSelect = onDistanceChange,
                )
            }
        }

        item {
            Section(stringResource(R.string.settings_greeting)) {
                OutlinedTextField(
                    value = settings.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.settings_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Health Connect holds no name, so there is nowhere else this
                // could come from. Left blank, the greeting simply has none.
                Text(
                    text = stringResource(R.string.settings_name_explanation),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Section(stringResource(R.string.settings_data)) {
                OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_refresh))
                }
                OutlinedButton(
                    onClick = { openHealthConnectSettings(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_permissions))
                }
                // An app cannot toggle its own health permissions; only Health
                // Connect can. Saying so beats a switch that does nothing.
                Text(
                    text = stringResource(
                        when (historyAccess) {
                            HistoryAccess.Granted -> R.string.settings_history_granted
                            HistoryAccess.NotGranted -> R.string.settings_history_not_granted
                            HistoryAccess.Unsupported -> R.string.settings_history_unsupported
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Section(stringResource(R.string.settings_about)) {
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.rationale_body),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

/**
 * Health Connect's own settings, which is the only place these permissions can
 * actually be changed.
 */
private fun openHealthConnectSettings(context: Context) {
    val intents = listOf(
        Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"),
        Intent(Intent.ACTION_VIEW).setPackage("com.google.android.apps.healthdata"),
    )
    for (intent in intents) {
        try {
            context.startActivity(intent)
            return
        } catch (_: ActivityNotFoundException) {
            // Try the next one.
        }
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun SettingsPreview() {
    HealthyTheme {
        SettingsScreen(
            settings = Settings(name = "Tamás"),
            historyAccess = HistoryAccess.Granted,
            onThemeChange = {},
            onMassChange = {},
            onDistanceChange = {},
            onNameChange = {},
            onRefresh = {},
        )
    }
}
