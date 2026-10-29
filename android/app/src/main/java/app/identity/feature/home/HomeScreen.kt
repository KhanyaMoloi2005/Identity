package app.identity.feature.home

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.identity.R
import app.identity.protection.IdentityVpnService
import app.identity.ui.brand.IdentityWordmark
import app.identity.ui.components.ScreenColumn

/** Home tab. Shows protection on/off (M2.2; observed status comes in M3) and later the "I'm struggling" card (M1.4). */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    ScreenColumn(modifier) {
        IdentityWordmark(fontSize = 34.sp)
        ProtectionCard()
    }
}

@Composable
private fun ProtectionCard() {
    val context = LocalContext.current
    val running = IdentityVpnService.isRunning

    // Step 3: Android's own "Connection request" dialog. Only "OK" starts protection.
    val vpnConsent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) IdentityVpnService.start(context)
    }
    // Step 2: ask for the VPN unless the user already allowed it before.
    fun requestVpn() {
        val consentIntent = VpnService.prepare(context)
        if (consentIntent == null) IdentityVpnService.start(context) else vpnConsent.launch(consentIntent)
    }
    // Step 1 (Android 13+): notification permission. Protection starts whatever the answer;
    // without it Android hides the notification, but the VPN key icon still shows.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        requestVpn()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_protection_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    if (running) R.string.home_protection_status_on else R.string.home_protection_status_off,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (running) {
                OutlinedButton(
                    onClick = { IdentityVpnService.stop(context) },
                    modifier = Modifier.padding(top = 12.dp),
                ) { Text(stringResource(R.string.home_protection_turn_off)) }
            } else {
                Button(
                    onClick = {
                        if (needsNotificationPermission(context)) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            requestVpn()
                        }
                    },
                    modifier = Modifier.padding(top = 12.dp),
                ) { Text(stringResource(R.string.home_protection_turn_on)) }
            }
        }
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
