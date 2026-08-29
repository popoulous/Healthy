package hu.galambos.healthy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MetricDao {

    /**
     * Replace on conflict, deliberately: a day read twice is the same day, and
     * the newer read is the truer one.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBuckets(buckets: List<DailyBucketEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLatest(readings: List<LatestReadingEntity>)

    @Query("SELECT * FROM daily_bucket WHERE epochDay >= :fromEpochDay ORDER BY epochDay")
    fun observeBuckets(fromEpochDay: Long): Flow<List<DailyBucketEntity>>

    @Query("SELECT * FROM latest_reading")
    fun observeLatest(): Flow<List<LatestReadingEntity>>

    @Query("SELECT * FROM latest_reading WHERE metricId = :metricId")
    suspend fun latestFor(metricId: String): LatestReadingEntity?

    /**
     * Used when a deletion arrives: Health Connect reports only the id of the
     * deleted record and not its type, so the affected day cannot be worked
     * out — the window is cleared and read again.
     */
    @Query("DELETE FROM daily_bucket WHERE metricId = :metricId AND epochDay >= :fromEpochDay")
    suspend fun clearBucketsFrom(metricId: String, fromEpochDay: Long)

    @Query("SELECT MIN(epochDay) FROM daily_bucket")
    suspend fun earliestDay(): Long?
}

@Dao
interface ScaleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(measurement: ScaleMeasurementEntity)

    @Query("SELECT * FROM scale_measurement ORDER BY timeEpochMillis DESC")
    fun observeAll(): Flow<List<ScaleMeasurementEntity>>

    @Query("SELECT * FROM scale_measurement ORDER BY timeEpochMillis DESC LIMIT 1")
    suspend fun latest(): ScaleMeasurementEntity?

    /** Every measurement, for when a profile change invalidates the derived values. */
    @Query("SELECT * FROM scale_measurement ORDER BY timeEpochMillis")
    suspend fun all(): List<ScaleMeasurementEntity>

    /**
     * A row with no impedance is a weight caught mid-step, not a weigh-in.
     * Such rows were briefly recorded while the completion rule was too loose.
     */
    @Query("DELETE FROM scale_measurement WHERE impedanceOhms IS NULL")
    suspend fun deleteIncomplete(): Int
}

@Dao
interface SyncStateDao {

    @Query("SELECT * FROM sync_state WHERE id = 0")
    suspend fun get(): SyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(state: SyncStateEntity)
}
