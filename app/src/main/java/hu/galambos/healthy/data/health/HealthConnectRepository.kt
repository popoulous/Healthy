package hu.galambos.healthy.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import hu.galambos.healthy.data.HealthRepository
import hu.galambos.healthy.domain.HealthConnectAvailability
import hu.galambos.healthy.domain.HistoryAccess
import hu.galambos.healthy.domain.metric.MetricRegistry

/**
 * The only class in the app that talks to the Health Connect SDK.
 */
class HealthConnectRepository(private val context: Context) : HealthRepository {

    private val client: HealthConnectClient? by lazy {
        if (availability() == HealthConnectAvailability.Available) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    override fun availability(): HealthConnectAvailability =
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.UpdateRequired
            else -> HealthConnectAvailability.NotInstalled
        }

    override val readPermissions: Set<String> =
        MetricRegistry.all.mapTo(mutableSetOf()) { HealthPermission.getReadPermission(it.recordType) }

    override suspend fun permissionsToRequest(): Set<String> =
        if (isHistorySupported()) {
            readPermissions + HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY
        } else {
            readPermissions
        }

    override suspend fun grantedPermissions(): Set<String> =
        client?.permissionController?.getGrantedPermissions().orEmpty()

    override suspend fun historyAccess(): HistoryAccess = when {
        !isHistorySupported() -> HistoryAccess.Unsupported
        HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY in grantedPermissions() ->
            HistoryAccess.Granted
        else -> HistoryAccess.NotGranted
    }

    /**
     * Reading other apps' records past 30 days needs a permission that only
     * newer Health Connect versions have. On the older phone it simply is not
     * there, and the app says so rather than silently showing a month.
     */
    private fun isHistorySupported(): Boolean {
        val features = client?.features ?: return false
        return features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY) ==
            HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    }
}
