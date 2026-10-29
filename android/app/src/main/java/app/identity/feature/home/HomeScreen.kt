package app.identity.feature.home

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.provider.Settings
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.identity.R
import app.identity.filter.PrivateDns
import app.identity.protection.ProtectionPrefs
import app.identity.protection.IdentityVpnService
import app.identity.ui.brand.IdentityWordmark
import app.identity.ui.components.ScreenColumn

/** Home tab. Shows protection on/off (M2.2; observed status comes in M3) and later the "I'm struggling" card (M1.4). */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    ScreenColumn(modifier) {
        IdentityWordmark(fontSize = 34.sp)
        ProtectionCard()
        AttentionCards()
    }
}

/**
 * Things that quietly weaken protection (M3.4): a custom Private DNS provider sends lookups around
 * Identity (R-02), and without Always-on VPN a tap on "Turn off" in Android's settings or a
 * restart can leave the phone unprotected. Re-checked whenever the user comes back to the app.
 */
@Composable
private fun AttentionCards() {
    val context = LocalContext.current
    var privateDnsBypass by remember { mutableStateOf(isPrivateDnsBypass(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) privateDnsBypass = isPrivateDnsBypass(context)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    if (privateDnsBypass) {
        NoticeCard(
            title = R.string.home_private_dns_title,
            body = R.string.home_private_dns_body,
            button = R.string.home_private_dns_button,
            action = Settings.ACTION_WIRELESS_SETTINGS,
        )
    }
    if (IdentityVpnService.isRunning && !IdentityVpnService.alwaysOn) {
        NoticeCard(
            title = R.string.home_always_on_title,
            body = R.string.home_always_on_body,
            button = R.string.home_always_on_button,
            action = Settings.ACTION_VPN_SETTINGS,
        )
    }
}

@Composable
private fun NoticeCard(title: Int, body: Int, button: Int, action: String) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            OutlinedButton(
                onClick = { context.startActivity(Intent(action)) },
                modifier = Modifier.padding(top = 12.dp),
            ) { Text(stringResource(button)) }
        }
    }
}

private fun isPrivateDnsBypass(context: Context): Boolean =
    PrivateDns.bypassesProtection(Settings.Global.getString(context.contentResolver, "private_dns_mode"))

@Composable
private fun ProtectionCard() {
    val context = LocalContext.current
    val running = IdentityVpnService.isRunning
    // Meant to be on but not running: the service was ended, or couldn't restart after a reboot.
    val interrupted = !running && ProtectionPrefs(context).wantsProtection

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
                    when {
                        running -> R.string.home_protection_status_on
                        interrupted -> R.string.home_protection_status_interrupted
                        else -> R.string.home_protection_status_off
                    },
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
