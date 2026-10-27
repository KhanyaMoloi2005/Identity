package app.identity.feature.accountability

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.identity.R
import app.identity.ui.components.ScreenColumn

/** Partner tab. Placeholder until accountability arrives (M4). */
@Composable
fun PartnerScreen(modifier: Modifier = Modifier) {
    ScreenColumn(modifier, title = stringResource(R.string.partner_title)) {
        Text(
            text = stringResource(R.string.partner_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
