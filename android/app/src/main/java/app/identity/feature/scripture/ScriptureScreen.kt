package app.identity.feature.scripture

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.identity.R
import app.identity.ui.components.ScreenColumn
import app.identity.ui.theme.ScriptureBody

/** Scripture tab. Placeholder until topics and verses arrive (M1.2–M1.5). Shows a sample verse in the reading font. */
@Composable
fun ScriptureScreen(modifier: Modifier = Modifier) {
    ScreenColumn(modifier, title = stringResource(R.string.scripture_title)) {
        Text(
            text = stringResource(R.string.scripture_sample_text),
            style = ScriptureBody,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.scripture_sample_reference),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.scripture_placeholder),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
