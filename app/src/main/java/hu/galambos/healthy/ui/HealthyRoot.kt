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
import hu.galambos.healthy.ui.onboarding.OnboardingScreen
import hu.galambos.healthy.ui.overview.DashboardViewModel
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
    modifier: Modifier = Modifier,
) {
    val access by viewModel.access.collectAsStateWithLifecycle()
    val dashboard by dashboardViewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAccess()
        // Throttled inside; coming back from Mi Fitness after a sync is the
        // normal way new data arrives.
        dashboardViewModel.load()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        // The result set is ignored on purpose: the repository is the single
        // source of truth for what is granted, and a partial grant is normal.
        viewModel.refreshAccess()
        dashboardViewModel.load(force = true)
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
        )
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
