package hu.galambos.healthy.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import hu.galambos.healthy.data.HealthRepository
import hu.galambos.healthy.domain.metric.MetricId
import hu.galambos.healthy.domain.metric.MetricRegistry
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
) {
    fun summaryFor(id: MetricId): MetricSummary =
        summaries[id] ?: MetricSummary(id, LoadState.Loading)
}

class DashboardViewModel(private val repository: HealthRepository) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var lastLoadAt = 0L

    init {
        load(force = true)
    }

    /**
     * Called on launch, on every return to the foreground, and on pull. The
     * middle one is why the throttle exists: switching to Mi Fitness to sync
     * and back is the normal way to use this, but so is flicking between apps,
     * and re-querying every metric each time walks straight into Health
     * Connect's rate limit.
     */
    fun load(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastLoadAt < MIN_RELOAD_INTERVAL_MS) return
        lastLoadAt = now

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val window = _state.value.window
            // Sequential on purpose: the cards fill in as answers arrive,
            // which reads better than a long blank wait, and it keeps the
            // request rate well under the limit.
            for (descriptor in MetricRegistry.all) {
                val summary = repository.loadSummary(descriptor, window)
                _state.update { it.copy(summaries = it.summaries + (descriptor.id to summary)) }
            }
            _state.update { it.copy(loading = false) }
        }
    }

    fun setWindow(window: TrendWindow) {
        if (window == _state.value.window) return
        _state.update { it.copy(window = window) }
        load(force = true)
    }

    companion object {
        private const val MIN_RELOAD_INTERVAL_MS = 60_000L

        fun factory(repository: HealthRepository) = viewModelFactory {
            initializer { DashboardViewModel(repository) }
        }
    }
}
