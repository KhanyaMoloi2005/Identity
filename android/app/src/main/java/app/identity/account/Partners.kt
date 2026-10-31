package app.identity.account

import java.time.OffsetDateTime

/** Why an invitation or a connection change was refused, as the database functions report it (M4.3). */
internal enum class PartnerError {
    INVALID_CODE, CODE_USED, CODE_EXPIRED, OWN_CODE, ALREADY_PARTNERS, PARTNER_LIMIT, TOO_MANY_PENDING, TOO_MANY_TRIES, NOT_FOUND;

    companion object {
        /** The `error` value of a `{ "ok": false }` answer. Null for anything unexpected. */
        fun fromServer(code: String?): PartnerError? =
            when (code) {
                "invalid" -> INVALID_CODE
                "used" -> CODE_USED
                "expired" -> CODE_EXPIRED
                "self" -> OWN_CODE
                "already_partners" -> ALREADY_PARTNERS
                "partner_limit" -> PARTNER_LIMIT
                "too_many_pending" -> TOO_MANY_PENDING
                "rate_limited" -> TOO_MANY_TRIES
                "not_found" -> NOT_FOUND
                else -> null
            }
    }
}

/** What the supported person lets a partner see (stored now, used from M5). Defaults as decided in D-037. */
internal data class Sharing(val status: Boolean = true, val blockedCount: Boolean = false, val settingChanges: Boolean = false)

/**
 * One connection between two people. [partnerOfMe] is true when the other person supports me (I chose
 * what they see); false when I support them. [nickname] is null when it has never been seen on this phone.
 */
internal data class Connection(
    val id: String,
    val otherId: String,
    val partnerOfMe: Boolean,
    val nickname: String?,
    val sinceIso: String,
    val endedIso: String?,
    val endedByMe: Boolean,
    val sharing: Sharing,
) {
    val active get() = endedIso == null
}

/** An invitation that has not been used, cancelled or run out. The code itself is never stored. */
internal data class PendingInvitation(val id: String, val expiresIso: String)

/** A newly made invitation: the only moment the code exists on the phone. */
internal data class NewInvitation(val id: String, val code: String, val expiresIso: String)

/** Everything the Partner tab lists. Ended connections are kept for [Partners.SHOW_ENDED_MS] so the change is noticed. */
internal data class Partners(val connections: List<Connection>, val invitations: List<PendingInvitation>) {
    val myPartners get() = connections.filter { it.active && it.partnerOfMe }
    val peopleISupport get() = connections.filter { it.active && !it.partnerOfMe }
    val recentlyEnded get() = connections.filter { !it.active }

    companion object {
        const val MAX_PARTNERS = 3
        const val SHOW_ENDED_MS = 7 * 24 * 60 * 60 * 1000L
    }
}

internal object InvitationCode {
    /** The code as the server compares it: letters and digits only, upper case. */
    fun normalize(input: String) = input.filter { it.isLetterOrDigit() }.uppercase()

    /** Whether [input] can possibly be a code (32 hex characters, any spacing), so an obvious typo costs no try. */
    fun looksValid(input: String) = normalize(input).let { code -> code.length == 32 && code.all { it in '0'..'9' || it in 'A'..'F' } }
}

/** Milliseconds since 1970 from the server's timestamps ("2026-11-01T08:07:08.819869+00:00"); null if unreadable. */
internal fun epochMsOf(iso: String): Long? =
    try {
        OffsetDateTime.parse(iso).toInstant().toEpochMilli()
    } catch (e: Exception) {
        null
    }
