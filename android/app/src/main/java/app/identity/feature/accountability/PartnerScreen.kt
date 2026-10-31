package app.identity.feature.accountability

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.identity.R
import app.identity.account.AccountError
import app.identity.account.AccountRepository
import app.identity.account.AccountState
import app.identity.account.FieldProblem
import app.identity.account.Outcome
import app.identity.ui.components.ScreenColumn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Partner tab (M4.2). An account is optional: filtering, limits and Scripture never need one. Signed
 * out, it explains what is shared and offers to create an account or sign in. Signed in, it shows
 * the account, the nickname a partner will see, this phone's registration, and the partners (M4.3).
 */
@Composable
fun PartnerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { AccountRepository.get(context) }
    val state by repository.state.collectAsState()

    ScreenColumn(modifier, title = stringResource(R.string.partner_title)) {
        Text(
            text = stringResource(R.string.partner_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (val current = state) {
            AccountState.Loading -> Unit
            AccountState.SignedOut -> SignedOutSection(repository)
            is AccountState.SignedIn -> SignedInSection(repository, current)
        }
    }
}

@Composable
private fun SignedOutSection(repository: AccountRepository) {
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }

    fun submit() {
        busy = true
        message = null
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                if (creating) repository.signUp(email, password, nickname) else repository.signIn(email, password)
            }
            busy = false
            when (outcome) {
                Outcome.Done -> password = ""
                Outcome.NeedsConfirmation -> message = R.string.partner_confirm_email
                is Outcome.Failed -> message = failureText(outcome)
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (creating) {
            Button(onClick = {}, enabled = false) { Text(stringResource(R.string.partner_create_tab)) }
            OutlinedButton(onClick = { creating = false; message = null }) { Text(stringResource(R.string.partner_signin_tab)) }
        } else {
            OutlinedButton(onClick = { creating = true; message = null }) { Text(stringResource(R.string.partner_create_tab)) }
            Button(onClick = {}, enabled = false) { Text(stringResource(R.string.partner_signin_tab)) }
        }
    }

    OutlinedTextField(
        value = email,
        onValueChange = { email = it; message = null },
        label = { Text(stringResource(R.string.partner_email)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it; message = null },
        label = { Text(stringResource(R.string.partner_password)) },
        supportingText = if (creating) ({ Text(stringResource(R.string.partner_password_hint)) }) else null,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
    if (creating) {
        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it; message = null },
            label = { Text(stringResource(R.string.partner_nickname)) },
            supportingText = { Text(stringResource(R.string.partner_nickname_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.partner_what_is_shared),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = consent, onCheckedChange = { consent = it })
            Text(
                text = stringResource(R.string.partner_consent),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
    }
    message?.let {
        Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    }
    Button(
        onClick = { submit() },
        enabled = !busy && email.isNotBlank() && password.isNotEmpty() && (!creating || (consent && nickname.isNotBlank())),
    ) {
        Text(stringResource(if (busy) R.string.partner_working else if (creating) R.string.partner_create_button else R.string.partner_signin_button))
    }
}

@Composable
private fun SignedInSection(repository: AccountRepository, state: AccountState.SignedIn) {
    val scope = rememberCoroutineScope()
    var edited by remember(state.nickname) { mutableStateOf(state.nickname.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }

    Text(
        text = stringResource(R.string.partner_signed_in_as, state.email),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.semantics { heading() },
    )

    OutlinedTextField(
        value = edited,
        onValueChange = { edited = it; message = null },
        label = { Text(stringResource(R.string.partner_nickname)) },
        supportingText = { Text(stringResource(R.string.partner_nickname_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
    Button(
        enabled = !busy && edited.isNotBlank() && edited.trim() != state.nickname,
        onClick = {
            busy = true
            scope.launch {
                val outcome = withContext(Dispatchers.IO) { repository.setNickname(edited) }
                busy = false
                message = (outcome as? Outcome.Failed)?.let(::failureText)
            }
        },
    ) { Text(stringResource(R.string.partner_save_nickname)) }

    Text(
        text = stringResource(if (state.deviceRegistered) R.string.partner_phone_registered else R.string.partner_phone_not_registered),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (!state.deviceRegistered || state.nickname == null) {
        OutlinedButton(
            enabled = !busy,
            onClick = {
                busy = true
                scope.launch {
                    withContext(Dispatchers.IO) { repository.touch() }
                    busy = false
                }
            },
        ) { Text(stringResource(R.string.partner_retry)) }
    }

    PartnersSection(repository)

    TextButton(
        modifier = Modifier.padding(top = 8.dp),
        enabled = !busy,
        onClick = {
            busy = true
            scope.launch {
                withContext(Dispatchers.IO) { repository.signOut() }
                busy = false
            }
        },
    ) { Text(stringResource(R.string.partner_sign_out)) }
}

internal fun failureText(failure: Outcome.Failed): Int =
    when (failure.problem) {
        FieldProblem.EMAIL_INVALID -> R.string.partner_error_email
        FieldProblem.PASSWORD_TOO_SHORT -> R.string.partner_error_password_short
        FieldProblem.PASSWORD_TOO_LONG -> R.string.partner_error_password_long
        FieldProblem.NICKNAME_EMPTY -> R.string.partner_error_nickname_empty
        FieldProblem.NICKNAME_TOO_LONG -> R.string.partner_error_nickname_long
        null -> when (failure.error) {
            AccountError.EMAIL_TAKEN -> R.string.partner_error_taken
            AccountError.WRONG_LOGIN -> R.string.partner_error_wrong_login
            AccountError.WEAK_PASSWORD -> R.string.partner_error_weak
            AccountError.INVALID_EMAIL -> R.string.partner_error_email
            AccountError.RATE_LIMITED -> R.string.partner_error_rate
            AccountError.NO_CONNECTION -> R.string.partner_error_offline
            AccountError.SESSION_EXPIRED -> R.string.partner_error_session
            else -> R.string.partner_error_generic
        }
    }
