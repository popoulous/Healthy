package hu.galambos.healthy.domain.summary

import java.time.LocalDate

/**
 * How far back a card looks. Seven and thirty days for now; ninety was
 * deliberately left for later, once there is a chart worth stretching.
 */
enum class TrendWindow(val days: Int) {
    Week(7),
    Month(30),
    ;

    /** The days of the window, oldest first, ending with [today]. */
    fun dates(today: LocalDate): List<LocalDate> =
        (days - 1 downTo 0).map { today.minusDays(it.toLong()) }
}

/**
 * Turns whatever days actually carried data into one entry per day of the
 * window. Gaps stay as nulls rather than being dropped, so a sparkline shows
 * a missing Tuesday as a hole instead of quietly closing it up and implying
 * a reading that never happened.
 */
fun buildTrend(
    window: TrendWindow,
    today: LocalDate,
    values: Map<LocalDate, Double>,
): List<Bucket> = window.dates(today).map { date -> Bucket(date, values[date]) }
