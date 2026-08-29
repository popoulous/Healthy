package hu.galambos.healthy.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The palette is fixed rather than derived from the wallpaper. Material You
 * would recolour the neutral ground the design specifies, and the metric
 * accents below carry meaning — red is the heart, not decoration.
 */

// Neutral ground — light
val LightBackground = Color(0xFFF7F7F3)
val LightSurface = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF202124)
val LightTextSecondary = Color(0xFF6B7078)

// Neutral ground — dark
val DarkBackground = Color(0xFF121417)
val DarkSurface = Color(0xFF1B1F24)
val DarkSurfaceElevated = Color(0xFF23272D)
val DarkTextPrimary = Color(0xFFF5F7FA)
val DarkTextSecondary = Color(0xFF9AA1AA)

// Chrome: navigation, buttons, selection.
val BrandGreen = Color(0xFF2E7D32)
val BrandGreenLight = Color(0xFF7BC67E)

/**
 * One accent per metric. These are applied to the icon, the mini chart and the
 * trend indicator — never to a card background.
 */
object MetricAccent {
    val Steps = Color(0xFF4CAF50)
    val Heart = Color(0xFFE53935)
    val Sleep = Color(0xFF6C5CE7)
    val OxygenSaturation = Color(0xFF20A4B8)
    val Calories = Color(0xFFF57C00)
    val Weight = Color(0xFF2F80ED)
}
