package hu.galambos.healthy.domain.metric

import androidx.annotation.StringRes
import hu.galambos.healthy.R

/** Health Connect's own grouping, which is also how the dashboard is sectioned. */
enum class MetricCategory(@param:StringRes val labelRes: Int) {
    Activity(R.string.category_activity),
    Body(R.string.category_body),
    Vitals(R.string.category_vitals),
    Sleep(R.string.category_sleep),
    Nutrition(R.string.category_nutrition),
    Wellness(R.string.category_wellness),
}

/**
 * Which accent a metric wears. The design names six; everything else is
 * neutral, so the coloured cards stay the ones that carry meaning.
 */
enum class MetricAccent {
    Steps,
    Heart,
    Sleep,
    Oxygen,
    Calories,
    Weight,
    Neutral,
}
