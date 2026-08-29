package hu.galambos.healthy.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import hu.galambos.healthy.R

/**
 * The four tabs of the bottom bar.
 *
 * Tab state is held in the composition rather than in a NavHost: with four
 * flat destinations and no back stack to speak of, navigation-compose would
 * only add a dependency. It arrives in F4, when metric detail screens need a
 * real back stack.
 *
 * The icons are local vector drawables. Compose no longer ships the Material
 * icon set with material3, and a whole icon library for four glyphs would sit
 * badly with an app whose dependency list is part of its promise.
 */
enum class Destination(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    Overview(R.string.tab_overview, R.drawable.ic_tab_overview),
    Trends(R.string.tab_trends, R.drawable.ic_tab_trends),
    Sources(R.string.tab_sources, R.drawable.ic_tab_sources),
    Settings(R.string.tab_settings, R.drawable.ic_tab_settings),
}
