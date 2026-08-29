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

    init {
        // Rows recorded while the completion rule was too loose are not
        // measurements — a weight caught mid-step is not a body — so they are
        // cleared once, and what remains is worked out again.
        viewModelScope.launch { recorder.discardIncomplete(settings.settings.first()) }
    }

    fun refreshAvailability() {
        _state.update { it.copy(availability = scanner.availability()) }
    }

    fun startListening() {
        refreshAvailability()
        if (_state.value.availability != ScaleAvailability.Ready) return
        if (listenJob?.isActive == true) return

        listenJob?.cancel()
        _state.update { it.copy(listening = true, live = null, recorded = null) }
        listenJob = viewModelScope.launch {
            scanner.readings().collect { reading ->
                _state.update { it.copy(live = reading) }

                // The impedance is what marks the end of a weigh-in, and
                // nothing else does. The "stabilised" flag is set on
                // transitional readings too: loosening the rule to trust it
                // recorded twenty-three rows from one weigh-in, ending with
                // 3.25 kg — the instant of stepping off, filed as a body
                // weight. The impedance appeared exactly twice in that run,
                // both times on the real 95.1 kg reading.
                if (reading.isComplete) {
                    // Storing is keyed by the measurement's own timestamp, so
                    // the scale repeating its last result costs nothing and a
                    // second weigh-in is a second record.
                    recorder.record(reading, settings.settings.first())
                    _state.update { it.copy(recorded = reading) }
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
