package app.identity.feature.accountability

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.identity.R
import app.identity.account.AccountRepository
import app.identity.account.Connection
import app.identity.account.InviteOutcome
import app.identity.account.NewInvitation
import app.identity.account.Outcome
import app.identity.account.PartnerError
import app.identity.account.Partners
import app.identity.account.PendingInvitation
import app.identity.account.Sharing
import app.identity.account.epochMsOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/**
 * Partners (M4.3, D-037): invite someone with a one-time code, enter a code someone shared, see who
 * supports you and whom you support, choose what each partner sees, and end a connection from either
 * side. Every rule is checked by the database; this only shows the answers.
 */
@Composable
internal fun PartnersSection(repository: AccountRepository) {
    val scope = rememberCoroutineScope()
    val state by repository.partners.collectAsState()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }
    var newInvitation by remember { mutableStateOf<NewInvitation?>(null) } // shown once, never saved
    var confirmEnd by remember { mutableStateOf<Connection?>(null) }

    /** Runs [action] off the main thread and shows its failure, if any. */
    fun run(action: () -> Outcome) {
        busy = true
        message = null
        scope.launch {
            val outcome = withContext(Dispatchers.IO) { action() }
            busy = false
            message = (outcome as? Outcome.Failed)?.let(::partnerFailureText)
        }
    }

    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { repository.refreshPartners() } }

    val partners = state.partners
    val shownMessage = message ?: state.error?.let { partnerFailureText(Outcome.Failed(error = it)) }

    // ---- People who support me
    Heading(stringResource(R.string.partners_mine_heading))
    when {
        partners == null && state.loading -> Muted(stringResource(R.string.partners_loading))
        partners == null -> Unit
        partners.myPartners.isEmpty() -> Muted(stringResource(R.string.partners_mine_none))
        else -> partners.myPartners.forEach { connection ->
            MyPartnerCard(
                connection,
                enabled = !busy,
                onSharing = { sharing -> run { repository.setSharing(connection.id, sharing) } },
                onEnd = { confirmEnd = connection },
            )
        }
    }

    val invitation = newInvitation
    if (invitation != null) {
        CodeCard(invitation, onDone = { newInvitation = null })
    } else if (partners == null || partners.myPartners.size < Partners.MAX_PARTNERS) {
        Muted(stringResource(R.string.partners_invite_explain))
        Button(
            enabled = !busy && partners != null,
            onClick = {
                busy = true
                message = null
                scope.launch {
                    when (val outcome = withContext(Dispatchers.IO) { repository.createInvitation() }) {
                        is InviteOutcome.Created -> newInvitation = outcome.invitation
                        is InviteOutcome.Failed -> message = partnerFailureText(outcome.failure)
                    }
                    busy = false
                }
            },
        ) { Text(stringResource(R.string.partners_invite_button)) }
    } else {
        Muted(stringResource(R.string.partners_limit_reached))
    }

    partners?.invitations?.takeIf { it.isNotEmpty() }?.let { waiting ->
        Heading(stringResource(R.string.partners_waiting_heading))
        waiting.forEach { pending -> WaitingRow(pending, enabled = !busy) { run { repository.revokeInvitation(pending.id) } } }
    }

    // ---- People I support
    Heading(stringResource(R.string.partners_supporting_heading))
    when {
        partners == null -> Unit
        partners.peopleISupport.isEmpty() -> Muted(stringResource(R.string.partners_supporting_none))
        else -> partners.peopleISupport.forEach { connection ->
            SupportingCard(connection, enabled = !busy, onEnd = { confirmEnd = connection })
        }
    }
    EnterCode(enabled = !busy) { code -> run { repository.acceptInvitation(code) } }

    // ---- Connections that ended recently, so the change is noticed on both sides
    partners?.recentlyEnded?.takeIf { it.isNotEmpty() }?.let { ended ->
        Heading(stringResource(R.string.partners_ended_heading))
        ended.forEach { Muted(endedText(it)) }
    }

    shownMessage?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
    TextButton(enabled = !busy, onClick = { run { repository.refreshPartners() } }) {
        Text(stringResource(if (busy || state.loading) R.string.partner_working else R.string.partners_refresh))
    }

    confirmEnd?.let { connection ->
        AlertDialog(
            onDismissRequest = { confirmEnd = null },
            title = { Text(stringResource(R.string.partners_end_title, nameOf(connection))) },
            text = {
                Text(
                    stringResource(
                        if (connection.partnerOfMe) R.string.partners_end_text_mine else R.string.partners_end_text_supporting,
                        nameOf(connection),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = null
                    run { repository.endConnection(connection.id) }
                }) { Text(stringResource(R.string.partners_end_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmEnd = null }) { Text(stringResource(R.string.partners_cancel)) } },
        )
    }
}

/** Someone who supports me, with what they may see. Only I can change it. */
@Composable
private fun MyPartnerCard(connection: Connection, enabled: Boolean, onSharing: (Sharing) -> Unit, onEnd: () -> Unit) {
    ConnectionCard {
        Text(nameOf(connection), style = MaterialTheme.typography.titleMedium)
        Muted(stringResource(R.string.partners_since, dateOf(connection.sinceIso)))
        Text(stringResource(R.string.partners_sharing_heading), style = MaterialTheme.typography.bodyMedium)
        val sharing = connection.sharing
        ShareRow(stringResource(R.string.partners_share_status), sharing.status, enabled) { onSharing(sharing.copy(status = it)) }
        ShareRow(stringResource(R.string.partners_share_blocked), sharing.blockedCount, enabled) { onSharing(sharing.copy(blockedCount = it)) }
        ShareRow(stringResource(R.string.partners_share_changes), sharing.settingChanges, enabled) { onSharing(sharing.copy(settingChanges = it)) }
        Muted(stringResource(R.string.partners_sharing_note))
        TextButton(enabled = enabled, onClick = onEnd) { Text(stringResource(R.string.partners_end_button)) }
    }
}

/** Someone I support. They choose what I see. */
@Composable
private fun SupportingCard(connection: Connection, enabled: Boolean, onEnd: () -> Unit) {
    ConnectionCard {
        Text(nameOf(connection), style = MaterialTheme.typography.titleMedium)
        Muted(stringResource(R.string.partners_since, dateOf(connection.sinceIso)))
        Muted(stringResource(R.string.partners_supporting_note))
        TextButton(enabled = enabled, onClick = onEnd) { Text(stringResource(R.string.partners_end_button)) }
    }
}

/** The new code, shown this once, with Android's share sheet. */
@Composable
private fun CodeCard(invitation: NewInvitation, onDone: () -> Unit) {
    val context = LocalContext.current
    val shareText = stringResource(R.string.partners_share_message, invitation.code)
    val chooserTitle = stringResource(R.string.partners_share_button)
    ConnectionCard {
        Text(stringResource(R.string.partners_code_heading), style = MaterialTheme.typography.titleMedium)
        SelectionContainer {
            Text(invitation.code, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
        }
        Muted(stringResource(R.string.partners_code_note, dateTimeOf(invitation.expiresIso)))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                context.startActivity(Intent.createChooser(send, chooserTitle))
            }) { Text(chooserTitle) }
            OutlinedButton(onClick = onDone) { Text(stringResource(R.string.partners_code_done)) }
        }
    }
}

@Composable
private fun WaitingRow(pending: PendingInvitation, enabled: Boolean, onCancel: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Muted(stringResource(R.string.partners_waiting_item, dateTimeOf(pending.expiresIso)), Modifier.weight(1f))
        TextButton(enabled = enabled, onClick = onCancel) { Text(stringResource(R.string.partners_waiting_cancel)) }
    }
}

/** Where a partner enters the code they were sent. Accepting makes this account their partner. */
@Composable
private fun EnterCode(enabled: Boolean, onAccept: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Muted(stringResource(R.string.partners_enter_explain))
    OutlinedTextField(
        value = code,
        onValueChange = { code = it },
        label = { Text(stringResource(R.string.partners_enter_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        enabled = enabled && code.isNotBlank(),
        onClick = {
            onAccept(code)
            code = ""
        },
    ) { Text(stringResource(R.string.partners_enter_button)) }
}

@Composable
private fun ShareRow(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun ConnectionCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 16.dp).semantics { heading() },
    )
}

@Composable
private fun Muted(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Composable
private fun nameOf(connection: Connection) = connection.nickname ?: stringResource(R.string.partners_unknown_name)

@Composable
private fun endedText(connection: Connection): String {
    val name = nameOf(connection)
    val date = dateOf(connection.endedIso.orEmpty())
    return when {
        connection.endedByMe -> stringResource(R.string.partners_ended_by_me, name, date)
        else -> stringResource(R.string.partners_ended_by_other, name, date)
    }
}

private fun dateOf(iso: String): String = epochMsOf(iso)?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) } ?: ""

private fun dateTimeOf(iso: String): String =
    epochMsOf(iso)?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)) } ?: ""

/** A failure about partners, in plain words. Falls back to the account messages. */
internal fun partnerFailureText(failure: Outcome.Failed): Int =
    when (failure.partner) {
        PartnerError.INVALID_CODE -> R.string.partners_error_invalid
        PartnerError.CODE_USED -> R.string.partners_error_used
        PartnerError.CODE_EXPIRED -> R.string.partners_error_expired
        PartnerError.OWN_CODE -> R.string.partners_error_self
        PartnerError.ALREADY_PARTNERS -> R.string.partners_error_already
        PartnerError.PARTNER_LIMIT -> R.string.partners_error_limit
        PartnerError.TOO_MANY_PENDING -> R.string.partners_error_pending
        PartnerError.TOO_MANY_TRIES -> R.string.partners_error_tries
        PartnerError.NOT_FOUND -> R.string.partners_error_gone
        null -> failureText(failure)
    }
