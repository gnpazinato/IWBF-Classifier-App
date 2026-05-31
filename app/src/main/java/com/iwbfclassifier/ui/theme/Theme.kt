package com.iwbfclassifier.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = AppColors.Gold,
    onPrimary = AppColors.InkBlack,
    secondary = AppColors.Gold,
    onSecondary = AppColors.InkBlack,
    background = AppColors.InkBlack,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.PanelBlack,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.CardCharcoal,
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.DividerGray,
    error = AppColors.AlertRed,
    onError = AppColors.TextPrimary,
)

/** Dark institutional shell (docs/12). The paper note canvas opts back into light locally. */
@Composable
fun ClassifierTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Material3Typography,
        shapes = Material3Shapes,
        content = content,
    )
}
