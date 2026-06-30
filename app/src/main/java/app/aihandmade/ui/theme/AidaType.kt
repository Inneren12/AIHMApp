package app.aihandmade.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.aihandmade.ui.theme.AidaColors as C

/**
 * aida type scale. The three target fonts are Bricolage Grotesque (display), Hanken Grotesk (body),
 * JetBrains Mono (mono). To keep this PR free of new deps / font binaries, the families below fall
 * back to system fonts (mono via FontFamily.Monospace). TO SWAP IN THE REAL FONTS later, either:
 *   (a) add `androidx.compose.ui:ui-text-google-fonts`, declare a GoogleFont.Provider, and set
 *       displayFamily/bodyFamily/monoFamily to FontFamily(Font(googleFont, provider)); or
 *   (b) drop the .ttf files into res/font and use FontFamily(Font(R.font.xxx)).
 * Only these three vals change; every TextStyle below already references them.
 */
object AidaType {
    val displayFamily: FontFamily = FontFamily.Default   // -> Bricolage Grotesque
    val bodyFamily: FontFamily = FontFamily.Default      // -> Hanken Grotesk
    val monoFamily: FontFamily = FontFamily.Monospace    // -> JetBrains Mono

    val wordmark = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, letterSpacing = (-0.02).em, color = C.textHeading)
    val sectionTitle = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.Bold, fontSize = 21.sp, letterSpacing = (-0.01).em, color = C.textHeading)
    val kicker = TextStyle(fontFamily = monoFamily, fontSize = 9.sp, letterSpacing = 0.14.em, color = C.textMuted)
    val monoCaption = TextStyle(fontFamily = monoFamily, fontSize = 11.sp, color = C.textMuted)
    val groupLabel = TextStyle(fontFamily = monoFamily, fontSize = 10.sp, letterSpacing = 0.1.em, color = C.textMuted)
    val body = TextStyle(fontFamily = bodyFamily, fontSize = 14.sp, color = C.textBody)
    val dmcCode = TextStyle(fontFamily = monoFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = C.textHeading)
    val dmcName = TextStyle(fontFamily = bodyFamily, fontSize = 12.5.sp, color = C.textSecondary)
    val count = TextStyle(fontFamily = monoFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = C.textStrong)
    val countCaption = TextStyle(fontFamily = monoFamily, fontSize = 9.sp, color = C.textFaint)
    val buttonPrimary = TextStyle(fontFamily = bodyFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
    val chartCaption = TextStyle(fontFamily = monoFamily, fontSize = 10.sp, color = C.textFaint, textAlign = TextAlign.Center)
}
