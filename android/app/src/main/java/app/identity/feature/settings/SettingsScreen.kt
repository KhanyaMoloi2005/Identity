package app.identity.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.identity.R
import app.identity.ui.components.ScreenColumn

/** Settings tab. Placeholder; entries are added as features arrive. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    ScreenColumn(modifier, title = stringResource(R.string.settings_title)) {
        Text(
            text = stringResource(R.string.settings_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
