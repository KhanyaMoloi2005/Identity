package app.identity.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// onPrimary is Ink in both themes: Ink on DawnLight is about 4.8:1 and Ink on DawnDark about 7.7:1 (WCAG AA).
// DawnLight is NOT used for small text on Parchment (only about 3.2:1).

private val LightColors = lightColorScheme(
    primary = DawnLight,
    onPrimary = Ink,
    background = Parchment,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    onSurfaceVariant = MutedLight,
    error = InterruptedLight,
    onError = Parchment,
)

private val DarkColors = darkColorScheme(
    primary = DawnDark,
    onPrimary = Ink,
    background = Ink,
    onBackground = Parchment,
    surface = SurfaceDark,
    onSurface = Parchment,
    onSurfaceVariant = MutedDark,
    error = InterruptedDark,
    onError = Ink,
)

/** Wraps every screen so it uses Identity's colours; follows the phone's light/dark setting. */
@Composable
fun IdentityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
