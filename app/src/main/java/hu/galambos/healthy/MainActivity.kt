package hu.galambos.healthy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import hu.galambos.healthy.ui.HealthyApp
import hu.galambos.healthy.ui.theme.HealthyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HealthyTheme {
                HealthyApp()
            }
        }
    }
}
