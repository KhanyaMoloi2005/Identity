package app.identity.data.scripture

import android.content.Context

/** Read-only access to the bundled verse library. Inactive verses are never returned. */
interface ScriptureRepository {
    fun topics(): List<Topic>
    fun versesFor(topicId: String): List<Verse>
    fun verse(id: String): Verse?
}

/** Holds the verses in memory. Room arrives in M1.5 for saved verses and reflections (D-019). */
class InMemoryScriptureRepository(
    allVerses: List<Verse>,
    private val topics: List<Topic> = StarterTopics,
) : ScriptureRepository {
    private val active = allVerses.filter { it.isActive }

    override fun topics(): List<Topic> = topics

    override fun versesFor(topicId: String): List<Verse> = active.filter { topicId in it.topicTags }

    override fun verse(id: String): Verse? = active.firstOrNull { it.id == id }
}

private const val VERSES_ASSET = "scripture/verses.json"

/** Loads the bundled library from assets. Throws [VerseDataException] if the file is bad. */
fun loadBundledScripture(context: Context): ScriptureRepository {
    val text = context.assets.open(VERSES_ASSET).bufferedReader().use { it.readText() }
    return InMemoryScriptureRepository(VerseLibraryParser.parse(text).getOrThrow())
}
