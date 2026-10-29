package app.identity.filter

/**
 * Which apps and which on-screen markers mean "YouTube Shorts" or "Instagram Reels" (M3.3c-2, D-031).
 * The markers are the view ids those apps give the Shorts and Reels screens, found by looking at the
 * screens on the product owner's phone. The apps can rename them in any update, so they are kept in
 * one place and the list is checked on a real phone after app updates.
 */
object ShortsReels {
    const val YOUTUBE = "com.google.android.youtube"
    const val INSTAGRAM = "com.instagram.android"

    /** The only apps the service is allowed to look at. */
    val WATCHED_PACKAGES: Set<String> = setOf(YOUTUBE, INSTAGRAM)

    private val markers: Map<String, List<String>> = mapOf(
        YOUTUBE to listOf("reel_watch_fragment_root", "reel_recycler", "reel_player_page_container"),
        INSTAGRAM to listOf("clips_viewer_view_pager", "root_clips_layout", "clips_viewer_container"),
    )

    /** The full view ids that mean Shorts/Reels in [packageName], or null for any app we must not look at. */
    fun markerIds(packageName: String): List<String>? = markers[packageName]?.map { "$packageName:id/$it" }
}
