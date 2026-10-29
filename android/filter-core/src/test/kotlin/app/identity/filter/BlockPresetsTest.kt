package app.identity.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockPresetsTest {
    private val social = DomainMatcher(BlockPresets.X_AND_REDDIT)
    private val images = DomainMatcher(BlockPresets.IMAGE_SEARCH)

    @Test
    fun xAndRedditAreBlockedWithTheirAppAndImageServers() {
        for (name in listOf(
            "x.com", "api.x.com", "twitter.com", "mobile.twitter.com", "pbs.twimg.com", "video.twimg.com", "t.co",
            "reddit.com", "www.reddit.com", "oauth.reddit.com", "i.redd.it", "v.redd.it", "preview.redd.it",
            "styles.redditmedia.com", "www.redditstatic.com",
        )) assertTrue(name, social.isBlocked(name))
    }

    @Test
    fun lookAlikesAreNotCaught() {
        for (name in listOf("max.com", "box.com", "netflix.com", "t.com", "redditalternative.example")) {
            assertFalse(name, social.isBlocked(name))
        }
    }

    @Test
    fun imageSearchPicturesAreBlocked() {
        for (name in listOf(
            "encrypted-tbn0.gstatic.com", "encrypted-tbn3.gstatic.com",
            "tse1.mm.bing.net", "tse4.mm.bing.net", "th.bing.com", "external-content.duckduckgo.com",
        )) assertTrue(name, images.isBlocked(name))
    }

    @Test
    fun searchMailAndMapsKeepWorking() {
        for (name in listOf(
            "www.google.com", "forcesafesearch.google.com", "www.gstatic.com", "fonts.gstatic.com",
            "mail.google.com", "maps.google.com", "maps.googleapis.com", "lh3.googleusercontent.com",
            "www.bing.com", "strict.bing.com", "duckduckgo.com", "safe.duckduckgo.com",
        )) assertFalse(name, images.isBlocked(name))
    }

    @Test
    fun everyRuleIsAValidDomain() {
        for (rule in BlockPresets.X_AND_REDDIT + BlockPresets.IMAGE_SEARCH + BlockPresets.TIKTOK) assertEquals(rule, normalizeDomain(rule))
    }

    @Test
    fun tiktokIsBlockedWithItsVideoServersButNotLookAlikes() {
        val tiktok = DomainMatcher(BlockPresets.TIKTOK)
        for (name in listOf("tiktok.com", "www.tiktok.com", "v16m.tiktokcdn.com", "api16-normal.tiktokv.com", "musical.ly")) {
            assertTrue(name, tiktok.isBlocked(name))
        }
        for (name in listOf("nottiktok.com", "google.com", "youtube.com", "instagram.com")) {
            assertFalse(name, tiktok.isBlocked(name))
        }
    }
}
