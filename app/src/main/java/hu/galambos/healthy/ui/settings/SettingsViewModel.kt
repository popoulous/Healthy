package hu.galambos.healthy.ui.settings

import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import hu.galambos.healthy.data.settings.DistanceUnit
import hu.galambos.healthy.data.settings.MassUnit
import hu.galambos.healthy.data.settings.Settings
import hu.galambos.healthy.data.settings.SettingsStore
import hu.galambos.healthy.data.settings.ThemeChoice
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Read wherever a value has to be shown in the unit the user picked. A
 * composition local rather than a parameter on every formatter, because the
 * alternative is threading preferences through every card on the screen.
 */
val LocalSettings = compositionLocalOf { Settings() }

class SettingsViewModel(private val store: SettingsStore) : ViewModel() {

    val settings: StateFlow<Settings> = store.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = Settings(),
    )

    fun setTheme(choice: ThemeChoice) = viewModelScope.launch { store.setTheme(choice) }

    fun setMassUnit(unit: MassUnit) = viewModelScope.launch { store.setMassUnit(unit) }

    fun setDistanceUnit(unit: DistanceUnit) = viewModelScope.launch { store.setDistanceUnit(unit) }

    fun setName(name: String) = viewModelScope.launch { store.setName(name) }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(store: SettingsStore) = viewModelFactory {
            initializer { SettingsViewModel(store) }
        }
    }
}
