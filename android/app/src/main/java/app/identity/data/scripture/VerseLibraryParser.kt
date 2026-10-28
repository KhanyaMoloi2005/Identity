package app.identity.data.scripture

import kotlinx.serialization.json.Json

/** Raised when the bundled verse file is malformed or breaks a rule. */
class VerseDataException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Turns the verse JSON into a checked list of [Verse]s.
 * Bad data returns a failed [Result] instead of crashing the app.
 */
object VerseLibraryParser {
    private val json = Json { ignoreUnknownKeys = false }

    fun parse(text: String, knownTopics: List<Topic> = StarterTopics): Result<List<Verse>> =
        try {
            val verses = json.decodeFromString<VerseFile>(text).verses
            validate(verses, knownTopics.map { it.id }.toSet())
            Result.success(verses)
        } catch (e: VerseDataException) {
            Result.failure(e)
        } catch (e: Exception) {
            // Covers broken JSON and missing required fields.
            Result.failure(VerseDataException("Verse data could not be read", e))
        }

    private fun validate(verses: List<Verse>, topicIds: Set<String>) {
        val seen = mutableSetOf<String>()
        for (v in verses) {
            if (v.id.isBlank() || v.reference.isBlank() || v.text.isBlank() || v.translation.isBlank()) {
                throw VerseDataException("Verse '${v.id}' has a blank required field")
            }
            if (!seen.add(v.id)) throw VerseDataException("Duplicate verse id '${v.id}'")
            if (v.topicTags.isEmpty()) throw VerseDataException("Verse '${v.id}' has no topic")
            val unknown = v.topicTags.filterNot { it in topicIds }
            if (unknown.isNotEmpty()) throw VerseDataException("Verse '${v.id}' has unknown topic $unknown")
        }
    }
}
