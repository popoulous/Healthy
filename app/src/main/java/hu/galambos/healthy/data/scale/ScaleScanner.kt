package hu.galambos.healthy.data.scale

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import hu.galambos.healthy.BuildConfig
import hu.galambos.healthy.domain.scale.ScaleAdvertisement
import hu.galambos.healthy.domain.scale.ScaleReading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

/**
 * Spelled out rather than referenced through `Manifest.permission`, whose
 * constant only exists from API 31. The name is stable; the field is not
 * available to compile against at this app's floor.
 */
const val BLUETOOTH_SCAN_PERMISSION = "android.permission.BLUETOOTH_SCAN"

/**
 * Why the scale cannot be listened to right now, if it cannot.
 */
enum class ScaleAvailability {
    Ready,

    /**
     * Below Android 12 a Bluetooth scan also demands location access, because
     * beacons can place you. Asking for someone's location to read their
     * bathroom scale is a bad trade in an app that promises to send nothing
     * anywhere, so the feature simply does not offer itself there.
     */
    NeedsAndroid12,
    NoBluetoothHardware,
    BluetoothOff,
    PermissionMissing,
}

/**
 * Listens for the scale.
 *
 * There is no pairing and no connection: the scale shouts its measurement into
 * the room and anything in range can hear it. That also means nothing is ever
 * sent *to* the scale — this is as read-only as the rest of the app.
 */
class ScaleScanner(private val context: Context) {

    private val manager: BluetoothManager? =
        ContextCompat.getSystemService(context, BluetoothManager::class.java)

    private val adapter: BluetoothAdapter? get() = manager?.adapter

    fun availability(): ScaleAvailability = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> ScaleAvailability.NeedsAndroid12
        !context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) ->
            ScaleAvailability.NoBluetoothHardware
        adapter?.isEnabled != true -> ScaleAvailability.BluetoothOff
        !hasScanPermission() -> ScaleAvailability.PermissionMissing
        else -> ScaleAvailability.Ready
    }

    fun hasScanPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, BLUETOOTH_SCAN_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Every advertisement the scale sends while someone is standing on it —
     * including the unsettled ones. Filtering those out is the caller's job,
     * because "the weight is still moving" is worth showing on screen.
     */
    @SuppressLint("MissingPermission") // Guarded by availability() above.
    fun readings(): Flow<ScaleReading> = callbackFlow {
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null || availability() != ScaleAvailability.Ready) {
            close()
            return@callbackFlow
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val payload = result.scanRecord?.serviceData?.get(WEIGHT_SCALE_SERVICE)
                if (payload == null) {
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "advertisement without weight-scale data: ${result.device.address}")
                    }
                    return
                }
                val parsed = ScaleAdvertisement.parse(payload)
                if (BuildConfig.DEBUG) {
                    // The bytes as they actually arrive. The layout is
                    // documented by the community rather than by Xiaomi, and
                    // a measurement that never records is exactly the symptom
                    // of a flag bit sitting somewhere else than expected.
                    Log.d(
                        TAG,
                        "payload=${payload.joinToString("") { "%02X".format(it) }} " +
                            "flags=%04X".format(
                                (payload[0].toInt() and 0xFF) or
                                    ((payload[1].toInt() and 0xFF) shl 8),
                            ) +
                            " parsed=$parsed",
                    )
                }
                parsed?.let { trySend(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.w(TAG, "scan failed: $errorCode")
                close()
            }
        }

        // No filter. A ScanFilter on service *data* needs a mask, and an empty
        // one is read as "match nothing" on some stacks — which is a silent
        // failure, the worst kind. The scale's advertisement is picked out in
        // the callback instead; a scan lasting seconds can afford to see
        // everything in the room.
        val settings = ScanSettings.Builder()
            // A weigh-in lasts seconds and the user is watching the screen, so
            // latency matters more than the battery here. Scanning only runs
            // while this flow is collected.
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        Log.d(TAG, "listening for the scale")
        scanner.startScan(emptyList(), settings, callback)
        awaitClose {
            Log.d(TAG, "stopped listening")
            scanner.stopScan(callback)
        }
    }

    private companion object {
        const val TAG = "HealthyScale"

        /** The standard Bluetooth Weight Scale service the Mi scale broadcasts under. */
        val WEIGHT_SCALE_SERVICE: ParcelUuid =
            ParcelUuid(UUID.fromString("0000181b-0000-1000-8000-00805f9b34fb"))
    }
}
