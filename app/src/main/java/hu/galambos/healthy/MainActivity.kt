package hu.galambos.healthy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.galambos.healthy.data.health.HealthConnectRepository
import hu.galambos.healthy.data.local.HealthyDatabase
import hu.galambos.healthy.data.local.MetricStore
import hu.galambos.healthy.data.sync.HealthSync
import hu.galambos.healthy.data.settings.SettingsStore
import hu.galambos.healthy.data.settings.ThemeChoice
import hu.galambos.healthy.ui.AppViewModel
import hu.galambos.healthy.ui.HealthyRoot
import hu.galambos.healthy.ui.overview.DashboardViewModel
import hu.galambos.healthy.ui.settings.LocalSettings
import hu.galambos.healthy.ui.settings.SettingsViewModel
import hu.galambos.healthy.ui.theme.HealthyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Wired by hand. Two repositories and three view models do not justify
        // a dependency injection framework.
        val repository = HealthConnectRepository(applicationContext)
        val settingsStore = SettingsStore(applicationContext)
        val metricStore = MetricStore(HealthyDatabase.get(applicationContext))
        val sync = HealthSync(repository, metricStore)

        setContent {
            val settingsViewModel: SettingsViewModel =
                viewModel(factory = SettingsViewModel.factory(settingsStore))
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            HealthyTheme(
                darkTheme = when (settings.theme) {
                    ThemeChoice.System -> isSystemInDarkTheme()
                    ThemeChoice.Light -> false
                    ThemeChoice.Dark -> true
                },
            ) {
                // Units are chosen once and read wherever a value is formatted.
                CompositionLocalProvider(LocalSettings provides settings) {
                    HealthyRoot(
                        viewModel = viewModel(factory = AppViewModel.factory(repository)),
                        dashboardViewModel = viewModel(
                            factory = DashboardViewModel.factory(repository, metricStore, sync),
                        ),
                        settingsViewModel = settingsViewModel,
                    )
                }
            }
        }
    }
}
