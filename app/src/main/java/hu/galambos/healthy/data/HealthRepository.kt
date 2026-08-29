package hu.galambos.healthy.data

import hu.galambos.healthy.domain.HealthConnectAvailability
import hu.galambos.healthy.domain.HistoryAccess
import hu.galambos.healthy.domain.metric.MetricDescriptor
import hu.galambos.healthy.domain.sleep.SleepNight
import hu.galambos.healthy.domain.summary.MetricSummary
import hu.galambos.healthy.domain.summary.TrendWindow

/**
 * The boundary between the app and Health Connect.
 *
 * It exists because an emulator holds no health data: without a fake behind
 * this interface, every preview and every test would need the phone. Later
 * additions — a widget, an export — attach as new implementations rather than
 * as changes to this one.
 */
interface HealthRepository {

    fun availability(): HealthConnectAvailability

    /** Read permissions for every metric in the registry. */
    val readPermissions: Set<String>

    /**
     * What to ask for, which is the read permissions plus the history
     * permission where the device offers it. Asking for a permission this
     * device's Health Connect does not know is a good way to have the whole
     * request refused.
     */
    suspend fun permissionsToRequest(): Set<String>

    suspend fun grantedPermissions(): Set<String>

    suspend fun historyAccess(): HistoryAccess

    /**
     * Reads one metric: the newest record for the headline value, and daily
     * buckets for the trend. Never throws — a failure comes back as a state
     * on the summary, because one metric failing must not take the dashboard
     * down with it.
     */
    suspend fun loadSummary(descriptor: MetricDescriptor, window: TrendWindow): MetricSummary

    /**
     * The most recent night with its stages, and the heart rate, blood oxygen
     * and respiration recorded during it. Separate from [loadSummary] because
     * a sleep card needs a duration while a sleep screen needs the shape of
     * the night, and reading the second for every card would be waste.
     */
    suspend fun loadSleepNight(): SleepNight?
}
