package com.iwbfclassifier.ui.theme

import androidx.compose.ui.graphics.Color

/** Exact palette from docs/12_visual_design_direction.md. */
object AppColors {
    val InkBlack = Color(0xFF0E0E0E)
    val PanelBlack = Color(0xFF181818)
    val CardCharcoal = Color(0xFF222222)
    val DividerGray = Color(0xFF3D3D3D)
    val TextPrimary = Color(0xFFFFFFFF)
    // Contrast raised vs. docs/12 per field feedback: gray text on black was too faint
    // to read on the tablet. These still keep a hierarchy but stay clearly legible.
    val TextSecondary = Color(0xFFD7D7D7)
    val TextMuted = Color(0xFFAAAAAA)
    val Gold = Color(0xFFA3975D)
    val GoldSoft = Color(0x26A3975D)
    val GoldBorder = Color(0x80A3975D)
    val PaperSurface = Color(0xFFFAFAFA)
    val InkStroke = Color(0xFF111111)
    val AlertRed = Color(0xFFEA384C)
    val InfoBlue = Color(0xFF1C276D)
}
