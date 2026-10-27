package app.identity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.identity.ui.WelcomeScreen
import app.identity.ui.theme.IdentityTheme

/** The app's single entry point. Android starts this when the user taps the Identity icon. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IdentityTheme {
                WelcomeScreen()
            }
        }
    }
}
