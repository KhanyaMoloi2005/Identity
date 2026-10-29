package app.identity.feature.home

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import android.text.format.DateUtils
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
import app.identity.filter.InterruptionLog
import app.identity.filter.PrivateDns
import app.identity.filter.ProtectionHealth
import app.identity.protection.InterruptionStore
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
        HistoryCard()
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
            // What Identity actually observes (M3.5), not just the switch.
            val problems = IdentityVpnService.health?.problems.orEmpty()
            Text(
                text = stringResource(
                    when {
                        running && problems.isNotEmpty() -> R.string.home_protection_status_attention
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
                for (problem in problems) {
                    Text(
                        text = stringResource(problemText(problem)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
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

private fun problemText(problem: ProtectionHealth.Problem): Int = when (problem) {
    ProtectionHealth.Problem.DNS_HANDLER_STOPPED -> R.string.home_problem_dns_stopped
    ProtectionHealth.Problem.FILTER_SERVICE_UNREACHABLE -> R.string.home_problem_unreachable
    ProtectionHealth.Problem.PRIVATE_DNS_BYPASS -> R.string.home_problem_private_dns
}

private fun eventText(kind: InterruptionLog.Kind): Int = when (kind) {
    InterruptionLog.Kind.TURNED_ON -> R.string.history_turned_on
    InterruptionLog.Kind.TURNED_OFF -> R.string.history_turned_off
    InterruptionLog.Kind.TURNED_OFF_IN_ANDROID -> R.string.history_turned_off_in_android
    InterruptionLog.Kind.ENDED_UNEXPECTEDLY -> R.string.history_ended_unexpectedly
    InterruptionLog.Kind.DNS_HANDLER_STOPPED -> R.string.home_problem_dns_stopped
    InterruptionLog.Kind.FILTER_SERVICE_UNREACHABLE -> R.string.home_problem_unreachable
    InterruptionLog.Kind.PRIVATE_DNS_BYPASS -> R.string.home_problem_private_dns
    InterruptionLog.Kind.BLOCKLIST_UPDATED -> R.string.history_blocklist_updated
    InterruptionLog.Kind.BLOCKLIST_UPDATE_FAILED -> R.string.history_blocklist_failed
}

/** The last few things protection did, newest first. Kept on the phone for 30 days (M3.5). */
@Composable
private fun HistoryCard() {
    val context = LocalContext.current
    val running = IdentityVpnService.isRunning
    val health = IdentityVpnService.health
    var tick by remember { mutableStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) tick++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val events = remember(running, health, tick) { InterruptionStore(context).load().asReversed().take(HISTORY_SHOWN) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.history_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (events.isEmpty()) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            for (event in events) {
                Text(
                    text = stringResource(
                        R.string.history_row,
                        DateUtils.formatDateTime(
                            context,
                            event.timeMs,
                            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
                        ),
                        stringResource(eventText(event.kind)),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private const val HISTORY_SHOWN = 8
