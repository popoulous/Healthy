package hu.galambos.healthy.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import hu.galambos.healthy.data.ChangePoll
import hu.galambos.healthy.data.HealthRepository
import hu.galambos.healthy.domain.HealthConnectAvailability
import hu.galambos.healthy.domain.HistoryAccess
import hu.galambos.healthy.domain.metric.MetricDescriptor
import hu.galambos.healthy.domain.metric.MetricId
import hu.galambos.healthy.domain.metric.MetricRegistry
import hu.galambos.healthy.domain.sleep.SleepNight
import hu.galambos.healthy.domain.summary.DataPoint
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

/**
 * The only class in the app that talks to the Health Connect SDK.
 */
class HealthConnectRepository(
    private val context: Context,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : HealthRepository {

    private val client: HealthConnectClient? by lazy {
        if (availability() == HealthConnectAvailability.Available) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    private val reader: SummaryReader? by lazy { client?.let { SummaryReader(it, zone) } }

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
     * Null means "never asked", which is not the same as "nothing granted" —
     * the dashboard starts loading at the same moment the access check does,
     * and treating the empty cache as an answer showed every card as refused.
     */
    @Volatile
    private var cachedGranted: Set<String>? = null

    override suspend fun grantedPermissions(): Set<String> {
        val granted = client?.permissionController?.getGrantedPermissions().orEmpty()
        cachedGranted = granted
        return granted
    }

    private suspend fun granted(): Set<String> = cachedGranted ?: grantedPermissions()

    override suspend fun isGranted(descriptor: MetricDescriptor): Boolean =
        client != null && HealthPermission.getReadPermission(descriptor.recordType) in granted()

    override suspend fun historyAccess(): HistoryAccess = when {
        !isHistorySupported() -> HistoryAccess.Unsupported
        HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY in granted() -> HistoryAccess.Granted
        else -> HistoryAccess.NotGranted
    }

    override suspend fun readLatest(descriptor: MetricDescriptor): DataPoint? =
        guard { reader?.readLatest(descriptor) }

    override suspend fun readDailyValues(
        descriptor: MetricDescriptor,
        from: LocalDate,
        to: LocalDate,
    ): Map<LocalDate, Double> = guard { reader?.readDailyValues(descriptor, from, to) }.orEmpty()

    override suspend fun loadSleepNight(): SleepNight? {
        val client = client ?: return null
        if (HealthPermission.getReadPermission(SleepSessionRecord::class) !in granted()) return null
        return guard { SleepReader(client).readLatestNight() }
    }

    override suspend fun newChangesToken(): String? = guard {
        val client = client ?: return@guard null
        client.getChangesToken(
            ChangesTokenRequest(
                recordTypes = MetricRegistry.all.mapTo(mutableSetOf()) { it.recordType },
            ),
        )
    }

    override suspend fun pollChanges(token: String): ChangePoll {
        val client = client ?: return ChangePoll.Unavailable
        val affected = mutableMapOf<MetricId, LocalDate>()
        var deletions = false
        var next = token

        return try {
            var hasMore: Boolean
            do {
                val response = client.getChanges(next)
                if (response.changesTokenExpired) return ChangePoll.TokenExpired

                response.changes.forEach { change ->
                    when (change) {
                        is UpsertionChange -> {
                            val record = change.record
                            val id = MetricRegistry.idOf(record::class)
                            if (id != null) {
                                val day = record.metadata.lastModifiedTime
                                    .atZone(zone)
                                    .toLocalDate()
                                // Keep the earliest day touched: the re-read
                                // runs from there to today, so a late-arriving
                                // night is not missed because a newer one
                                // followed it.
                                affected[id] = minOf(affected[id] ?: day, day)
                            }
                        }
                        // Only an id arrives, never the type. Nothing useful
                        // can be inferred, so the caller re-reads the window.
                        is DeletionChange -> deletions = true
                    }
                }
                next = response.nextChangesToken
                hasMore = response.hasMore
            } while (hasMore)

            ChangePoll.Changes(affected, deletions, next)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            ChangePoll.Unavailable
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

    /**
     * One metric failing must not take the whole sync down with it.
     * Cancellation is rethrown: a reload cancels the previous pass.
     */
    private inline fun <T> guard(block: () -> T): T? = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        null
    }
}
