package com.iwbfclassifier.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppShapes

/**
 * Light, paper-like surface for handwriting (docs/12). Phase 3 mounts the S Pen
 * ink canvas inside [content]; for now it renders the faint dot grid + paper feel.
 */
@Composable
fun PaperNoteCanvasContainer(
    modifier: Modifier = Modifier,
    showDotGrid: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .shadow(2.dp, AppShapes.canvas)
            .clip(AppShapes.canvas)
            .background(AppColors.PaperSurface),
    ) {
        if (showDotGrid) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val step = 24.dp.toPx()
                val radius = 1.2.dp.toPx()
                var y = step
                while (y < size.height) {
                    var x = step
                    while (x < size.width) {
                        drawCircle(
                            color = AppColors.DividerGray.copy(alpha = 0.25f),
                            radius = radius,
                            center = Offset(x, y),
                        )
                        x += step
                    }
                    y += step
                }
            }
        }
        content()
    }
}
