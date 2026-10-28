package app.identity.data.scripture

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptureDataTest {
    // Gradle runs unit tests with the module folder (android/app) as the working directory.
    private val bundled = File("src/main/assets/scripture/verses.json").readText()

    private fun verse(
        id: String = "v1",
        topics: List<String> = listOf("hope"),
        active: Boolean = true,
    ) = Verse(id, "Ref 1:1", "Some text.", "WEB", topics, null, active)

    @Test
    fun bundledFileParses() {
        val result = VerseLibraryParser.parse(bundled)
        assertTrue(result.exceptionOrNull()?.message, result.isSuccess)
    }

    @Test
    fun everyTopicHasAtLeastThreeVerses() {
        val repo = InMemoryScriptureRepository(VerseLibraryParser.parse(bundled).getOrThrow())
        for (topic in repo.topics()) {
            assertTrue("${topic.id} has too few verses", repo.versesFor(topic.id).size >= 3)
        }
    }

    @Test
    fun bundledVersesAllUseTheWebTranslation() {
        val verses = VerseLibraryParser.parse(bundled).getOrThrow()
        assertTrue(verses.all { it.translation == "WEB" })
    }

    @Test
    fun unknownTopicIsRejected() {
        val result = VerseLibraryParser.parse(
            """{"translation":"WEB","verses":[{"id":"a","reference":"R 1:1","text":"t","translation":"WEB","topic_tags":["nope"]}]}""",
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun missingRequiredFieldIsRejected() {
        val result = VerseLibraryParser.parse(
            """{"translation":"WEB","verses":[{"id":"a","reference":"R 1:1","translation":"WEB","topic_tags":["hope"]}]}""",
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun brokenJsonIsRejectedWithoutCrashing() {
        assertTrue(VerseLibraryParser.parse("{ not json").isFailure)
        assertTrue(VerseLibraryParser.parse("").isFailure)
    }

    @Test
    fun blankTextAndDuplicateIdsAreRejected() {
        val blank = """{"translation":"WEB","verses":[{"id":"a","reference":"R 1:1","text":" ","translation":"WEB","topic_tags":["hope"]}]}"""
        assertTrue(VerseLibraryParser.parse(blank).isFailure)
        val dup = """{"translation":"WEB","verses":[
            {"id":"a","reference":"R 1:1","text":"t","translation":"WEB","topic_tags":["hope"]},
            {"id":"a","reference":"R 1:2","text":"t","translation":"WEB","topic_tags":["hope"]}]}"""
        assertTrue(VerseLibraryParser.parse(dup).isFailure)
    }

    @Test
    fun versesAreFilteredByTopic() {
        val repo = InMemoryScriptureRepository(
            listOf(verse("a", listOf("hope")), verse("b", listOf("grace")), verse("c", listOf("hope", "grace"))),
        )
        assertEquals(listOf("a", "c"), repo.versesFor("hope").map { it.id })
        assertEquals(listOf("b", "c"), repo.versesFor("grace").map { it.id })
        assertTrue(repo.versesFor("purity").isEmpty())
    }

    @Test
    fun inactiveVersesAreHidden() {
        val repo = InMemoryScriptureRepository(listOf(verse("on"), verse("off", active = false)))
        assertEquals(listOf("on"), repo.versesFor("hope").map { it.id })
        assertNotNull(repo.verse("on"))
        assertNull(repo.verse("off"))
    }

    @Test
    fun unknownVerseIdReturnsNull() {
        assertNull(InMemoryScriptureRepository(listOf(verse())).verse("missing"))
    }
}
