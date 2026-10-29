package app.identity.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** A stand-in for the server that records every request, so the tests can check what was sent and in what order. */
private class FakeTransport : HttpTransport {
    class Request(val method: String, val path: String, val body: String?, val token: String?)

    val requests = mutableListOf<Request>()
    private val routes = mutableListOf<Triple<String, String, (Request) -> HttpResult>>()

    /** Later registrations win, so a test can change one answer. */
    fun on(method: String, pathStart: String, reply: (Request) -> HttpResult) {
        routes.add(0, Triple(method, pathStart, reply))
    }

    fun count(method: String, pathStart: String) = requests.count { it.method == method && it.path.startsWith(pathStart) }

    override fun send(method: String, path: String, body: String?, token: String?, headers: Map<String, String>): HttpResult {
        val request = Request(method, path, body, token)
        requests.add(request)
        val route = routes.firstOrNull { it.first == method && path.startsWith(it.second) }
        return route?.third?.invoke(request) ?: HttpResult(404, "")
    }
}

/** "Encrypts" by flipping bits, and can be told to lose its key. */
private class FakeCipher : SessionCipher {
    var keyLost = false
    override fun encrypt(plain: ByteArray) = ByteArray(plain.size) { (plain[it].toInt() xor 0x5A).toByte() }
    override fun decrypt(data: ByteArray): ByteArray? = if (keyLost) null else ByteArray(data.size) { (data[it].toInt() xor 0x5A).toByte() }
}

class AccountRepositoryTest {
    private val now = 1_800_000_000_000L
    private var clock = now
    private val transport = FakeTransport()
    private val cipher = FakeCipher()
    private val dir: File = Files.createTempDirectory("identity-account-test").toFile()
    private val store = SessionStore(dir, cipher)
    private val device = DeviceInfo("SM-A346E", "16", "0.0.2")
    private val repository = AccountRepository(AccountApi(transport) { clock / 1000 }, store, { device }, { clock })

    private fun signedJson(access: String = "AT1", refresh: String = "RT1", lifetimeSec: Long = 3600, id: String = "u-1", email: String = "a@b.co") =
        """{"access_token":"$access","refresh_token":"$refresh","expires_in":$lifetimeSec,"expires_at":${clock / 1000 + lifetimeSec},"user":{"id":"$id","email":"$email"}}"""

    private fun everythingWorks(nickname: String? = null) {
        transport.on("POST", "/auth/v1/signup") { HttpResult(200, signedJson()) }
        transport.on("POST", "/auth/v1/token?grant_type=password") { HttpResult(200, signedJson()) }
        transport.on("GET", "/rest/v1/profiles") { HttpResult(200, if (nickname == null) "[]" else """[{"nickname":"$nickname"}]""") }
        transport.on("POST", "/rest/v1/profiles") { HttpResult(201, "") }
        transport.on("PATCH", "/rest/v1/profiles") { HttpResult(200, """[{"nickname":"x"}]""") }
        transport.on("POST", "/rest/v1/devices") { HttpResult(201, """[{"id":"dev-1"}]""") }
        transport.on("PATCH", "/rest/v1/devices") { HttpResult(200, """[{"id":"dev-1"}]""") }
        transport.on("DELETE", "/rest/v1/devices") { HttpResult(200, "") }
        transport.on("POST", "/auth/v1/logout") { HttpResult(204, "") }
    }

    private fun signedIn() = repository.state.value as AccountState.SignedIn

    @Test
    fun signingUpCreatesTheProfileAndRegistersThePhoneInOrder() {
        everythingWorks()
        val outcome = repository.signUp("  Alex@Example.com ", "correct-horse", " Alex ")
        assertEquals(Outcome.Done, outcome)
        assertEquals(AccountState.SignedIn("a@b.co", "Alex", deviceRegistered = true), repository.state.value)
        assertEquals(
            listOf("/auth/v1/signup", "/rest/v1/profiles?id=eq.u-1&select=nickname", "/rest/v1/profiles", "/rest/v1/devices"),
            transport.requests.map { it.path },
        )
        assertTrue(transport.requests[0].body!!.contains("alex@example.com")) // tidied
        assertTrue(transport.requests[2].body!!.contains("\"nickname\":\"Alex\""))
        assertTrue(transport.requests[3].body!!.contains("SM-A346E")) // model and versions only
        assertEquals("AT1", transport.requests[2].token)
        val saved = store.load()!!
        assertEquals("dev-1", saved.deviceId)
        assertEquals("Alex", saved.nickname)
    }

    @Test
    fun badInputIsRefusedBeforeAnythingIsSent() {
        val results = listOf(
            repository.signUp("nope", "correct-horse", "Alex") to FieldProblem.EMAIL_INVALID,
            repository.signUp("a@b.co", "short", "Alex") to FieldProblem.PASSWORD_TOO_SHORT,
            repository.signUp("a@b.co", "correct-horse", "  ") to FieldProblem.NICKNAME_EMPTY,
        )
        for ((outcome, problem) in results) assertEquals(Outcome.Failed(problem = problem), outcome)
        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun anEmailThatIsAlreadyRegisteredIsExplained() {
        transport.on("POST", "/auth/v1/signup") { HttpResult(422, """{"code":422,"error_code":"user_already_exists","msg":"User already registered"}""") }
        repository.load()
        assertEquals(Outcome.Failed(error = AccountError.EMAIL_TAKEN), repository.signUp("a@b.co", "correct-horse", "Alex"))
        assertEquals(AccountState.SignedOut, repository.state.value)
        assertNull(store.load())
    }

    @Test
    fun anAccountWaitingForEmailConfirmationIsNotSignedIn() {
        transport.on("POST", "/auth/v1/signup") { HttpResult(200, """{"id":"u-2","email":"a@b.co","confirmation_sent_at":"2026-10-29T21:00:00Z"}""") }
        repository.load()
        assertEquals(Outcome.NeedsConfirmation, repository.signUp("a@b.co", "correct-horse", "Alex"))
        assertEquals(AccountState.SignedOut, repository.state.value)
    }

    @Test
    fun signingInKeepsTheNicknameFromTheServerAndDoesNotCreateAProfile() {
        everythingWorks(nickname = "Alex")
        assertEquals(Outcome.Done, repository.signIn("a@b.co", "whatever"))
        assertEquals("Alex", signedIn().nickname)
        assertEquals(0, transport.count("POST", "/rest/v1/profiles"))
    }

    @Test
    fun aWrongPasswordIsExplainedAndNothingIsSaved() {
        transport.on("POST", "/auth/v1/token?grant_type=password") { HttpResult(400, """{"code":400,"error_code":"invalid_credentials","msg":"Invalid login credentials"}""") }
        repository.load()
        assertEquals(Outcome.Failed(error = AccountError.WRONG_LOGIN), repository.signIn("a@b.co", "wrong"))
        assertNull(store.load())
    }

    @Test
    fun offlineAfterSigningInKeepsYouSignedInAndLaterFillsInTheRest() {
        everythingWorks()
        transport.on("GET", "/rest/v1/profiles") { HttpResult(0, "") }
        transport.on("POST", "/rest/v1/devices") { HttpResult(0, "") }
        assertEquals(Outcome.Done, repository.signUp("a@b.co", "correct-horse", "Alex"))
        assertEquals(AccountState.SignedIn("a@b.co", null, deviceRegistered = false), repository.state.value)

        everythingWorks() // the connection is back
        repository.touch()
        assertEquals(AccountState.SignedIn("a@b.co", AccountRepository.DEFAULT_NICKNAME, deviceRegistered = true), repository.state.value)
    }

    @Test
    fun aFailedProfileCreationIsRetriedInsteadOfBeingForgotten() {
        everythingWorks()
        transport.on("POST", "/rest/v1/profiles") { HttpResult(0, "") }
        repository.signUp("a@b.co", "correct-horse", "Alex")
        assertNull(signedIn().nickname)
    }

    @Test
    fun aLoginThatIsAboutToExpireIsRefreshedAndSaved() {
        everythingWorks(nickname = "Alex")
        repository.signIn("a@b.co", "x")
        clock += 3_600_000L - 30_000L // 30 seconds before it expires
        transport.on("POST", "/auth/v1/token?grant_type=refresh_token") { HttpResult(200, signedJson(access = "AT2", refresh = "RT2")) }
        repository.touch()
        assertEquals(1, transport.count("POST", "/auth/v1/token?grant_type=refresh_token"))
        assertEquals("AT2", store.load()!!.accessToken)
        assertEquals("RT2", store.load()!!.refreshToken)
        assertEquals("Alex", store.load()!!.nickname) // nothing else is lost
    }

    @Test
    fun aLoginTheServerNoLongerAcceptsSignsYouOut() {
        everythingWorks(nickname = "Alex")
        repository.signIn("a@b.co", "x")
        clock += 3_700_000L
        transport.on("POST", "/auth/v1/token?grant_type=refresh_token") { HttpResult(400, """{"code":400,"error_code":"validation_failed","msg":"Refresh token is not valid"}""") }
        repository.touch()
        assertEquals(AccountState.SignedOut, repository.state.value)
        assertNull(store.load())
    }

    @Test
    fun offlineAtRefreshTimeNeverSignsYouOut() {
        everythingWorks(nickname = "Alex")
        repository.signIn("a@b.co", "x")
        clock += 3_600_000L - 30_000L
        transport.on("POST", "/auth/v1/token?grant_type=refresh_token") { HttpResult(0, "") }
        repository.touch()
        assertNotNull(store.load())
        assertTrue(repository.state.value is AccountState.SignedIn)
    }

    @Test
    fun thePhoneIsMarkedSeenAtMostEverySixHours() {
        everythingWorks(nickname = "Alex")
        repository.signIn("a@b.co", "x")
        repository.touch()
        assertEquals(0, transport.count("PATCH", "/rest/v1/devices")) // just registered
        clock += 5 * 3_600_000L
        repository.touch()
        assertEquals(0, transport.count("PATCH", "/rest/v1/devices"))
        clock += 2 * 3_600_000L
        // the login is refreshed on the way (it lasts an hour)
        transport.on("POST", "/auth/v1/token?grant_type=refresh_token") { HttpResult(200, signedJson(access = "AT2", refresh = "RT2")) }
        repository.touch()
        assertEquals(1, transport.count("PATCH", "/rest/v1/devices"))
        assertTrue(transport.requests.last { it.path.startsWith("/rest/v1/devices") }.body!!.contains("last_seen_at"))
    }

    @Test
    fun aPhoneRemovedFromTheServerIsRegisteredAgain() {
        everythingWorks(nickname = "Alex")
        repository.signIn("a@b.co", "x")
        clock += 6 * 3_600_000L + 1
        transport.on("POST", "/auth/v1/token?grant_type=refresh_token") { HttpResult(200, signedJson(access = "AT2", refresh = "RT2")) }
        transport.on("PATCH", "/rest/v1/devices") { HttpResult(200, "[]") } // no such row any more
        transport.on("POST", "/rest/v1/devices") { HttpResult(201, """[{"id":"dev-2"}]""") }
        repository.touch()
        assertEquals("dev-2", store.load()!!.deviceId)
    }

    @Test
    fun anAccountThatIsFullOfPhonesStaysSignedInButUnregistered() {
        everythingWorks(nickname = "Alex")
        transport.on("POST", "/rest/v1/devices") { HttpResult(400, """{"code":"P0001","message":"device_limit"}""") }
        assertEquals(Outcome.Done, repository.signIn("a@b.co", "x"))
        assertEquals(AccountState.SignedIn("a@b.co", "Alex", deviceRegistered = false), repository.state.value)
    }

    @Test
    fun signingOutRemovesThePhoneAndEndsOnlyThisSession() {
        everythingWorks(nickname = "Alex")
        repository.signIn("a@b.co", "x")
        repository.signOut()
        assertEquals(AccountState.SignedOut, repository.state.value)
        assertNull(store.load())
        assertEquals("/rest/v1/devices?id=eq.dev-1", transport.requests.first { it.method == "DELETE" }.path)
        assertEquals("/auth/v1/logout?scope=local", transport.requests.last().path) // not "everywhere"
    }

    @Test
    fun signingOutAlwaysWorksEvenOffline() {
        everythingWorks(nickname = "Alex")
        repository.signIn("a@b.co", "x")
        transport.on("DELETE", "/rest/v1/devices") { HttpResult(0, "") }
        transport.on("POST", "/auth/v1/logout") { HttpResult(0, "") }
        repository.signOut()
        assertEquals(AccountState.SignedOut, repository.state.value)
        assertNull(store.load())
    }

    @Test
    fun theNicknameCanBeChangedAndInvalidOnesAreNotSent() {
        everythingWorks(nickname = "Alex")
        repository.signIn("a@b.co", "x")
        val before = transport.requests.size
        assertEquals(Outcome.Failed(problem = FieldProblem.NICKNAME_TOO_LONG), repository.setNickname("x".repeat(31)))
        assertEquals(before, transport.requests.size)
        assertEquals(Outcome.Done, repository.setNickname("  Sam  "))
        assertEquals("Sam", signedIn().nickname)
        assertTrue(transport.requests.last().body!!.contains("\"nickname\":\"Sam\""))
    }

    @Test
    fun nothingHappensWhenSignedOut() {
        repository.load()
        repository.touch()
        repository.signOut()
        assertTrue(transport.requests.isEmpty())
        assertEquals(AccountState.SignedOut, repository.state.value)
    }

    @Test
    fun theSavedLoginSurvivesARestart() {
        everythingWorks(nickname = "Alex")
        repository.signIn("a@b.co", "x")
        val restarted = AccountRepository(AccountApi(transport) { clock / 1000 }, SessionStore(dir, cipher), { device }, { clock })
        restarted.load()
        assertEquals(AccountState.SignedIn("a@b.co", "Alex", deviceRegistered = true), restarted.state.value)
    }

    @Test
    fun aSavedLoginWhoseKeyIsLostMeansSignedOut() {
        everythingWorks(nickname = "Alex")
        repository.signIn("a@b.co", "x")
        cipher.keyLost = true
        val restarted = AccountRepository(AccountApi(transport) { clock / 1000 }, SessionStore(dir, cipher), { device }, { clock })
        restarted.load()
        assertEquals(AccountState.SignedOut, restarted.state.value)
        assertFalse(File(dir, "account.bin").exists())
    }

    @Test
    fun theSavedFileDoesNotContainTheTokensInTheClear() {
        everythingWorks(nickname = "Alex")
        repository.signIn("a@b.co", "x")
        val onDisk = File(dir, "account.bin").readBytes().decodeToString()
        assertFalse(onDisk.contains("AT1"))
        assertFalse(onDisk.contains("RT1"))
        assertFalse(onDisk.contains("a@b.co"))
    }

    @Test
    fun noPasswordIsEverWrittenToDisk() {
        everythingWorks(nickname = "Alex")
        repository.signUp("a@b.co", "correct-horse-battery", "Alex")
        for (file in dir.listFiles().orEmpty()) {
            assertFalse(file.name, file.readBytes().decodeToString().contains("correct-horse-battery"))
        }
    }
}
