package hu.galambos.healthy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Roboto, the system font — no bundled or downloadable font, because the app
 * makes no network requests at all.
 *
 * The hierarchy is deliberately steep: a metric value should dwarf its unit,
 * so a reading is legible at a glance and the metadata stays quiet.
 */
private val SystemFont = FontFamily.Default

val HealthyTypography = Typography(
    // Metric value: the largest thing on any card.
    displaySmall = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
    ),
    // Screen title.
    headlineMedium = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    // Section heading.
    titleMedium = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    // Metric name, body copy.
    bodyMedium = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // Timestamp, data source, units.
    labelSmall = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)
