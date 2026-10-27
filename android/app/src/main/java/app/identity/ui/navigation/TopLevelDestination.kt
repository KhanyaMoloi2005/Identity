package app.identity.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.identity.R
import kotlinx.serialization.Serializable

// Routes: each screen is identified by a type, so a typo becomes a compile error, not a crash.
@Serializable data object HomeRoute
@Serializable data object ScriptureRoute
@Serializable data object PartnerRoute
@Serializable data object SettingsRoute

/** The four tabs in the bottom navigation bar, in display order. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
) {
    Home(HomeRoute, R.string.nav_home, R.drawable.ic_nav_home),
    Scripture(ScriptureRoute, R.string.nav_scripture, R.drawable.ic_nav_scripture),
    Partner(PartnerRoute, R.string.nav_partner, R.drawable.ic_nav_partner),
    Settings(SettingsRoute, R.string.nav_settings, R.drawable.ic_nav_settings),
}
