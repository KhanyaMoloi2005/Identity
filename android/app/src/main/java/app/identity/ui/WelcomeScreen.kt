package app.identity.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.identity.R
import app.identity.ui.brand.IdentityWordmark
import app.identity.ui.theme.IdentityTheme

/** Temporary M0 screen: proves the app, theme and brand mark work. Replaced by real navigation in M1. */
@Composable
fun WelcomeScreen(modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IdentityWordmark()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.welcome_status),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(name = "Light")
@Composable
private fun WelcomeLightPreview() {
    IdentityTheme(darkTheme = false) { WelcomeScreen() }
}

@Preview(name = "Dark")
@Composable
private fun WelcomeDarkPreview() {
    IdentityTheme(darkTheme = true) { WelcomeScreen() }
}
