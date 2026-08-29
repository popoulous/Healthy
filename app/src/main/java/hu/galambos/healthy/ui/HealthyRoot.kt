package hu.galambos.healthy.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.galambos.healthy.domain.HealthConnectAvailability
import hu.galambos.healthy.domain.metric.MetricId
import hu.galambos.healthy.ui.onboarding.OnboardingScreen
import hu.galambos.healthy.ui.overview.DashboardViewModel
import hu.galambos.healthy.ui.scale.ScaleViewModel
import hu.galambos.healthy.ui.settings.SettingsViewModel
import hu.galambos.healthy.ui.permissions.HealthConnectUnavailableScreen

/**
 * Decides which of the three states the app is in before anything is drawn:
 * Health Connect missing, no permissions yet, or ready.
 *
 * Access is re-checked on every resume rather than only at launch, because
 * granting and revoking happens in Health Connect's own screens — the user
 * leaves, changes something, and comes back expecting the app to know.
 */
@Composable
fun HealthyRoot(
    viewModel: AppViewModel,
    dashboardViewModel: DashboardViewModel,
    settingsViewModel: SettingsViewModel,
    scaleViewModel: ScaleViewModel,
    modifier: Modifier = Modifier,
) {
    val access by viewModel.access.collectAsStateWithLifecycle()
    val dashboard by dashboardViewModel.state.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val scale by scaleViewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAccess()
        // Throttled inside; coming back from Mi Fitness after a sync is the
        // normal way new data arrives.
        dashboardViewModel.refresh()
        // Bluetooth can be switched off while the app is away, and the answer
        // is only true at the moment it is asked.
        scaleViewModel.refreshAvailability()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        // The result set is ignored on purpose: the repository is the single
        // source of truth for what is granted, and a partial grant is normal.
        viewModel.refreshAccess()
        dashboardViewModel.refresh(force = true)
    }

    // Only HealthyApp brings a Scaffold, which handles its own insets. The
    // screens before it are bare, so they need the padding here or they draw
    // under the status bar.
    val inset = modifier.windowInsetsPadding(WindowInsets.safeDrawing)

    when {
        !access.checked -> LoadingScreen(inset)

        access.availability != HealthConnectAvailability.Available ->
            HealthConnectUnavailableScreen(
                availability = access.availability,
                onRetry = viewModel::refreshAccess,
                modifier = inset,
            )

        !access.hasAnyPermission ->
            OnboardingScreen(
                historyAccess = access.historyAccess,
                onGrantRequested = { permissionLauncher.launch(access.required) },
                modifier = inset,
            )

        else -> HealthyApp(
            dashboard = dashboard,
            onWindowChange = dashboardViewModel::setWindow,
            modifier = modifier,
            onGrantRequested = { permissionLauncher.launch(access.required) },
            onDetailOpened = { id ->
                if (id == MetricId.Sleep) dashboardViewModel.loadSleepNight()
            },
            settings = settings,
            historyAccess = access.historyAccess,
            onThemeChange = { settingsViewModel.setTheme(it) },
            onMassChange = { settingsViewModel.setMassUnit(it) },
            onDistanceChange = { settingsViewModel.setDistanceUnit(it) },
            onNameChange = { settingsViewModel.setName(it) },
            onRefresh = { dashboardViewModel.refresh(force = true) },
            scaleState = scale,
            onHeightChange = {
                settingsViewModel.setHeightCm(it)
                scaleViewModel.recomputeFromProfile()
            },
            onBirthYearChange = {
                settingsViewModel.setBirthYear(it)
                scaleViewModel.recomputeFromProfile()
            },
            onSexChange = {
                settingsViewModel.setSex(it)
                scaleViewModel.recomputeFromProfile()
            },
            onScaleStart = scaleViewModel::startListening,
            onScaleStop = scaleViewModel::stopListening,
            onScalePermissionGranted = scaleViewModel::refreshAvailability,
        )
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
