package hu.galambos.healthy.ui.scale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import hu.galambos.healthy.data.scale.ScaleAvailability
import hu.galambos.healthy.data.scale.ScaleRecorder
import hu.galambos.healthy.data.scale.ScaleScanner
import hu.galambos.healthy.data.settings.SettingsStore
import hu.galambos.healthy.domain.scale.ScaleReading
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScaleState(
    val availability: ScaleAvailability = ScaleAvailability.PermissionMissing,
    val listening: Boolean = false,
    /** The weight as it settles, so the screen can show something happening. */
    val live: ScaleReading? = null,
    val recorded: ScaleReading? = null,
)

/**
 * Listening for the scale, and only while asked to.
 *
 * The scan runs in the foreground and stops the moment a measurement completes
 * or the screen is left. A background service would catch weigh-ins the app
 * missed, at the cost of a permanent notification and a radio that never
 * sleeps — not a trade worth making before this has been proven to work at
 * all.
 */
class ScaleViewModel(
    private val scanner: ScaleScanner,
    private val recorder: ScaleRecorder,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ScaleState(scanner.availability()))
    val state: StateFlow<ScaleState> = _state.asStateFlow()

    private var listenJob: Job? = null

    fun refreshAvailability() {
        _state.update { it.copy(availability = scanner.availability()) }
    }

    fun startListening() {
        refreshAvailability()
        if (_state.value.availability != ScaleAvailability.Ready) return

        listenJob?.cancel()
        _state.update { it.copy(listening = true, live = null, recorded = null) }
        listenJob = viewModelScope.launch {
            scanner.readings().collect { reading ->
                _state.update { it.copy(live = reading) }

                // A weigh-in is finished when the weight has settled and the
                // impedance has arrived. Anything earlier is a number on its
                // way somewhere.
                if (reading.isComplete) {
                    recorder.record(reading, settings.settings.first())
                    _state.update { it.copy(recorded = reading, listening = false) }
                    stopListening()
                }
            }
        }
    }

    fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        _state.update { it.copy(listening = false) }
    }

    /**
     * The stored measurements are raw; the numbers shown come from them plus
     * the profile. Change the profile and the history has to be worked out
     * again, or it stays computed for somebody else.
     */
    fun recomputeFromProfile() {
        viewModelScope.launch { recorder.recomputeAll(settings.settings.first()) }
    }

    override fun onCleared() {
        // The radio must not keep scanning once nothing is watching.
        stopListening()
    }

    companion object {
        fun factory(
            scanner: ScaleScanner,
            recorder: ScaleRecorder,
            settings: SettingsStore,
        ) = viewModelFactory {
            initializer { ScaleViewModel(scanner, recorder, settings) }
        }
    }
}
