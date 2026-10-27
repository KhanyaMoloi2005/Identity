package app.identity.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// Proportions of the I-cross mark, relative to its height. Matches docs/design/brand/*.svg.
private const val MARK_WIDTH = 0.55f
private const val STEM_WIDTH = 0.17f
private const val BAR_TOP = 0.27f
private const val BAR_HEIGHT = 0.11f

/** The I-cross mark: a stem in [stemColor] with a crossbar in [barColor]. */
@Composable
fun IdentityMark(
    height: Dp,
    stemColor: Color,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(width = height * MARK_WIDTH, height = height)) {
        val h = size.height
        val w = size.width
        val stemW = h * STEM_WIDTH
        val corner = CornerRadius(stemW * 0.12f)
        drawRoundRect(
            color = stemColor,
            topLeft = Offset((w - stemW) / 2f, 0f),
            size = Size(stemW, h),
            cornerRadius = corner,
        )
        drawRoundRect(
            color = barColor,
            topLeft = Offset(0f, h * BAR_TOP),
            size = Size(w, h * BAR_HEIGHT),
            cornerRadius = corner,
        )
    }
}

/**
 * "Identity" wordmark: the mark stands in for the capital I, followed by "dentity".
 * Screen readers hear a single word, "Identity".
 */
@Composable
fun IdentityWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 44.sp,
) {
    val colors = MaterialTheme.colorScheme
    // The mark is sized to the cap height of the text (about 71% of the font size) so it lines up as a letter.
    val markHeight = with(LocalDensity.current) { (fontSize * 0.71f).toDp() }
    Row(modifier.clearAndSetSemantics { contentDescription = "Identity" }) {
        IdentityMark(
            height = markHeight,
            stemColor = colors.onBackground,
            barColor = colors.primary,
            modifier = Modifier.alignBy { it.measuredHeight },
        )
        Text(
            text = "dentity",
            fontSize = fontSize,
            color = colors.onBackground,
            modifier = Modifier.alignByBaseline(),
        )
    }
}
