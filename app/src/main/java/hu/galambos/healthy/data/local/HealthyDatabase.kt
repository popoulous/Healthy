package hu.galambos.healthy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app's own store, for the two things Health Connect cannot keep for it:
 * summarised history that has to outlive Health Connect's own auto-delete, and
 * scale measurements, which never reach Health Connect at all.
 *
 * Raw records are not here. Nothing in this app draws a minute-by-minute heart
 * rate, and a year of those would be a table no screen ever reads.
 */
@Database(
    entities = [
        DailyBucketEntity::class,
        LatestReadingEntity::class,
        ScaleMeasurementEntity::class,
        SyncStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class HealthyDatabase : RoomDatabase() {

    abstract fun metricDao(): MetricDao

    abstract fun scaleDao(): ScaleDao

    abstract fun syncStateDao(): SyncStateDao

    companion object {
        @Volatile
        private var instance: HealthyDatabase? = null

        fun get(context: Context): HealthyDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                HealthyDatabase::class.java,
                "healthy.db",
            ).build().also { instance = it }
        }
    }
}
