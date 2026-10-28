package app.identity.data.scripture

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One curated verse. Field names match the data model in spec §5. */
@Serializable
data class Verse(
    val id: String,
    val reference: String,
    val text: String,
    val translation: String,
    @SerialName("topic_tags") val topicTags: List<String>,
    @SerialName("optional_reflection") val optionalReflection: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
)

/** The shape of `assets/scripture/verses.json`. */
@Serializable
internal data class VerseFile(
    val translation: String,
    val verses: List<Verse>,
)

/** A browsable topic. [id] is what verses use in `topic_tags`. */
data class Topic(val id: String, val displayName: String)

/** The eight starter topics from spec §5. */
val StarterTopics: List<Topic> = listOf(
    Topic("lust", "Lust"),
    Topic("temptation", "Temptation"),
    Topic("self-control", "Self-control"),
    Topic("purity", "Purity"),
    Topic("renewing-the-mind", "Renewing the mind"),
    Topic("repentance", "Repentance"),
    Topic("grace", "Grace"),
    Topic("hope", "Hope"),
)
