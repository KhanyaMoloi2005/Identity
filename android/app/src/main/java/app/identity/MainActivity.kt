package app.identity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.identity.protection.IdentityVpnService
import app.identity.ui.navigation.IdentityApp
import app.identity.ui.theme.IdentityTheme

/** The app's single entry point. Android starts this when the user taps the Identity icon. */
class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        // The user is looking at the app, so protection that should be on but was ended can start again.
        IdentityVpnService.resumeIfWanted(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IdentityTheme {
                IdentityApp()
            }
        }
    }
}
