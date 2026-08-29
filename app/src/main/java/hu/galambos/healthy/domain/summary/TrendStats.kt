package hu.galambos.healthy.domain.summary

/**
 * What a detail screen says about a window beyond drawing it: the range, the
 * average, and how the newest reading sits against that average.
 *
 * Days with no data are skipped rather than counted as zero. Averaging a
 * missing Tuesday as nought would drag every average down and invent a bad
 * week out of a watch left on the charger.
 */
data class TrendStats(
    val min: Double,
    val max: Double,
    val average: Double,
    val latest: Double?,
    val daysWithData: Int,
) {
    /** How far the latest reading is from the window's average. */
    val deltaFromAverage: Double? get() = latest?.let { it - average }
}

fun List<Bucket>.stats(): TrendStats? {
    val values = mapNotNull { it.value }
    if (values.isEmpty()) return null
    return TrendStats(
        min = values.min(),
        max = values.max(),
        average = values.average(),
        latest = lastOrNull { it.value != null }?.value,
        daysWithData = values.size,
    )
}
