package hu.galambos.healthy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One value per metric per day — the trend, already summarised.
 *
 * Keyed by metric and day so a re-read overwrites rather than duplicates: when
 * a source syncs late and yesterday changes, the next pass corrects that day
 * and touches nothing else.
 *
 * [epochDay] rather than a date string: compact, sortable, and unambiguous
 * about which calendar it means.
 */
@Entity(tableName = "daily_bucket", primaryKeys = ["metricId", "epochDay"])
data class DailyBucketEntity(
    val metricId: String,
    val epochDay: Long,
    val value: Double,
)

/**
 * The newest reading of each metric: the number a card leads with, plus the
 * timestamp and the writing app that an aggregate cannot carry.
 */
@Entity(tableName = "latest_reading")
data class LatestReadingEntity(
    @PrimaryKey val metricId: String,
    val value: Double,
    val timeEpochMillis: Long,
    val sourcePackage: String,
)

/**
 * A weigh-in, stored raw.
 *
 * Weight and impedance are what the scale actually broadcasts; body fat, muscle
 * and the rest are computed from them together with the owner's profile. Only
 * the raw pair is kept, so correcting a height or a birth year re-derives every
 * past measurement instead of leaving a trail of figures computed from a
 * profile nobody remembers.
 */
@Entity(tableName = "scale_measurement")
data class ScaleMeasurementEntity(
    @PrimaryKey val timeEpochMillis: Long,
    val weightKg: Double,
    /** Null until the scale has settled enough to report it. */
    val impedanceOhms: Int?,
)

/**
 * Where the incremental sync left off.
 *
 * The token is Health Connect's; [lastSyncEpochMillis] exists because an unused
 * token expires after thirty days, and the documented recovery is to re-read
 * from the last known point rather than from nothing.
 */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = SINGLE_ROW,
    val changesToken: String?,
    val lastSyncEpochMillis: Long,
) {
    companion object {
        const val SINGLE_ROW = 0
    }
}
