package app.identity.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.identity.R

// Fonts are bundled in res/font (SIL Open Font Licence; licence texts in assets/licenses).
// Each file is a variable font: one file contains every weight, selected here by its "wght" value.

@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Interface font. */
val InterFamily = FontFamily(
    variableFont(R.font.inter, FontWeight.Normal),
    variableFont(R.font.inter, FontWeight.Medium),
    variableFont(R.font.inter, FontWeight.SemiBold),
)

/** Scripture reading font, designed for long-form screen reading. */
val LiterataFamily = FontFamily(
    variableFont(R.font.literata, FontWeight.Normal),
    variableFont(R.font.literata, FontWeight.SemiBold),
)

/** Body style for Scripture passages (DESIGN.md §4: 19 sp; user-adjustable size comes later). */
val ScriptureBody = TextStyle(
    fontFamily = LiterataFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 19.sp,
    lineHeight = 30.sp,
)

// Material's default sizes already match DESIGN.md §4 (headlineLarge 32, titleLarge 22,
// bodyLarge 16, labelLarge 14, bodySmall 12); we only swap in Inter everywhere.
private val defaults = Typography()

val IdentityTypography = Typography(
    displayLarge = defaults.displayLarge.copy(fontFamily = InterFamily),
    displayMedium = defaults.displayMedium.copy(fontFamily = InterFamily),
    displaySmall = defaults.displaySmall.copy(fontFamily = InterFamily),
    headlineLarge = defaults.headlineLarge.copy(fontFamily = InterFamily),
    headlineMedium = defaults.headlineMedium.copy(fontFamily = InterFamily),
    headlineSmall = defaults.headlineSmall.copy(fontFamily = InterFamily),
    titleLarge = defaults.titleLarge.copy(fontFamily = InterFamily),
    titleMedium = defaults.titleMedium.copy(fontFamily = InterFamily),
    titleSmall = defaults.titleSmall.copy(fontFamily = InterFamily),
    bodyLarge = defaults.bodyLarge.copy(fontFamily = InterFamily),
    bodyMedium = defaults.bodyMedium.copy(fontFamily = InterFamily),
    bodySmall = defaults.bodySmall.copy(fontFamily = InterFamily),
    labelLarge = defaults.labelLarge.copy(fontFamily = InterFamily),
    labelMedium = defaults.labelMedium.copy(fontFamily = InterFamily),
    labelSmall = defaults.labelSmall.copy(fontFamily = InterFamily),
)
