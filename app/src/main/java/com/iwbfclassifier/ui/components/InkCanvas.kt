package com.iwbfclassifier.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.iwbfclassifier.data.model.InkPoint
import com.iwbfclassifier.data.model.InkStroke
import com.iwbfclassifier.data.model.InkTool
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppShapes
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography

enum class CanvasTool { PEN, HIGHLIGHTER, ERASER }

private const val PEN_COLOR = 0xFF111111L
private const val HIGHLIGHTER_COLOR = 0x55A3975DL
private const val PEN_WIDTH_DP = 2.5f
private const val HIGHLIGHTER_WIDTH_DP = 14f

private fun Long.toComposeColor(): Color {
    val a = ((this shr 24) and 0xFF).toInt()
    val r = ((this shr 16) and 0xFF).toInt()
    val g = ((this shr 8) and 0xFF).toInt()
    val b = (this and 0xFF).toInt()
    return Color(red = r, green = g, blue = b, alpha = a)
}

/**
 * Low-latency freehand ink surface. Captures the primary pointer (S Pen or finger)
 * and draws smoothed strokes. Points are normalized to the canvas so saved notes
 * survive rotation. Extra simultaneous pointers are ignored (basic palm rejection).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InkCanvas(
    strokes: List<InkStroke>,
    tool: CanvasTool,
    onAddStroke: (InkStroke) -> Unit,
    onEraseStrokes: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strokesState = rememberUpdatedState(strokes)
    val toolState = rememberUpdatedState(tool)
    val onAddState = rememberUpdatedState(onAddStroke)
    val onEraseState = rememberUpdatedState(onEraseStrokes)

    // Live in-progress stroke, in canvas px. Read inside the Canvas draw to redraw.
    var live by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                val scope = this
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = scope.size.width.toFloat()
                    val h = scope.size.height.toFloat()
                    if (w <= 0f || h <= 0f) return@awaitEachGesture

                    val pts = mutableListOf(down.position)
                    live = pts.toList()
                    down.consume()

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        change.historical.forEach { pts.add(it.position) }
                        pts.add(change.position)
                        change.consume()
                        live = pts.toList()
                        if (!change.pressed) break
                    }

                    when (toolState.value) {
                        CanvasTool.ERASER -> {
                            val radiusPx = 16.dp.toPx()
                            val hit = strokesState.value.indicesIntersecting(pts, w, h, radiusPx)
                            if (hit.isNotEmpty()) onEraseState.value(hit)
                        }
                        CanvasTool.PEN, CanvasTool.HIGHLIGHTER -> {
                            val isHi = toolState.value == CanvasTool.HIGHLIGHTER
                            val norm = pts.map {
                                InkPoint((it.x / w).coerceIn(0f, 1f), (it.y / h).coerceIn(0f, 1f))
                            }
                            onAddState.value(
                                InkStroke(
                                    tool = if (isHi) InkTool.HIGHLIGHTER else InkTool.PEN,
                                    color = if (isHi) HIGHLIGHTER_COLOR else PEN_COLOR,
                                    widthDp = if (isHi) HIGHLIGHTER_WIDTH_DP else PEN_WIDTH_DP,
                                    points = norm,
                                ),
                            )
                        }
                    }
                    live = emptyList()
                }
            },
    ) {
        strokes.forEach { drawInkStroke(it, size.width, size.height) }
        if (live.isNotEmpty()) {
            val isHi = tool == CanvasTool.HIGHLIGHTER
            if (tool != CanvasTool.ERASER) {
                drawSmoothPath(
                    pts = live,
                    color = (if (isHi) HIGHLIGHTER_COLOR else PEN_COLOR).toComposeColor(),
                    widthPx = (if (isHi) HIGHLIGHTER_WIDTH_DP else PEN_WIDTH_DP).dp.toPx(),
                )
            }
        }
    }
}

private fun DrawScope.drawInkStroke(stroke: InkStroke, w: Float, h: Float) {
    if (w <= 0f || h <= 0f) return
    val pts = stroke.points.map { Offset(it.x * w, it.y * h) }
    drawSmoothPath(pts, stroke.color.toComposeColor(), stroke.widthDp.dp.toPx())
}

private fun DrawScope.drawSmoothPath(pts: List<Offset>, color: Color, widthPx: Float) {
    if (pts.isEmpty()) return
    if (pts.size == 1) {
        drawCircle(color = color, radius = widthPx / 2f, center = pts[0])
        return
    }
    val path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size - 1) {
            val midX = (pts[i].x + pts[i + 1].x) / 2f
            val midY = (pts[i].y + pts[i + 1].y) / 2f
            quadraticBezierTo(pts[i].x, pts[i].y, midX, midY)
        }
        lineTo(pts.last().x, pts.last().y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun List<InkStroke>.indicesIntersecting(
    eraser: List<Offset>,
    w: Float,
    h: Float,
    radiusPx: Float,
): Set<Int> {
    val result = mutableSetOf<Int>()
    forEachIndexed { idx, stroke ->
        val pts = stroke.points.map { Offset(it.x * w, it.y * h) }
        loop@ for (e in eraser) {
            for (p in pts) {
                if ((p - e).getDistance() <= radiusPx) {
                    result.add(idx)
                    break@loop
                }
            }
        }
    }
    return result
}

/**
 * Paper note panel: icon-light toolbar + light paper surface with the ink canvas.
 * Strokes are owned by the caller so undo/redo and autosave reset per Player.
 */
@Composable
fun NoteCanvasPanel(
    strokes: List<InkStroke>,
    onAddStroke: (InkStroke) -> Unit,
    onEraseStrokes: (Set<Int>) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    modifier: Modifier = Modifier,
) {
    var tool by remember { mutableStateOf(CanvasTool.PEN) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        NoteToolbar(
            tool = tool,
            onToolChange = { tool = it },
            onUndo = onUndo,
            onRedo = onRedo,
            onClear = onClear,
            canUndo = canUndo,
            canRedo = canRedo,
        )
        PaperNoteCanvasContainer(modifier = Modifier.fillMaxWidth().weight(1f)) {
            InkCanvas(
                strokes = strokes,
                tool = tool,
                onAddStroke = onAddStroke,
                onEraseStrokes = onEraseStrokes,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun NoteToolbar(
    tool: CanvasTool,
    onToolChange: (CanvasTool) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        ToolButton("Pen", tool == CanvasTool.PEN) { onToolChange(CanvasTool.PEN) }
        ToolButton("Highlight", tool == CanvasTool.HIGHLIGHTER) { onToolChange(CanvasTool.HIGHLIGHTER) }
        ToolButton("Eraser", tool == CanvasTool.ERASER) { onToolChange(CanvasTool.ERASER) }
        Spacer(Modifier.weight(1f))
        ToolButton("Undo", false, enabled = canUndo) { onUndo() }
        ToolButton("Redo", false, enabled = canRedo) { onRedo() }
        ToolButton("Clear", false, enabled = canUndo) { onClear() }
    }
}

@Composable
private fun ToolButton(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(AppShapes.button)
            .background(if (selected) AppColors.Gold else AppColors.CardCharcoal)
            .border(1.dp, if (selected) AppColors.Gold else AppColors.DividerGray, AppShapes.button)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = AppSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = AppTypography.chip,
            color = when {
                selected -> AppColors.InkBlack
                !enabled -> AppColors.TextMuted
                else -> AppColors.TextPrimary
            },
        )
    }
}
