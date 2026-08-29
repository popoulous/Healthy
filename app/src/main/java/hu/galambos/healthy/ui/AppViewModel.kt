package hu.galambos.healthy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import hu.galambos.healthy.data.HealthRepository
import hu.galambos.healthy.domain.HealthConnectAvailability
import hu.galambos.healthy.domain.HistoryAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the app knows about its access to Health Connect. Everything here can
 * change while the app is in the background — the user may grant or revoke
 * permissions in Health Connect itself — so it is re-read whenever the app
 * comes back to the foreground.
 */
data class AccessState(
    val checked: Boolean = false,
    val availability: HealthConnectAvailability = HealthConnectAvailability.NotInstalled,
    val granted: Set<String> = emptySet(),
    val required: Set<String> = emptySet(),
    val historyAccess: HistoryAccess = HistoryAccess.Unsupported,
) {
    val hasAnyPermission: Boolean get() = granted.isNotEmpty()
}

class AppViewModel(private val repository: HealthRepository) : ViewModel() {

    private val _access = MutableStateFlow(AccessState())
    val access: StateFlow<AccessState> = _access.asStateFlow()

    init {
        refreshAccess()
    }

    fun refreshAccess() {
        viewModelScope.launch {
            val availability = repository.availability()
            if (availability != HealthConnectAvailability.Available) {
                _access.update {
                    it.copy(checked = true, availability = availability, granted = emptySet())
                }
                return@launch
            }
            _access.update {
                it.copy(
                    checked = true,
                    availability = availability,
                    granted = repository.grantedPermissions(),
                    required = repository.permissionsToRequest(),
                    historyAccess = repository.historyAccess(),
                )
            }
        }
    }

    companion object {
        fun factory(repository: HealthRepository) = viewModelFactory {
            initializer { AppViewModel(repository) }
        }
    }
}
