package app.identity.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.Instant

/**
 * Partners (M4.3). The server answers below are copied from the real Supabase project (captured
 * 2026-10-30 with two throw-away accounts), with the ids shortened.
 */
class PartnersTest {
    private val now = Instant.parse("2026-10-30T10:00:00Z").toEpochMilli()
    private var clock = now
    private val transport = FakeTransport()
    private val cipher = FakeCipher()
    private val dir: File = Files.createTempDirectory("identity-partners-test").toFile()
    private val store = SessionStore(dir, cipher)
    private val repository = AccountRepository(AccountApi(transport) { clock / 1000 }, store, { DeviceInfo("m", "16", "1") }, { clock })

    private val code = "24D2-332D-255D-E93B-7422-564C-9EE9-1AE3"

    /** Sam supports me (I chose what Sam sees); I support Alex. */
    private var partnerships = """[
        {"id":"p-sam","supported_id":"me","partner_id":"sam","created_at":"2026-10-29T08:07:10.007457+00:00","ended_at":null,"ended_by":null,"share_status":true,"share_blocked_count":false,"share_setting_changes":false},
        {"id":"p-alex","supported_id":"alex","partner_id":"me","created_at":"2026-10-28T08:07:10.007457+00:00","ended_at":null,"ended_by":null,"share_status":true,"share_blocked_count":true,"share_setting_changes":false}
    ]"""
    private var profiles = """[{"id":"sam","nickname":"Sam"},{"id":"alex","nickname":"Alex"}]"""
    private var invitations = """[{"id":"inv-1","expires_at":"2026-11-01T08:07:08.819869+00:00","used_at":null,"revoked_at":null}]"""

    private fun signedIn() {
        store.save(StoredAccount("AT1", "RT1", now / 1000 + 3600, "me", "me@example.com", nickname = "Me", deviceId = "dev-1", lastTouchMs = now))
        repository.load()
        transport.on("GET", "/rest/v1/partnerships") { HttpResult(200, partnerships) }
        transport.on("GET", "/rest/v1/profiles?id=in.") { HttpResult(200, profiles) }
        transport.on("GET", "/rest/v1/invitations") { HttpResult(200, invitations) }
    }

    private fun rpc(name: String, status: Int = 200, body: String) = transport.on("POST", "/rest/v1/rpc/$name") { HttpResult(status, body) }

    private fun lists() = repository.partners.value.partners!!

    @Test
    fun theListsSortPeopleByWhoSupportsWhomAndShowOnlyNicknames() {
        signedIn()
        assertEquals(Outcome.Done, repository.refreshPartners())
        val partners = lists()
        assertEquals(listOf("Sam"), partners.myPartners.map { it.nickname })
        assertEquals(listOf("Alex"), partners.peopleISupport.map { it.nickname })
        assertEquals(Sharing(status = true, blockedCount = false, settingChanges = false), partners.myPartners.single().sharing)
        assertEquals(listOf(PendingInvitation("inv-1", "2026-11-01T08:07:08.819869+00:00")), partners.invitations)
        assertTrue(partners.recentlyEnded.isEmpty())
    }

    @Test
    fun invitationsAreReadWithNamedColumnsNeverEverything() {
        signedIn()
        repository.refreshPartners()
        val path = transport.requests.single { it.path.startsWith("/rest/v1/invitations") }.path
        assertTrue(path, path.contains("select=id,expires_at,used_at,revoked_at"))
        assertFalse(path.contains("select=*"))
        assertFalse(path.contains("code_hash"))
    }

    @Test
    fun expiredUsedAndCancelledInvitationsAreNotListed() {
        invitations = """[
            {"id":"old","expires_at":"2026-10-30T09:59:59+00:00","used_at":null,"revoked_at":null},
            {"id":"used","expires_at":"2026-11-01T08:00:00+00:00","used_at":"2026-10-30T08:00:00+00:00","revoked_at":null},
            {"id":"live","expires_at":"2026-11-01T08:00:00+00:00","used_at":null,"revoked_at":null}
        ]"""
        signedIn()
        repository.refreshPartners()
        assertEquals(listOf("live"), lists().invitations.map { it.id })
    }

    @Test
    fun aNewCodeIsReturnedOnceAndNeverSaved() {
        signedIn()
        rpc("create_invitation", body = """{"ok": true, "code": "$code", "expires_at": "2026-11-01T08:07:08.819869+00:00", "invitation_id": "inv-2"}""")
        val outcome = repository.createInvitation()
        assertEquals(InviteOutcome.Created(NewInvitation("inv-2", code, "2026-11-01T08:07:08.819869+00:00")), outcome)
        val saved = File(dir, "account.bin").readBytes().let { cipher.decrypt(it)!!.decodeToString() }
        assertFalse(saved.contains(code))
        assertEquals(1, transport.count("GET", "/rest/v1/partnerships")) // lists reloaded
    }

    @Test
    fun aRefusedInvitationIsExplained() {
        signedIn()
        rpc("create_invitation", body = """{"ok": false, "error": "partner_limit"}""")
        assertEquals(InviteOutcome.Failed(Outcome.Failed(partner = PartnerError.PARTNER_LIMIT)), repository.createInvitation())
        rpc("create_invitation", body = """{"ok": false, "error": "too_many_pending"}""")
        assertEquals(InviteOutcome.Failed(Outcome.Failed(partner = PartnerError.TOO_MANY_PENDING)), repository.createInvitation())
    }

    @Test
    fun anObviouslyWrongCodeIsRefusedWithoutUsingATry() {
        signedIn()
        for (bad in listOf("ABCD", "$code-FF", "ZZZZ-332D-255D-E93B-7422-564C-9EE9-1AE3")) {
            assertEquals(Outcome.Failed(partner = PartnerError.INVALID_CODE), repository.acceptInvitation(bad))
        }
        assertEquals(0, transport.count("POST", "/rest/v1/rpc/accept_invitation"))
    }

    @Test
    fun aCodeIsTidiedBeforeItIsSentAndTheListsReload() {
        signedIn()
        rpc("accept_invitation", body = """{"ok": true, "partnership_id": "p-new"}""")
        assertEquals(Outcome.Done, repository.acceptInvitation("  " + code.lowercase().replace("-", " ") + " "))
        val sent = transport.requests.single { it.path == "/rest/v1/rpc/accept_invitation" }
        assertEquals("""{"p_code":"24D2332D255DE93B7422564C9EE91AE3"}""", sent.body)
        assertEquals("AT1", sent.token)
        assertEquals(listOf("Alex"), lists().peopleISupport.map { it.nickname })
    }

    @Test
    fun everyRefusalOfACodeHasItsOwnReason() {
        signedIn()
        val cases = mapOf(
            "invalid" to PartnerError.INVALID_CODE,
            "used" to PartnerError.CODE_USED,
            "expired" to PartnerError.CODE_EXPIRED,
            "self" to PartnerError.OWN_CODE,
            "already_partners" to PartnerError.ALREADY_PARTNERS,
            "partner_limit" to PartnerError.PARTNER_LIMIT,
            "rate_limited" to PartnerError.TOO_MANY_TRIES,
        )
        for ((server, expected) in cases) {
            rpc("accept_invitation", body = """{"ok": false, "error": "$server"}""")
            assertEquals(server, Outcome.Failed(partner = expected), repository.acceptInvitation(code))
        }
    }

    @Test
    fun anEndedConnectionKeepsTheNameSeenBeforeAndSaysWhoEndedIt() {
        signedIn()
        repository.refreshPartners() // Sam's nickname is seen while connected
        rpc("end_partnership", body = """{"ok": true}""")
        partnerships = """[{"id":"p-sam","supported_id":"me","partner_id":"sam","created_at":"2026-10-29T08:07:10+00:00","ended_at":"2026-10-30T09:00:00+00:00","ended_by":"sam","share_status":true,"share_blocked_count":false,"share_setting_changes":false}]"""
        profiles = "[]" // the server hides the nickname once the connection has ended
        assertEquals(Outcome.Done, repository.endConnection("p-sam"))
        val ended = lists().recentlyEnded.single()
        assertEquals("Sam", ended.nickname)
        assertFalse(ended.endedByMe)
        assertTrue(lists().myPartners.isEmpty())
        assertEquals(mapOf("sam" to "Sam"), store.load()!!.knownNicknames) // Alex is no longer listed, so forgotten
    }

    @Test
    fun anEndedConnectionDropsOffTheListAfterAWeekAndItsNameIsForgotten() {
        signedIn()
        repository.refreshPartners()
        partnerships = """[{"id":"p-sam","supported_id":"me","partner_id":"sam","created_at":"2026-10-01T08:00:00+00:00","ended_at":"2026-10-20T09:00:00+00:00","ended_by":"me","share_status":true,"share_blocked_count":false,"share_setting_changes":false}]"""
        profiles = "[]"
        repository.refreshPartners()
        assertTrue(lists().connections.isEmpty())
        assertTrue(store.load()!!.knownNicknames.isEmpty())
    }

    @Test
    fun endingWhatTheOtherPersonAlreadyEndedCountsAsDone() {
        signedIn()
        rpc("end_partnership", body = """{"ok": false, "error": "not_found"}""")
        assertEquals(Outcome.Done, repository.endConnection("p-sam"))
        rpc("revoke_invitation", body = """{"ok": false, "error": "not_found"}""")
        assertEquals(Outcome.Done, repository.revokeInvitation("inv-1"))
        assertEquals("""{"p_invitation_id":"inv-1"}""", transport.requests.last { it.path == "/rest/v1/rpc/revoke_invitation" }.body)
    }

    @Test
    fun sharingChoicesAreSavedAndShown() {
        signedIn()
        repository.refreshPartners()
        transport.on("PATCH", "/rest/v1/partnerships") { HttpResult(200, """[{"id":"p-sam"}]""") }
        val wanted = Sharing(status = true, blockedCount = true, settingChanges = true)
        assertEquals(Outcome.Done, repository.setSharing("p-sam", wanted))
        val sent = transport.requests.last { it.method == "PATCH" }
        assertEquals("/rest/v1/partnerships?id=eq.p-sam", sent.path)
        assertEquals("""{"share_status":true,"share_blocked_count":true,"share_setting_changes":true}""", sent.body)
        assertEquals(wanted, lists().myPartners.single().sharing)
    }

    @Test
    fun sharingThatTheServerDidNotChangeIsReported() {
        signedIn()
        repository.refreshPartners()
        transport.on("PATCH", "/rest/v1/partnerships") { HttpResult(200, "[]") } // real answer when not allowed or ended
        assertEquals(Outcome.Failed(partner = PartnerError.NOT_FOUND), repository.setSharing("p-sam", Sharing(blockedCount = true)))
        assertEquals(Sharing(), lists().myPartners.single().sharing)
    }

    @Test
    fun offlineKeepsTheLastListsAndSaysSo() {
        signedIn()
        repository.refreshPartners()
        transport.on("GET", "/rest/v1/partnerships") { HttpResult(0, "") }
        assertEquals(Outcome.Failed(error = AccountError.NO_CONNECTION), repository.refreshPartners())
        val state = repository.partners.value
        assertEquals(AccountError.NO_CONNECTION, state.error)
        assertFalse(state.loading)
        assertEquals(listOf("Sam"), state.partners!!.myPartners.map { it.nickname })
        rpc("accept_invitation", status = 0, body = "")
        assertEquals(Outcome.Failed(error = AccountError.NO_CONNECTION), repository.acceptInvitation(code))
    }

    @Test
    fun aRefusedLoginOnAFunctionMeansSignInAgain() {
        signedIn()
        rpc("create_invitation", status = 401, body = """{"code": "42501", "details": null, "hint": null, "message": "permission denied for function create_invitation"}""")
        assertEquals(InviteOutcome.Failed(Outcome.Failed(error = AccountError.SESSION_EXPIRED)), repository.createInvitation())
    }

    @Test
    fun signedOutThereIsNothingToShowAndNothingIsSent() {
        repository.load()
        assertEquals(Outcome.Failed(error = AccountError.SESSION_EXPIRED), repository.refreshPartners())
        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun signingOutForgetsThePartnerLists() {
        signedIn()
        transport.on("DELETE", "/rest/v1/devices") { HttpResult(200, "") }
        transport.on("POST", "/auth/v1/logout") { HttpResult(204, "") }
        repository.refreshPartners()
        repository.signOut()
        assertNull(repository.partners.value.partners)
        assertNull(store.load())
    }

    @Test
    fun codesAreCheckedForShapeOnly() {
        assertTrue(InvitationCode.looksValid(code))
        assertTrue(InvitationCode.looksValid(code.lowercase().replace("-", "")))
        assertFalse(InvitationCode.looksValid(code.dropLast(1)))
        assertEquals("ABCD12", InvitationCode.normalize(" ab-cd 12 "))
    }
}
