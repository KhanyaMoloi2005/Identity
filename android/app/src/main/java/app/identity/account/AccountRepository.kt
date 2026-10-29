package app.identity.account

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    data class Failed(val error: AccountError? = null, val problem: FieldProblem? = null) : Outcome
}

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
        store.clear()
        mutableState.value = AccountState.SignedOut
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
                    store.clear()
                    mutableState.value = AccountState.SignedOut
                    null
                } else {
                    account.accessToken.takeIf { nowMs() < account.expiresAtSec * 1000 } // offline: still good until it expires
                }
            AuthResult.NeedsConfirmation -> null
        }
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
