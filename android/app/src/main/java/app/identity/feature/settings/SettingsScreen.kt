package app.identity.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.identity.R
import app.identity.filter.CustomRules
import app.identity.protection.CustomRule
import app.identity.protection.CustomRulesStore
import app.identity.protection.IdentityVpnService
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
