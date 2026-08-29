package hu.galambos.healthy.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import hu.galambos.healthy.domain.metric.MetricAccent
import hu.galambos.healthy.domain.sleep.SleepStage

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
 * One accent per metric, applied to the icon, the mini chart and the trend
 * indicator — never to a card background.
 */
object AccentColors {
    val Steps = Color(0xFF4CAF50)
    val Heart = Color(0xFFE53935)
    val Sleep = Color(0xFF6C5CE7)
    val Oxygen = Color(0xFF20A4B8)
    val Calories = Color(0xFFF57C00)
    val Weight = Color(0xFF2F80ED)
}

/**
 * Metrics the design did not name wear the neutral secondary colour, so the
 * coloured cards stay the ones that mean something.
 */
@Composable
fun colorOf(accent: MetricAccent): Color = when (accent) {
    MetricAccent.Steps -> AccentColors.Steps
    MetricAccent.Heart -> AccentColors.Heart
    MetricAccent.Sleep -> AccentColors.Sleep
    MetricAccent.Oxygen -> AccentColors.Oxygen
    MetricAccent.Calories -> AccentColors.Calories
    MetricAccent.Weight -> AccentColors.Weight
    MetricAccent.Neutral -> androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * The sleep stages, dark to light the way sleep charts read: deep the darkest
 * and lowest on the scale, REM the palest. Awake is deliberately outside that
 * ramp, in orange, because it is not a depth of sleep.
 */
@Composable
fun sleepStageColor(stage: SleepStage): Color = when (stage) {
    SleepStage.Deep -> Color(0xFF3B2FBF)
    SleepStage.Light, SleepStage.Sleeping -> Color(0xFF6C5CE7)
    SleepStage.Rem -> Color(0xFF9C8CFF)
    // Waking up breaks the blue-to-violet run on purpose: an interruption is
    // not a depth of sleep, and reading it as one is the whole point of seeing
    // it at all.
    SleepStage.Awake, SleepStage.AwakeInBed, SleepStage.OutOfBed -> Color(0xFFFF9F43)
    SleepStage.Unknown -> androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
}
