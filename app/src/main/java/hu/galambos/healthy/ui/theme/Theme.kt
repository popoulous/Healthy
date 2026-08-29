package hu.galambos.healthy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The whole scheme is spelled out, not just the few roles the design names.
 *
 * Material fills anything left unset from its own baseline, which is purple —
 * and that baseline is what paints the filter chips, the navigation bar and
 * the selected-tab pill. Setting only the background and the primary left the
 * app looking like stock Material with a green button in it.
 */
private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF0B2E10),

    secondary = Color(0xFF4F6352),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E8D8),
    onSecondaryContainer = Color(0xFF0E1F11),

    tertiary = Color(0xFF3C6472),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC4E4F0),
    onTertiaryContainer = Color(0xFF0A2932),

    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFE8E9E4),
    onSurfaceVariant = LightTextSecondary,

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBFBF8),
    surfaceContainer = Color(0xFFF2F2ED),
    surfaceContainerHigh = Color(0xFFECECE6),
    surfaceContainerHighest = Color(0xFFE6E6E0),

    outline = Color(0xFF8A8F87),
    outlineVariant = Color(0xFFDCDDD6),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = BrandGreenLight,
    onPrimary = Color(0xFF06290C),
    primaryContainer = Color(0xFF1E4622),
    onPrimaryContainer = Color(0xFFC8E6C9),

    secondary = Color(0xFFB6CCB8),
    onSecondary = Color(0xFF213724),
    secondaryContainer = Color(0xFF2A4530),
    onSecondaryContainer = Color(0xFFD7E8D8),

    tertiary = Color(0xFFA3CBDA),
    onTertiary = Color(0xFF10333E),
    tertiaryContainer = Color(0xFF264B57),
    onTertiaryContainer = Color(0xFFC4E4F0),

    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0xFF2A2F35),
    onSurfaceVariant = DarkTextSecondary,

    surfaceContainerLowest = Color(0xFF0D0F12),
    surfaceContainerLow = Color(0xFF16191D),
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceElevated,
    surfaceContainerHighest = Color(0xFF2A2F35),

    outline = Color(0xFF7C848C),
    outlineVariant = Color(0xFF2E343B),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

/**
 * [darkTheme] defaults to the system setting; the settings screen overrides it.
 */
@Composable
fun HealthyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = HealthyTypography,
        content = content,
    )
}
