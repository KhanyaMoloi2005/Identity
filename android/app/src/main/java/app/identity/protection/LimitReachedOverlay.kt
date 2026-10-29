package app.identity.protection

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import app.identity.R
import app.identity.data.scripture.loadBundledScripture

/**
 * The full-screen "limit reached" page drawn on top of TikTok (M3.3c-1, D-030). It is a window
 * drawn by the protection service, needing the "Display over other apps" permission, so it can
 * appear even when Identity itself isn't open. Plain views: no screen of its own to start.
 */
internal class LimitReachedOverlay(private val context: Context) {
    private val windows = context.getSystemService(WindowManager::class.java)
    private var view: View? = null

    fun canShow(): Boolean = Settings.canDrawOverlays(context)

    fun show() {
        if (view != null || !canShow()) return
        val page = build()
        val params = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE,
        )
        try {
            windows.addView(page, params)
            view = page
            page.requestFocus()
        } catch (e: Exception) {
            // Permission taken away a moment ago: the DNS cut-off still applies.
        }
    }

    fun hide() {
        val shown = view ?: return
        view = null
        try {
            windows.removeView(shown)
        } catch (e: Exception) {
            // Already gone.
        }
    }

    private fun build(): View {
        val ink = ContextCompat.getColor(context, R.color.ink)
        val parchment = ContextCompat.getColor(context, R.color.parchment)
        val dawn = ContextCompat.getColor(context, R.color.dawn)
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        fun text(content: String, sizeSp: Float, color: Int, bold: Boolean = false) = TextView(context).apply {
            text = content
            textSize = sizeSp
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(16))
        }

        val (reference, verse) = pickVerse()
        val leave = Button(context).apply {
            text = context.getString(R.string.limit_reached_leave)
            setTextColor(ink)
            setBackgroundColor(dawn)
            setOnClickListener { leaveTikTok() }
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(ink)
            setPadding(dp(32), dp(32), dp(32), dp(32))
            isFocusableInTouchMode = true
            // Back leaves TikTok instead of dropping back into it.
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    leaveTikTok()
                    true
                } else {
                    false
                }
            }
            addView(text(context.getString(R.string.limit_reached_title), 26f, parchment, bold = true))
            addView(text(context.getString(R.string.limit_reached_body), 16f, parchment))
            addView(text("“$verse”", 18f, parchment))
            addView(text(reference, 14f, dawn))
            addView(leave)
        }
    }

    /** A self-control verse from the bundled library, or a fixed fallback if the library can't be read. */
    private fun pickVerse(): Pair<String, String> =
        try {
            val verse = loadBundledScripture(context).versesFor("self-control").random()
            verse.reference to verse.text
        } catch (e: Exception) {
            context.getString(R.string.limit_reached_fallback_reference) to context.getString(R.string.limit_reached_fallback_verse)
        }

    private fun leaveTikTok() {
        hide()
        context.startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
