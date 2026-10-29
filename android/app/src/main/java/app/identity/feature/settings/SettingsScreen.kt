package app.identity.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.identity.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.identity.filter.CustomRules
import app.identity.filter.TimeLimit
import app.identity.protection.CustomRule
import app.identity.protection.CustomRulesStore
import app.identity.protection.IdentityVpnService
import app.identity.protection.TikTokLimit
import app.identity.ui.components.ScreenColumn

/**
 * Settings tab. Explains what protection always blocks (D-028; no switches) and lets the user add
 * their own blocks (D-029): websites and words in site names, with a 10-minute undo.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { CustomRulesStore(context) }
    var rules by remember { mutableStateOf(store.load()) }

    fun add(kind: CustomRule.Kind, value: String): Int? {
        val result = store.add(kind, value)
        rules = store.load()
        if (result == CustomRulesStore.AddResult.ADDED) IdentityVpnService.reload(context)
        return when (result) {
            CustomRulesStore.AddResult.ADDED -> null
            CustomRulesStore.AddResult.ALREADY_ADDED -> R.string.settings_rule_error_duplicate
            CustomRulesStore.AddResult.TOO_MANY -> R.string.settings_rule_error_too_many
        }
    }

    ScreenColumn(modifier, title = stringResource(R.string.settings_title)) {
        SectionHeading(stringResource(R.string.settings_protection_heading))
        Text(
            text = stringResource(R.string.settings_protection_always_on),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TikTokLimitSection()

        SectionHeading(stringResource(R.string.settings_custom_heading))
        Text(
            text = stringResource(R.string.settings_custom_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RuleInput(
            label = stringResource(R.string.settings_site_label),
            hint = stringResource(R.string.settings_site_hint),
            invalidError = R.string.settings_site_error,
            keyboardType = KeyboardType.Uri,
            clean = CustomRules::normalizeSite,
            onAdd = { add(CustomRule.Kind.SITE, it) },
        )
        RuleInput(
            label = stringResource(R.string.settings_word_label),
            hint = stringResource(R.string.settings_word_hint),
            invalidError = R.string.settings_word_error,
            keyboardType = KeyboardType.Text,
            clean = CustomRules::normalizeWord,
            onAdd = { add(CustomRule.Kind.WORD, it) },
        )

        if (rules.isNotEmpty()) {
            SectionHeading(stringResource(R.string.settings_your_blocks_heading))
            for (rule in rules.sortedByDescending { it.addedAtMs }) {
                RuleRow(rule, removable = store.canRemove(rule)) {
                    store.remove(rule) // refused if the 10 minutes ran out meanwhile
                    rules = store.load()
                    IdentityVpnService.reload(context)
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .padding(top = 8.dp)
            .semantics { heading() },
    )
}

/** A text box and Add button. [clean] turns the input into a rule, or null if it isn't valid. */
@Composable
private fun RuleInput(
    label: String,
    hint: String,
    invalidError: Int,
    keyboardType: KeyboardType,
    clean: (String) -> String?,
    onAdd: (String) -> Int?,
) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }

    fun submit() {
        val value = clean(text)
        error = if (value == null) invalidError else onAdd(value)
        if (error == null) text = ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                error = null
            },
            label = { Text(label) },
            supportingText = { Text(error?.let { stringResource(it) } ?: hint) },
            isError = error != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { submit() }, enabled = text.isNotBlank()) {
            Text(stringResource(R.string.settings_rule_add))
        }
    }
}

/** One rule: what it blocks, and Undo while the 10-minute window is open, otherwise "Permanent". */
@Composable
private fun RuleRow(rule: CustomRule, removable: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(rule.value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = stringResource(
                    if (rule.kind == CustomRule.Kind.SITE) R.string.settings_rule_kind_site else R.string.settings_rule_kind_word,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (removable) {
            TextButton(onClick = onRemove) { Text(stringResource(R.string.settings_rule_undo)) }
        } else {
            Text(
                text = stringResource(R.string.settings_rule_permanent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * TikTok's daily allowance (M3.3c-1, D-030): two permission steps, then six fixed choices. Lowering
 * applies at once; raising starts tomorrow. Refreshes when the user comes back from system Settings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TikTokLimitSection() {
    val context = LocalContext.current
    val limit = remember { TikTokLimit(context) }
    var allowance by remember { mutableStateOf(limit.allowance()) }
    var usageAccess by remember { mutableStateOf(TikTokLimit.hasUsageAccess(context)) }
    var overlayAccess by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var usedMs by remember { mutableStateOf<Long?>(null) }

    fun refresh() {
        val nowUsageAccess = TikTokLimit.hasUsageAccess(context)
        val nowOverlay = Settings.canDrawOverlays(context)
        // A permission was just granted: let the running protection notice.
        if (nowUsageAccess != usageAccess || nowOverlay != overlayAccess) IdentityVpnService.reload(context)
        usageAccess = nowUsageAccess
        overlayAccess = nowOverlay
        allowance = limit.allowance()
        usedMs = if (nowUsageAccess) {
            try {
                limit.tallyToday().usedMs
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    SectionHeading(stringResource(R.string.settings_tiktok_heading))
    Text(
        text = stringResource(R.string.settings_tiktok_explanation),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (!usageAccess) {
        Text(stringResource(R.string.settings_tiktok_usage_access_needed), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) {
            Text(stringResource(R.string.settings_tiktok_usage_access_button))
        }
    } else if (!overlayAccess) {
        Text(stringResource(R.string.settings_tiktok_overlay_needed), style = MaterialTheme.typography.bodyMedium)
        Button(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
                )
            },
        ) { Text(stringResource(R.string.settings_tiktok_overlay_button)) }
    }

    if (usageAccess) {
        Text(stringResource(R.string.settings_tiktok_choose), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (minutes in TimeLimit.ALLOWANCE_CHOICES_MIN) {
                FilterChip(
                    selected = allowance?.currentMin == minutes,
                    onClick = {
                        limit.choose(minutes)
                        IdentityVpnService.reload(context)
                        refresh()
                    },
                    label = { Text(durationText(minutes)) },
                )
            }
        }
        val current = allowance
        val status = when {
            current == null -> stringResource(R.string.settings_tiktok_none)
            else -> stringResource(
                R.string.settings_tiktok_status,
                durationText(current.currentMin),
                durationText(((usedMs ?: 0L) / 60_000L).toInt()),
            )
        }
        Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        if (current != null && usedMs != null && TimeLimit.isOver(usedMs ?: 0L, current.currentMin)) {
            Text(stringResource(R.string.settings_tiktok_over), style = MaterialTheme.typography.bodyMedium)
        }
        current?.pendingMin?.let {
            Text(
                stringResource(R.string.settings_tiktok_pending, durationText(it)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!IdentityVpnService.isRunning) {
            Text(
                stringResource(R.string.settings_tiktok_off_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 15 -> "15 min", 60 -> "1 h", 90 -> "1 h 30 min". */
@Composable
private fun durationText(minutes: Int): String = when {
    minutes < 60 -> stringResource(R.string.duration_minutes, minutes)
    minutes % 60 == 0 -> stringResource(R.string.duration_hours, minutes / 60)
    else -> stringResource(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
}
