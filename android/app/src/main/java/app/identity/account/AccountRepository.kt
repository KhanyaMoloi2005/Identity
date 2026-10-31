package app.identity.account

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.File
import java.time.Instant
import java.util.concurrent.Executors

/** What the Partner tab shows. */
internal sealed interface AccountState {
    data object Loading : AccountState
    data object SignedOut : AccountState

    /** [nickname] is null until it has been read from the server; [deviceRegistered] is false until this phone is registered. */
    data class SignedIn(val email: String, val nickname: String?, val deviceRegistered: Boolean) : AccountState
}

internal sealed interface Outcome {
    data object Done : Outcome

    /** The account was created but its email must be confirmed before signing in. */
    data object NeedsConfirmation : Outcome
    data class Failed(val error: AccountError? = null, val problem: FieldProblem? = null, val partner: PartnerError? = null) : Outcome
}

internal sealed interface InviteOutcome {
    /** The code is in [invitation] and nowhere else: show it once, then let it go. */
    data class Created(val invitation: NewInvitation) : InviteOutcome
    data class Failed(val failure: Outcome.Failed) : InviteOutcome
}

/** The partner lists. [partners] keeps the last good list while a refresh runs or fails (null before the first load). */
internal data class PartnersState(val partners: Partners? = null, val loading: Boolean = false, val error: AccountError? = null)

/**
 * Accounts (M4.2, D-037, D-038). An account is optional: nothing in protection depends on it, and
 * being signed out or offline never changes what Identity blocks. Every call here is blocking, so
 * callers use a background thread. Passwords are never stored; the saved login is encrypted.
 */
internal class AccountRepository(
    private val api: AccountApi,
    private val store: SessionStore,
    private val device: () -> DeviceInfo,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    private val mutableState = MutableStateFlow<AccountState>(AccountState.Loading)
    val state: StateFlow<AccountState> = mutableState
    private val mutablePartners = MutableStateFlow(PartnersState())
    val partners: StateFlow<PartnersState> = mutablePartners

    /** Reads the saved login, if any. Call once when the app starts. */
    @Synchronized
    fun load() {
        mutableState.value = store.load()?.let(::signedIn) ?: AccountState.SignedOut
    }

    @Synchronized
    fun signUp(email: String, password: String, nickname: String): Outcome {
        AccountValidation.email(email)?.let { return Outcome.Failed(problem = it) }
        AccountValidation.password(password)?.let { return Outcome.Failed(problem = it) }
        AccountValidation.nickname(nickname)?.let { return Outcome.Failed(problem = it) }
        return when (val result = api.signUp(AccountValidation.cleanEmail(email), password)) {
            is AuthResult.Signed -> finishSetup(result.session, AccountValidation.cleanNickname(nickname))
            AuthResult.NeedsConfirmation -> Outcome.NeedsConfirmation
            is AuthResult.Failed -> Outcome.Failed(error = result.error)
        }
    }

    @Synchronized
    fun signIn(email: String, password: String): Outcome {
        AccountValidation.email(email)?.let { return Outcome.Failed(problem = it) }
        if (password.isEmpty()) return Outcome.Failed(problem = FieldProblem.PASSWORD_TOO_SHORT)
        return when (val result = api.signIn(AccountValidation.cleanEmail(email), password)) {
            is AuthResult.Signed -> finishSetup(result.session, null)
            AuthResult.NeedsConfirmation -> Outcome.NeedsConfirmation
            is AuthResult.Failed -> Outcome.Failed(error = result.error)
        }
    }

    /**
     * Signs out of this phone only. The phone's registration and the login are removed here even if
     * the server can't be reached: being signed out always works.
     */
    @Synchronized
    fun signOut() {
        val account = store.load()
        if (account != null) {
            val token = freshToken(account)
            if (token != null) {
                account.deviceId?.let { api.deleteDevice(token, it) }
                api.signOut(token)
            }
        }
        signedOut()
    }

    /** Changes the nickname a partner sees. */
    @Synchronized
    fun setNickname(input: String): Outcome {
        AccountValidation.nickname(input)?.let { return Outcome.Failed(problem = it) }
        val account = store.load() ?: return Outcome.Failed(error = AccountError.SESSION_EXPIRED)
        val token = freshToken(account) ?: return Outcome.Failed(error = AccountError.NO_CONNECTION)
        val nickname = AccountValidation.cleanNickname(input)
        val saved = when (val r = api.setNickname(token, account.userId, nickname)) {
            is ApiResult.Ok -> if (r.value) ApiResult.Ok(Unit) else api.createProfile(token, account.userId, nickname)
            is ApiResult.Err -> r
        }
        if (saved is ApiResult.Err) return Outcome.Failed(error = saved.error)
        save(account.copy(nickname = nickname))
        return Outcome.Done
    }

    /**
     * Called when the app opens (and by the retry button): refreshes the login if needed, fills in
     * anything that failed earlier (nickname, this phone's registration), and marks the phone as
     * seen at most every 6 hours. Does nothing when signed out. Never throws.
     */
    @Synchronized
    fun touch() {
        val account = store.load() ?: return
        val token = freshToken(account) ?: return
        var current = store.load() ?: return // freshToken may have saved a new login
        if (current.nickname == null) {
            when (val r = api.getNickname(token, current.userId)) {
                is ApiResult.Ok -> {
                    val nickname = r.value ?: (api.createProfile(token, current.userId, DEFAULT_NICKNAME) as? ApiResult.Ok)?.let { DEFAULT_NICKNAME }
                    if (nickname != null) current = current.copy(nickname = nickname).also(::save)
                }
                is ApiResult.Err -> Unit
            }
        }
        val info = device()
        val id = current.deviceId
        if (id == null) {
            registerDevice(token, current, info)
        } else if (nowMs() - current.lastTouchMs >= TOUCH_EVERY_MS) {
            when (val r = api.touchDevice(token, id, info, Instant.ofEpochMilli(nowMs()).toString())) {
                is ApiResult.Ok -> if (r.value) save(current.copy(lastTouchMs = nowMs())) else registerDevice(token, current.copy(deviceId = null), info)
                is ApiResult.Err -> Unit
            }
        }
    }

    private fun finishSetup(session: Session, wantedNickname: String?): Outcome {
        var account = StoredAccount(session.accessToken, session.refreshToken, session.expiresAtSec, session.userId, session.email)
        save(account) // signed in from here on, even if the next steps can't be done yet
        val token = session.accessToken
        val nickname = when (val r = api.getNickname(token, session.userId)) {
            is ApiResult.Ok -> r.value ?: (wantedNickname ?: DEFAULT_NICKNAME).takeIf {
                api.createProfile(token, session.userId, it) is ApiResult.Ok // if this fails, touch() tries again later
            }
            is ApiResult.Err -> null // retried by touch()
        }
        account = account.copy(nickname = nickname).also(::save)
        registerDevice(token, account, device())
        return Outcome.Done
    }

    private fun registerDevice(token: String, account: StoredAccount, info: DeviceInfo) {
        when (val r = api.registerDevice(token, account.userId, info)) {
            is ApiResult.Ok -> save(account.copy(deviceId = r.value, lastTouchMs = nowMs()))
            is ApiResult.Err -> save(account.copy(deviceId = null)) // retried by touch(); a full account (10 phones) stays unregistered
        }
    }

    /** A usable access token, refreshing the login a minute before it runs out. Null if there is none (and the login is cleared if the server says it is no longer valid). */
    private fun freshToken(account: StoredAccount): String? {
        if (nowMs() < account.expiresAtSec * 1000 - REFRESH_MARGIN_MS) return account.accessToken
        return when (val r = api.refresh(account.refreshToken)) {
            is AuthResult.Signed -> {
                save(account.withSession(r.session))
                r.session.accessToken
            }
            is AuthResult.Failed ->
                if (r.error == AccountError.SESSION_EXPIRED || r.error == AccountError.WRONG_LOGIN) {
                    signedOut()
                    null
                } else {
                    account.accessToken.takeIf { nowMs() < account.expiresAtSec * 1000 } // offline: still good until it expires
                }
            AuthResult.NeedsConfirmation -> null
        }
    }

    // ---- Partners (M4.3, D-037). Every rule (one use, 48 hours, 3 partners, who may end or choose)
    // is enforced by the database; the app explains the answers and keeps its lists current.

    /** Reloads the partner lists. */
    @Synchronized
    fun refreshPartners(): Outcome {
        mutablePartners.value = mutablePartners.value.copy(loading = true, error = null)
        val outcome = withLogin { account, token -> reload(account, token) }
        if (outcome is Outcome.Failed) mutablePartners.value = mutablePartners.value.copy(loading = false, error = outcome.error)
        return outcome
    }

    /** Makes a one-time code to share. It is returned once and never saved. */
    @Synchronized
    fun createInvitation(): InviteOutcome {
        var created: NewInvitation? = null
        val outcome = withLogin { account, token ->
            when (val r = api.createInvitation(token)) {
                is RpcResult.Ok -> {
                    val id = r.fields.text("invitation_id")
                    val code = r.fields.text("code")
                    val expires = r.fields.text("expires_at")
                    if (id == null || code == null || expires == null) {
                        Outcome.Failed(error = AccountError.UNKNOWN)
                    } else {
                        created = NewInvitation(id, code, expires)
                        reload(account, token)
                        Outcome.Done
                    }
                }
                is RpcResult.Refused -> Outcome.Failed(partner = r.reason)
                is RpcResult.Err -> Outcome.Failed(error = r.error)
            }
        }
        return created?.let { InviteOutcome.Created(it) }
            ?: InviteOutcome.Failed(outcome as? Outcome.Failed ?: Outcome.Failed(error = AccountError.UNKNOWN))
    }

    /** Uses a code someone shared: this account becomes their partner. An obviously wrong code is refused here, so it costs no try. */
    @Synchronized
    fun acceptInvitation(code: String): Outcome {
        if (!InvitationCode.looksValid(code)) return Outcome.Failed(partner = PartnerError.INVALID_CODE)
        return change { token -> api.acceptInvitation(token, InvitationCode.normalize(code)) }
    }

    /** Cancels a code that has not been used. Already gone counts as done. */
    @Synchronized
    fun revokeInvitation(invitationId: String): Outcome = change(alreadyDone = true) { token -> api.revokeInvitation(token, invitationId) }

    /** Ends a connection, from either side. Already ended (perhaps by the other person) counts as done. */
    @Synchronized
    fun endConnection(partnershipId: String): Outcome = change(alreadyDone = true) { token -> api.endPartnership(token, partnershipId) }

    /** Changes what a partner may see. Only the supported person can, while the connection is active. */
    @Synchronized
    fun setSharing(partnershipId: String, sharing: Sharing): Outcome =
        withLogin { account, token ->
            when (val r = api.setSharing(token, partnershipId, sharing)) {
                is ApiResult.Ok ->
                    if (r.value) {
                        mutablePartners.value.partners?.let { current ->
                            val updated = current.connections.map { if (it.id == partnershipId) it.copy(sharing = sharing) else it }
                            mutablePartners.value = mutablePartners.value.copy(partners = current.copy(connections = updated))
                        }
                        Outcome.Done
                    } else {
                        reload(account, token)
                        Outcome.Failed(partner = PartnerError.NOT_FOUND)
                    }
                is ApiResult.Err -> Outcome.Failed(error = r.error)
            }
        }

    /** Runs one database function, then reloads the lists so the change shows. */
    private fun change(alreadyDone: Boolean = false, call: (String) -> RpcResult): Outcome =
        withLogin { account, token ->
            when (val r = call(token)) {
                is RpcResult.Ok -> {
                    reload(account, token)
                    Outcome.Done
                }
                is RpcResult.Refused ->
                    if (alreadyDone && r.reason == PartnerError.NOT_FOUND) {
                        reload(account, token)
                        Outcome.Done
                    } else {
                        Outcome.Failed(partner = r.reason)
                    }
                is RpcResult.Err -> Outcome.Failed(error = r.error)
            }
        }

    /** Runs [block] with the saved login and a usable token, or explains why there is none. */
    private inline fun withLogin(block: (StoredAccount, String) -> Outcome): Outcome {
        val account = store.load() ?: return Outcome.Failed(error = AccountError.SESSION_EXPIRED)
        val token = freshToken(account)
            ?: return Outcome.Failed(error = if (store.load() == null) AccountError.SESSION_EXPIRED else AccountError.NO_CONNECTION)
        return block(store.load() ?: account, token) // freshToken may have saved a new login
    }

    /** Reads the lists from the server and publishes them. A failure keeps the last good lists and is reported in the state. */
    private fun reload(account: StoredAccount, token: String): Outcome {
        val rows = when (val r = api.listPartnerships(token)) {
            is ApiResult.Ok -> r.value
            is ApiResult.Err -> return reloadFailed(r.error)
        }
        val invitations = when (val r = api.listOpenInvitations(token)) {
            is ApiResult.Ok -> r.value
            is ApiResult.Err -> return reloadFailed(r.error)
        }
        val now = nowMs()
        val me = account.userId
        val listed = rows.filter { row -> row.endedAt == null || epochMsOf(row.endedAt)?.let { now - it < Partners.SHOW_ENDED_MS } == true }
        fun other(row: PartnershipRow) = if (row.supportedId == me) row.partnerId else row.supportedId

        // The server shows a nickname only while connected; remember it so an ended connection still has a name.
        val fresh = (api.nicknames(token, listed.filter { it.endedAt == null }.map(::other).toSet()) as? ApiResult.Ok)?.value.orEmpty()
        val listedIds = listed.map(::other).toSet()
        val known = (account.knownNicknames + fresh).filterKeys { it in listedIds }
        if (known != account.knownNicknames) save(account.copy(knownNicknames = known))

        val connections = listed.map { row ->
            Connection(
                id = row.id,
                otherId = other(row),
                partnerOfMe = row.supportedId == me,
                nickname = known[other(row)],
                sinceIso = row.createdAt,
                endedIso = row.endedAt,
                endedByMe = row.endedBy == me,
                sharing = Sharing(row.shareStatus, row.shareBlockedCount, row.shareSettingChanges),
            )
        }
        val pending = invitations
            .filter { inv -> inv.usedAt == null && inv.revokedAt == null && epochMsOf(inv.expiresAt)?.let { it > now } == true }
            .map { PendingInvitation(it.id, it.expiresAt) }
        mutablePartners.value = PartnersState(Partners(connections, pending))
        return Outcome.Done
    }

    private fun reloadFailed(error: AccountError): Outcome {
        mutablePartners.value = mutablePartners.value.copy(loading = false, error = error)
        return Outcome.Failed(error = error)
    }

    private fun signedOut() {
        store.clear()
        mutableState.value = AccountState.SignedOut
        mutablePartners.value = PartnersState()
    }

    private fun save(account: StoredAccount) {
        store.save(account)
        mutableState.value = signedIn(account)
    }

    private fun signedIn(account: StoredAccount) =
        AccountState.SignedIn(account.email, account.nickname, deviceRegistered = account.deviceId != null)

    companion object {
        const val DEFAULT_NICKNAME = "Friend"
        private const val REFRESH_MARGIN_MS = 60_000L
        private const val TOUCH_EVERY_MS = 6 * 60 * 60 * 1000L

        @Volatile private var instance: AccountRepository? = null
        private val background = Executors.newSingleThreadExecutor()

        /** Runs [touch] off the main thread, ignoring any failure: the account is optional and must never get in the way. */
        fun touchInBackground(context: Context) {
            val appContext = context.applicationContext
            background.execute {
                try {
                    get(appContext).touch()
                } catch (e: Exception) {
                    // Nothing to do: it is retried the next time the app opens.
                }
            }
        }

        /** The one repository for the app, created on first use. */
        fun get(context: Context): AccountRepository =
            instance ?: synchronized(this) {
                instance ?: create(context.applicationContext).also { instance = it }
            }

        private fun create(context: Context): AccountRepository {
            val info = {
                val version = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
                } catch (e: Exception) {
                    ""
                }
                DeviceInfo(Build.MODEL.orEmpty(), Build.VERSION.RELEASE.orEmpty(), version)
            }
            val repository = AccountRepository(
                AccountApi(UrlConnectionTransport()),
                SessionStore(File(context.filesDir, "account"), KeystoreCipher()),
                info,
            )
            repository.load()
            return repository
        }
    }
}

private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
