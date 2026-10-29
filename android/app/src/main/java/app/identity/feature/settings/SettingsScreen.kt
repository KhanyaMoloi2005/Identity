package app.identity.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.identity.R
import app.identity.ui.components.ScreenColumn

/** Settings tab. For now it only explains what protection always blocks (D-028); there are no switches. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    ScreenColumn(modifier, title = stringResource(R.string.settings_title)) {
        Text(
            text = stringResource(R.string.settings_protection_heading),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.settings_protection_always_on),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
