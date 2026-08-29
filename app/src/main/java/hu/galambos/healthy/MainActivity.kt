package hu.galambos.healthy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.galambos.healthy.data.health.HealthConnectRepository
import hu.galambos.healthy.ui.AppViewModel
import hu.galambos.healthy.ui.HealthyRoot
import hu.galambos.healthy.ui.overview.DashboardViewModel
import hu.galambos.healthy.ui.theme.HealthyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Wired by hand. One repository and one view model do not justify a
        // dependency injection framework.
        val repository = HealthConnectRepository(applicationContext)

        setContent {
            HealthyTheme {
                HealthyRoot(
                    viewModel = viewModel(factory = AppViewModel.factory(repository)),
                    dashboardViewModel = viewModel(
                        factory = DashboardViewModel.factory(repository),
                    ),
                )
            }
        }
    }
}
