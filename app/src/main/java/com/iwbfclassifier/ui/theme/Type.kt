package com.iwbfclassifier.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Type scale from docs/12 (system sans / Roboto). */
object AppTypography {
    val screenTitle = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
    )
    val header = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp,
    )
    val chip = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp,
    )
    val body = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp,
    )

    /** Uppercase section labels, letter spacing ~0.08em (docs/12). */
    val microLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.08.em,
    )
}

val Material3Typography = Typography(
    titleLarge = AppTypography.screenTitle,
    titleMedium = AppTypography.header,
    bodyLarge = AppTypography.body,
    bodyMedium = AppTypography.body,
    labelLarge = AppTypography.chip,
    labelSmall = AppTypography.microLabel,
)
