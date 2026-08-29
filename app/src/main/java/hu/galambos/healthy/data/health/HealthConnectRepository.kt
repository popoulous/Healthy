package hu.galambos.healthy.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import hu.galambos.healthy.data.HealthRepository
import hu.galambos.healthy.domain.HealthConnectAvailability
import hu.galambos.healthy.domain.HistoryAccess
import hu.galambos.healthy.domain.metric.MetricDescriptor
import hu.galambos.healthy.domain.metric.MetricRegistry
import hu.galambos.healthy.domain.sleep.SleepNight
import hu.galambos.healthy.domain.summary.FailureReason
import hu.galambos.healthy.domain.summary.LoadState
import hu.galambos.healthy.domain.summary.MetricSummary
import hu.galambos.healthy.domain.summary.TrendWindow
import kotlin.coroutines.cancellation.CancellationException

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

    /**
     * Re-read whenever access is checked, then reused while a dashboard load
     * runs. Asking Health Connect once per metric would cost a round trip per
     * card for an answer that cannot change mid-load.
     */
    @Volatile
    private var cachedGranted: Set<String> = emptySet()

    override suspend fun grantedPermissions(): Set<String> {
        val granted = client?.permissionController?.getGrantedPermissions().orEmpty()
        cachedGranted = granted
        return granted
    }

    override suspend fun historyAccess(): HistoryAccess = when {
        !isHistorySupported() -> HistoryAccess.Unsupported
        HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY in grantedPermissions() ->
            HistoryAccess.Granted
        else -> HistoryAccess.NotGranted
    }

    override suspend fun loadSummary(
        descriptor: MetricDescriptor,
        window: TrendWindow,
    ): MetricSummary {
        val client = client
            ?: return MetricSummary(descriptor.id, LoadState.NotGranted)
        if (HealthPermission.getReadPermission(descriptor.recordType) !in cachedGranted) {
            return MetricSummary(descriptor.id, LoadState.NotGranted)
        }

        val reader = SummaryReader(client)
        return try {
            val latest = reader.readLatest(descriptor, window)
            val trend = reader.readTrend(descriptor, window)
            if (latest == null && trend.none { it.value != null }) {
                // Permission held, nothing written: a source app that does not
                // share this type. Saying so is the point of the Sources screen.
                MetricSummary(descriptor.id, LoadState.Empty, trend = trend)
            } else {
                MetricSummary(descriptor.id, LoadState.Loaded, latest, trend)
            }
        } catch (cancellation: CancellationException) {
            // Never swallow cancellation: the dashboard reloads by cancelling
            // the previous load.
            throw cancellation
        } catch (_: SecurityException) {
            MetricSummary(descriptor.id, LoadState.NotGranted)
        } catch (_: IllegalStateException) {
            // How Health Connect reports its rate limit.
            MetricSummary(descriptor.id, LoadState.Failed(FailureReason.RateLimited))
        } catch (_: Exception) {
            MetricSummary(descriptor.id, LoadState.Failed(FailureReason.Unknown))
        }
    }

    override suspend fun loadSleepNight(): SleepNight? {
        val client = client ?: return null
        if (HealthPermission.getReadPermission(SleepSessionRecord::class) !in cachedGranted) {
            return null
        }
        return try {
            SleepReader(client).readLatestNight()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }
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
