package hu.galambos.healthy.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale as ComposeLocale
import hu.galambos.healthy.R
import hu.galambos.healthy.domain.metric.MetricUnit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * A number and its unit, kept apart because the design sets them at very
 * different sizes: the reading is the point, the unit is a footnote.
 */
data class FormattedValue(val number: String, val unit: String)

@Composable
fun formatValue(value: Double, unit: MetricUnit): FormattedValue {
    val locale = currentLocale()
    return when (unit) {
        // Hours are read as hours and minutes, never as 7.7 of something.
        MetricUnit.Hours -> {
            val totalMinutes = (value * 60).roundToInt()
            FormattedValue(
                number = stringResource(
                    R.string.duration_hours_minutes,
                    totalMinutes / 60,
                    totalMinutes % 60,
                ),
                unit = "",
            )
        }

        else -> FormattedValue(
            number = formatNumber(value, unit.decimals, locale),
            unit = stringResource(unitLabel(unit)),
        )
    }
}

private fun formatNumber(value: Double, decimals: Int, locale: Locale): String =
    String.format(locale, "%,.${decimals}f", value)

private fun unitLabel(unit: MetricUnit) = when (unit) {
    MetricUnit.Count -> R.string.unit_count
    MetricUnit.Steps -> R.string.unit_steps
    MetricUnit.Bpm -> R.string.unit_bpm
    MetricUnit.Percent -> R.string.unit_percent
    MetricUnit.Kilograms -> R.string.unit_kg
    MetricUnit.Kilocalories -> R.string.unit_kcal
    MetricUnit.Hours -> R.string.unit_hours
    MetricUnit.Meters -> R.string.unit_m
    MetricUnit.Kilometres -> R.string.unit_km
    MetricUnit.Litres -> R.string.unit_l
    MetricUnit.Celsius -> R.string.unit_celsius
    MetricUnit.MillimolesPerLitre -> R.string.unit_mmol_l
    MetricUnit.MillimetresOfMercury -> R.string.unit_mmhg
    MetricUnit.Watts -> R.string.unit_w
    MetricUnit.MetresPerSecond -> R.string.unit_m_s
    MetricUnit.MillilitresPerMinuteKilogram -> R.string.unit_vo2
    MetricUnit.Minutes -> R.string.unit_minutes
    MetricUnit.BreathsPerMinute -> R.string.unit_breaths
    MetricUnit.Centimetres -> R.string.unit_cm
    MetricUnit.Milliseconds -> R.string.unit_ms
    MetricUnit.StepsPerMinute -> R.string.unit_spm
    MetricUnit.RevolutionsPerMinute -> R.string.unit_rpm
    MetricUnit.CelsiusDelta -> R.string.unit_celsius
}

/**
 * "Today 08:42" for something recorded today, a date otherwise — a bare
 * timestamp on a week-old reading invites the reader to think it is fresh.
 */
@Composable
fun formatTimestamp(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
    val locale = currentLocale()
    val dateTime = instant.atZone(zone)
    val today = LocalDate.now(zone)
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(locale)
        .format(dateTime)
    return when (dateTime.toLocalDate()) {
        today -> stringResource(R.string.timestamp_today, time)
        today.minusDays(1) -> stringResource(R.string.timestamp_yesterday, time)
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
            .format(dateTime) + " " + time
    }
}

/**
 * Compose's own locale rather than the configuration's, so a language change
 * recomposes the numbers instead of leaving them formatted for the old one.
 */
@Composable
private fun currentLocale(): Locale =
    Locale.forLanguageTag(ComposeLocale.current.toLanguageTag())
