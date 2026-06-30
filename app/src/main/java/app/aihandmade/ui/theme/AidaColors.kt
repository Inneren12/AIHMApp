package app.aihandmade.ui.theme

import androidx.compose.ui.graphics.Color

/** aida design tokens (from the Claude Design handoff). Used directly by the editor UI. */
object AidaColors {
    val linen = Color(0xFFF3EEE2)        // screen background
    val surface = Color(0xFFFBF8F0)      // raised card / panel
    val surfaceInk = Color(0xFF272019)   // dark card (export, swap-current, status chips)
    val bezel = Color(0xFF15130F)

    val border = Color(0xFFE6DECC)       // card border
    val borderStrong = Color(0xFFDCD3C1) // input / button border
    val divider = Color(0xFFE0D8C7)      // top divider

    val accent = Color(0xFF2E6A5B)       // spruce/teal primary
    val accentTint = Color(0xFFDCE8E2)   // teal tint bg
    val accentTintBorder = Color(0xFFBFD8CF)
    val accentLight = Color(0xFF7FD0B5)  // skeins highlight

    val textHeading = Color(0xFF1F1D18)
    val textStrong = Color(0xFF2B2823)
    val textBody = Color(0xFF5C5648)
    val textSecondary = Color(0xFF7A7263)
    val textMuted = Color(0xFF8E8676)
    val textFaint = Color(0xFF9C9482)

    // chart-specific
    val chartCanvasBg = surface                       // #FBF8F0 around the grid
    val chartCellBg = Color(0xFFFFFFFF)               // grid area before colours
    val gridThin = Color(0xFF2B2823).copy(alpha = 0.13f)  // rgba(43,40,35,0.13)
    val gridBold = Color(0xFF2B2823).copy(alpha = 0.60f)  // rgba(43,40,35,0.6)
    val symbolDark = Color(0xFF2B2823)
    val symbolLight = Color(0xFFFFFFFF).copy(alpha = 0.95f)
    val railTrack = Color(0xFFE5DDCB)                 // view-toggle / inactive rail track
    val symbolChipBg = Color(0xFF2B2823)
}
