package hu.galambos.healthy.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Which theme the app follows, overriding the system when asked to. */
enum class ThemeChoice { System, Light, Dark }

/** Metric everywhere by default; the alternatives exist because people differ. */
enum class MassUnit { Kilograms, Pounds }

enum class DistanceUnit { Kilometres, Miles }

data class Settings(
    val theme: ThemeChoice = ThemeChoice.System,
    val mass: MassUnit = MassUnit.Kilograms,
    val distance: DistanceUnit = DistanceUnit.Kilometres,
    /** Used only for the greeting; the app cannot learn a name from anywhere else. */
    val name: String = "",
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

/**
 * Preferences, and nothing else. There is no database here: Health Connect
 * holds the health data, and duplicating it would buy a cache to invalidate
 * and nothing more.
 */
class SettingsStore(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme")
    private val massKey = stringPreferencesKey("mass_unit")
    private val distanceKey = stringPreferencesKey("distance_unit")
    private val nameKey = stringPreferencesKey("name")

    val settings: Flow<Settings> = context.dataStore.data.map { preferences ->
        Settings(
            theme = preferences[themeKey]?.toEnum(ThemeChoice.entries) ?: ThemeChoice.System,
            mass = preferences[massKey]?.toEnum(MassUnit.entries) ?: MassUnit.Kilograms,
            distance = preferences[distanceKey]?.toEnum(DistanceUnit.entries)
                ?: DistanceUnit.Kilometres,
            name = preferences[nameKey].orEmpty(),
        )
    }

    suspend fun setTheme(choice: ThemeChoice) = put(themeKey, choice.name)

    suspend fun setMassUnit(unit: MassUnit) = put(massKey, unit.name)

    suspend fun setDistanceUnit(unit: DistanceUnit) = put(distanceKey, unit.name)

    suspend fun setName(name: String) = put(nameKey, name.trim())

    private suspend fun put(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { it[key] = value }
    }
}

/**
 * Falls back rather than throwing: a stored value from an older build that no
 * longer names anything should reset a preference, not crash the app.
 */
private fun <T : Enum<T>> String.toEnum(values: List<T>): T? =
    values.firstOrNull { it.name == this }
