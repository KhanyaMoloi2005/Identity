package app.identity.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomRulesTest {

    @Test
    fun sitesAreCleanedFromWhatPeopleTypeOrPaste() {
        assertEquals("example.com", CustomRules.normalizeSite("example.com"))
        assertEquals("example.com", CustomRules.normalizeSite("  Example.COM  "))
        assertEquals("example.com", CustomRules.normalizeSite("www.example.com"))
        assertEquals("example.com", CustomRules.normalizeSite("https://www.example.com/some/page?x=1#top"))
        assertEquals("example.com", CustomRules.normalizeSite("http://example.com:8080"))
        assertEquals("m.example.co.za", CustomRules.normalizeSite("m.example.co.za/"))
    }

    @Test
    fun thingsThatAreNotSitesAreRejected() {
        for (input in listOf("", "   ", "example", "exa mple.com", "ünicode.com", "..com", "https://")) {
            assertNull(input, CustomRules.normalizeSite(input))
        }
    }

    @Test
    fun wordsNeedFourOrMoreLettersAndNoSpaces() {
        assertEquals("casino", CustomRules.normalizeWord(" Casino "))
        assertEquals("only-fans", CustomRules.normalizeWord("only-fans"))
        assertEquals("1xbet", CustomRules.normalizeWord("1xbet"))
        for (input in listOf("", "sex", "two words", "dots.com", "ünicode")) assertNull(input, CustomRules.normalizeWord(input))
    }

    @Test
    fun wordRulesMatchNamesContainingTheWord() {
        val words = CustomRules.containingAny(listOf("casino"))
        val matcher = DomainMatcher(words)
        assertTrue(matcher.isBlocked("bestcasino.net"))
        assertTrue(matcher.isBlocked("casino.example"))
        assertTrue(matcher.isBlocked("play.casinoworld.co.za"))
        assertFalse(matcher.isBlocked("wikipedia.org"))
        assertFalse("casino" in CustomRules.containingAny(emptyList()))
    }

    @Test
    fun connectivityChecksSurviveABroadWord() {
        val matcher = DomainMatcher(CustomRules.containingAny(listOf("gstatic")), DomainSet.of(CustomRules.NEVER_BLOCKED))
        assertFalse(matcher.isBlocked("connectivitycheck.gstatic.com"))
        assertTrue(matcher.isBlocked("fonts.gstatic.com"))
    }

    @Test
    fun undoWorksForTenMinutesInTheSameBootOnly() {
        val added = 1_000_000L
        assertTrue(CustomRules.canRemove(5, added, 5, added))
        assertTrue(CustomRules.canRemove(5, added, 5, added + CustomRules.UNDO_WINDOW_MS - 1))
        assertFalse(CustomRules.canRemove(5, added, 5, added + CustomRules.UNDO_WINDOW_MS))
        assertFalse("after a restart", CustomRules.canRemove(5, added, 6, added + 1))
        assertFalse("time can't run backwards", CustomRules.canRemove(5, added, 5, added - 1))
    }
}
