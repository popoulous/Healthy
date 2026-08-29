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

    /** The reading being watched to see whether the scale has finished with it. */
    private var pending: ScaleReading? = null
    private var pendingSince = 0L
    private var lastRecorded: java.time.LocalDateTime? = null

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

                if (shouldRecord(reading)) {
                    lastRecorded = reading.measuredAt
                    recorder.record(reading, settings.settings.first())
                    _state.update { it.copy(recorded = reading) }
                }
            }
        }
    }

    /**
     * Whether this advertisement is a finished weigh-in.
     *
     * Neither flag in the packet answers that. "Stabilised" is set on readings
     * that are still moving — trusting it filed the instant of stepping off,
     * 3.25 kg, as a body weight — and the documented impedance flag is never
     * set at all. But the scale's own timestamp does answer it: while someone
     * is getting on, it advances every second, and when the measurement is
     * done it freezes and the same packet repeats.
     *
     * So an impedance is taken as done immediately, since it only ever arrives
     * at the end. Otherwise the reading has to hold still — same timestamp,
     * repeated — which is what a weigh-in in socks looks like: a real weight,
     * no body composition.
     */
    private fun shouldRecord(reading: ScaleReading): Boolean {
        if (!reading.stabilised) {
            pending = null
            return false
        }
        if (reading.measuredAt == lastRecorded) return false
        if (reading.isComplete) return true

        val now = System.currentTimeMillis()
        if (pending?.measuredAt != reading.measuredAt) {
            pending = reading
            pendingSince = now
            return false
        }
        return now - pendingSince >= SETTLED_HOLD_MS
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
        /**
         * How long a reading must stop changing before it counts. The scale
         * re-broadcasts every second or two, so this is a couple of repeats.
         */
        private const val SETTLED_HOLD_MS = 2_500L

        fun factory(
            scanner: ScaleScanner,
            recorder: ScaleRecorder,
            settings: SettingsStore,
        ) = viewModelFactory {
            initializer { ScaleViewModel(scanner, recorder, settings) }
        }
    }
}
