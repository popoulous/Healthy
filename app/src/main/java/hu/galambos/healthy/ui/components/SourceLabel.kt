package hu.galambos.healthy.ui.components

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Health Connect identifies a source by package name. Turning
 * `com.xiaomi.wearable` into "Mi Fitness" means asking the package manager,
 * and from Android 11 that only answers for packages the manifest declares in
 * its `queries` block. The known sources are declared there; anything else
 * shows as its package name, which is honest and still useful.
 *
 * QUERY_ALL_PACKAGES would answer for everything and is not worth it: an app
 * that promises to send nothing anywhere should not also ask to see every app
 * installed.
 */
private val labelCache = ConcurrentHashMap<String, String>()

@Composable
fun rememberSourceLabel(packageName: String): String {
    val context = LocalContext.current
    return remember(packageName) { resolveLabel(context, packageName) }
}

private fun resolveLabel(context: Context, packageName: String): String =
    labelCache.getOrPut(packageName) {
        try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }
