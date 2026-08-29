package hu.galambos.healthy.data

import hu.galambos.healthy.domain.HealthConnectAvailability
import hu.galambos.healthy.domain.HistoryAccess
import hu.galambos.healthy.domain.metric.MetricDescriptor
import hu.galambos.healthy.domain.metric.MetricId
import hu.galambos.healthy.domain.sleep.SleepNight
import hu.galambos.healthy.domain.summary.DataPoint
import java.time.LocalDate

/**
 * What Health Connect can tell this app.
 *
 * It exists because an emulator holds no health data: without a fake behind
 * this interface, every preview and every test would need the phone.
 *
 * Note what is no longer here: nothing returns a finished card. Reading and
 * keeping are separate jobs now — this side reads, the local store keeps, and
 * the screen draws from the store.
 */
interface HealthRepository {

    fun availability(): HealthConnectAvailability

    /** Read permissions for every metric in the registry. */
    val readPermissions: Set<String>

    /**
     * What to ask for: the read permissions plus the history permission where
     * the device offers it. Asking for a permission this device's Health
     * Connect does not know is a good way to have the whole request refused.
     */
    suspend fun permissionsToRequest(): Set<String>

    suspend fun grantedPermissions(): Set<String>

    suspend fun historyAccess(): HistoryAccess

    /** Whether this metric may be read at all right now. */
    suspend fun isGranted(descriptor: MetricDescriptor): Boolean

    /**
     * The newest raw record: the value, when it was taken, and which app wrote
     * it. Aggregates carry none of that, which is why it is read separately.
     */
    suspend fun readLatest(descriptor: MetricDescriptor): DataPoint?

    /** One value per day that has data, between the two dates inclusive. */
    suspend fun readDailyValues(
        descriptor: MetricDescriptor,
        from: LocalDate,
        to: LocalDate,
    ): Map<LocalDate, Double>

    /** The most recent night with its stages, and the vitals recorded during it. */
    suspend fun loadSleepNight(): SleepNight?

    /** A fresh token to track changes from now on. Null when unavailable. */
    suspend fun newChangesToken(): String?

    /** What changed since [token] was issued. */
    suspend fun pollChanges(token: String): ChangePoll
}

/**
 * The answer to "what changed".
 *
 * A deletion arrives without its record type — Health Connect gives only the
 * id, for privacy — so [Changes.deletions] is a flag rather than a list of
 * affected metrics. The caller has to re-read a window and cannot be cleverer
 * than that.
 */
sealed interface ChangePoll {

    data class Changes(
        /** Earliest day touched, per metric. */
        val affected: Map<MetricId, LocalDate>,
        val deletions: Boolean,
        val nextToken: String,
    ) : ChangePoll

    /** Unused for thirty days. The caller re-reads and asks for a new token. */
    data object TokenExpired : ChangePoll

    data object Unavailable : ChangePoll
}
