package app.identity.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortsReelsTest {

    @Test
    fun onlyYouTubeAndInstagramAreWatched() {
        assertEquals(setOf("com.google.android.youtube", "com.instagram.android"), ShortsReels.WATCHED_PACKAGES)
        for (other in listOf("com.whatsapp", "com.zhiliaoapp.musically", "com.android.chrome", "app.identity", "")) {
            assertNull(other, ShortsReels.markerIds(other))
        }
    }

    @Test
    fun markersAreFullViewIdsInsideTheirOwnApp() {
        for (app in ShortsReels.WATCHED_PACKAGES) {
            val ids = ShortsReels.markerIds(app)!!
            assertTrue(ids.isNotEmpty())
            for (id in ids) assertTrue(id, id.startsWith("$app:id/"))
        }
    }

    @Test
    fun theMarkersSeenOnThePhoneArePresent() {
        assertTrue("com.google.android.youtube:id/reel_watch_fragment_root" in ShortsReels.markerIds(ShortsReels.YOUTUBE)!!)
        assertTrue("com.instagram.android:id/clips_viewer_view_pager" in ShortsReels.markerIds(ShortsReels.INSTAGRAM)!!)
    }
}
