package app.identity.account

/** What can be wrong with what a person typed, checked before anything is sent anywhere. */
internal enum class FieldProblem { EMAIL_INVALID, PASSWORD_TOO_SHORT, PASSWORD_TOO_LONG, NICKNAME_EMPTY, NICKNAME_TOO_LONG }

/** Checks sign-up and sign-in fields. The server checks again; this just gives quick, clear messages. */
internal object AccountValidation {
    const val MIN_PASSWORD = 8

    /** Supabase hashes only the first 72 bytes of a password, so longer ones are refused rather than silently cut. */
    const val MAX_PASSWORD = 72
    const val MAX_NICKNAME = 30
    private val EMAIL = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")

    fun email(input: String): FieldProblem? {
        val text = input.trim()
        return if (text.length in 3..254 && EMAIL.matches(text)) null else FieldProblem.EMAIL_INVALID
    }

    fun password(input: String): FieldProblem? = when {
        input.length < MIN_PASSWORD -> FieldProblem.PASSWORD_TOO_SHORT
        input.toByteArray().size > MAX_PASSWORD -> FieldProblem.PASSWORD_TOO_LONG
        else -> null
    }

    fun nickname(input: String): FieldProblem? {
        val text = cleanNickname(input)
        return when {
            text.isEmpty() -> FieldProblem.NICKNAME_EMPTY
            text.length > MAX_NICKNAME -> FieldProblem.NICKNAME_TOO_LONG
            else -> null
        }
    }

    fun cleanEmail(input: String): String = input.trim().lowercase()

    /** Trims and drops control characters and line breaks, so a nickname is always one tidy line. */
    fun cleanNickname(input: String): String = input.filter { !it.isISOControl() }.trim().replace(Regex("\\s+"), " ")
}
