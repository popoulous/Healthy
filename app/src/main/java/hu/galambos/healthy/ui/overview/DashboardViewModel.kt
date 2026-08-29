package hu.galambos.healthy.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import hu.galambos.healthy.data.HealthRepository
import hu.galambos.healthy.data.local.MetricStore
import hu.galambos.healthy.data.sync.HealthSync
import hu.galambos.healthy.domain.metric.MetricId
import hu.galambos.healthy.domain.metric.MetricRegistry
import hu.galambos.healthy.domain.sleep.SleepNight
import hu.galambos.healthy.domain.summary.LoadState
import hu.galambos.healthy.domain.summary.MetricSummary
import hu.galambos.healthy.domain.summary.TrendWindow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardState(
    val window: TrendWindow = TrendWindow.Week,
    val summaries: Map<MetricId, MetricSummary> = emptyMap(),
    val loading: Boolean = false,
    /**
     * Loaded on demand rather than with the dashboard: the shape of a night
     * costs several reads and only the sleep detail screen shows it.
     */
    val sleepNight: SleepNight? = null,
) {
    fun summaryFor(id: MetricId): MetricSummary =
        summaries[id] ?: MetricSummary(id, LoadState.Loading)
}

/**
 * The dashboard reads the local store and shows whatever is there, then syncs
 * in the background and lets the store push the update through.
 *
 * That ordering is the point: the screen fills immediately from what was kept
 * last time instead of waiting on thirty-three queries against Health Connect.
 */
class DashboardViewModel(
    private val repository: HealthRepository,
    private val store: MetricStore,
    private val sync: HealthSync,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var observeJob: Job? = null
    private var syncJob: Job? = null
    private var lastSyncAt = 0L

    /** Metrics with no permission, so the store's "empty" is not shown as data. */
    private var refused: Set<MetricId> = emptySet()

    init {
        observeStore()
        refresh(force = true)
    }

    private fun observeStore() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            store.observeSummaries(_state.value.window).collect { fromStore ->
                _state.update { it.copy(summaries = overlayRefusals(fromStore)) }
            }
        }
    }

    /**
     * "Nothing was written" and "you did not allow this" look identical in the
     * store — both are simply absent. Only the permission state can tell them
     * apart, and the difference is the whole point of the empty card.
     */
    private fun overlayRefusals(
        summaries: Map<MetricId, MetricSummary>,
    ): Map<MetricId, MetricSummary> = summaries.mapValues { (id, summary) ->
        if (id in refused) MetricSummary(id, LoadState.NotGranted) else summary
    }

    /**
     * Called on launch, on every return to the foreground, and on pull. The
     * middle one is why the throttle exists: switching to Mi Fitness to sync
     * and back is the normal way to use this, but so is flicking between apps.
     */
    fun refresh(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSyncAt < MIN_RELOAD_INTERVAL_MS) return
        lastSyncAt = now

        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            refused = MetricRegistry.all
                .filterNot { repository.isGranted(it) }
                .mapTo(mutableSetOf()) { it.id }
            _state.update { it.copy(summaries = overlayRefusals(it.summaries)) }

            sync.sync()
            _state.update { it.copy(loading = false) }
        }
    }

    /** Called when the sleep detail opens; cheap to repeat, so not throttled. */
    fun loadSleepNight() {
        if (_state.value.sleepNight != null) return
        viewModelScope.launch {
            val night = repository.loadSleepNight()
            _state.update { it.copy(sleepNight = night) }
        }
    }

    fun setWindow(window: TrendWindow) {
        if (window == _state.value.window) return
        _state.update { it.copy(window = window) }
        // The window only changes how much of the archive is drawn; no new read
        // is needed for it.
        observeStore()
    }

    companion object {
        private const val MIN_RELOAD_INTERVAL_MS = 60_000L

        fun factory(
            repository: HealthRepository,
            store: MetricStore,
            sync: HealthSync,
        ) = viewModelFactory {
            initializer { DashboardViewModel(repository, store, sync) }
        }
    }
}
