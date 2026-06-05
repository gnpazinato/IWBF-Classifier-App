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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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
private const val ERASER_RADIUS_DP = 22f

private fun Long.toComposeColor(): Color {
    val a = ((this shr 24) and 0xFF).toInt()
    val r = ((this shr 16) and 0xFF).toInt()
    val g = ((this shr 8) and 0xFF).toInt()
    val b = (this and 0xFF).toInt()
    return Color(red = r, green = g, blue = b, alpha = a)
}

/** True only for the S Pen (tip or button-eraser). Finger/palm touches return false. */
private fun PointerInputChange.isStylus(): Boolean =
    type == PointerType.Stylus || type == PointerType.Eraser

/**
 * Low-latency freehand ink surface.
 *
 * Palm rejection (user request): only the S Pen writes — finger and palm touches are
 * ignored entirely, so a hand resting on the screen never draws or interrupts the pen
 * ("S Pen writes. Finger navigates.", CLAUDE.md).
 *
 * Eraser (user request): a rubber circle that erases by rubbing. As the pen moves with
 * the eraser tool (or the S Pen's button-eraser), ink under the circle is removed live,
 * splitting strokes — not a tap that deletes a whole stroke.
 *
 * Points are normalized to the canvas so saved notes survive rotation.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InkCanvas(
    strokes: List<InkStroke>,
    tool: CanvasTool,
    onAddStroke: (InkStroke) -> Unit,
    onErase: (List<InkStroke>) -> Unit,
    modifier: Modifier = Modifier,
    onCanvasSizeChanged: (Float) -> Unit = {},
) {
    val strokesState = rememberUpdatedState(strokes)
    val toolState = rememberUpdatedState(tool)
    val onAddState = rememberUpdatedState(onAddStroke)
    val onEraseState = rememberUpdatedState(onErase)

    // Live in-progress pen stroke, in canvas px. Read inside the Canvas draw to redraw.
    var live by remember { mutableStateOf<List<Offset>>(emptyList()) }
    // Eraser cursor position while rubbing (null when idle).
    var eraserPos by remember { mutableStateOf<Offset?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            // Report the canvas shape so saved notes can be re-rendered faithfully elsewhere.
            .onSizeChanged { if (it.height > 0) onCanvasSizeChanged(it.width.toFloat() / it.height.toFloat()) }
            .pointerInput(Unit) {
                val scope = this
                val eraserRadiusPx = ERASER_RADIUS_DP.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = scope.size.width.toFloat()
                    val h = scope.size.height.toFloat()
                    if (w <= 0f || h <= 0f) return@awaitEachGesture

                    // Palm rejection: ignore anything that is not the S Pen. We don't
                    // consume it, so finger gestures stay available for navigation.
                    if (!down.isStylus()) return@awaitEachGesture

                    val erasing = toolState.value == CanvasTool.ERASER || down.type == PointerType.Eraser
                    down.consume()

                    if (erasing) {
                        var working = strokesState.value
                        fun rubAt(pos: Offset) {
                            val next = eraseAt(working, pos, eraserRadiusPx, w, h)
                            if (next !== working) { working = next; onEraseState.value(next) }
                        }
                        eraserPos = down.position
                        rubAt(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            change.historical.forEach { rubAt(it.position) }
                            rubAt(change.position)
                            eraserPos = change.position
                            change.consume()
                            if (!change.pressed) break
                        }
                        eraserPos = null
                    } else {
                        val pts = mutableListOf(down.position)
                        live = pts.toList()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            change.historical.forEach { pts.add(it.position) }
                            pts.add(change.position)
                            change.consume()
                            live = pts.toList()
                            if (!change.pressed) break
                        }
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
                        live = emptyList()
                    }
                }
            },
    ) {
        strokes.forEach { drawInkStroke(it, size.width, size.height) }
        if (live.isNotEmpty() && tool != CanvasTool.ERASER) {
            val isHi = tool == CanvasTool.HIGHLIGHTER
            drawSmoothPath(
                pts = live,
                color = (if (isHi) HIGHLIGHTER_COLOR else PEN_COLOR).toComposeColor(),
                widthPx = (if (isHi) HIGHLIGHTER_WIDTH_DP else PEN_WIDTH_DP).dp.toPx(),
            )
        }
        eraserPos?.let { c ->
            val r = ERASER_RADIUS_DP.dp.toPx()
            drawCircle(color = Color(0x33000000), radius = r, center = c)
            drawCircle(color = Color(0xFF888888), radius = r, center = c, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

/**
 * Remove the points of every stroke that fall inside the eraser circle, splitting a
 * stroke into the surviving runs. Returns the SAME list instance when nothing changed
 * so callers can skip needless state updates.
 */
private fun eraseAt(
    strokes: List<InkStroke>,
    center: Offset,
    radiusPx: Float,
    w: Float,
    h: Float,
): List<InkStroke> {
    if (w <= 0f || h <= 0f) return strokes
    val r2 = radiusPx * radiusPx
    var changed = false
    val result = ArrayList<InkStroke>(strokes.size)
    for (stroke in strokes) {
        val pts = stroke.points
        if (pts.isEmpty()) { result.add(stroke); continue }
        var removedAny = false
        val runs = ArrayList<ArrayList<InkPoint>>()
        var cur = ArrayList<InkPoint>()
        for (p in pts) {
            val dx = p.x * w - center.x
            val dy = p.y * h - center.y
            if (dx * dx + dy * dy <= r2) {
                removedAny = true
                if (cur.size >= 2) runs.add(cur)
                cur = ArrayList()
            } else {
                cur.add(p)
            }
        }
        if (cur.size >= 2) runs.add(cur)
        if (!removedAny) {
            result.add(stroke)
        } else {
            changed = true
            for (run in runs) result.add(stroke.copy(points = run))
        }
    }
    return if (changed) result else strokes
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

/**
 * Read-only render of a Player's saved handwritten notes on the same light paper surface
 * used for writing — no input handling. Used to review notes outside the Observation
 * screen (e.g. the Edit Player screen), so every stroke written with the S Pen shows up
 * in the player record (user request).
 */
@Composable
fun NotePreview(
    strokes: List<InkStroke>,
    modifier: Modifier = Modifier,
) {
    PaperNoteCanvasContainer(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            strokes.forEach { drawInkStroke(it, size.width, size.height) }
        }
    }
}

/**
 * Paper note panel: icon-light toolbar + light paper surface with the ink canvas.
 * Strokes are owned by the caller so undo/redo and autosave reset per Player.
 */
@Composable
fun NoteCanvasPanel(
    strokes: List<InkStroke>,
    onAddStroke: (InkStroke) -> Unit,
    onErase: (List<InkStroke>) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    modifier: Modifier = Modifier,
    onCanvasAspectRatio: (Float) -> Unit = {},
    // When set, the canvas is laid out at this width/height ratio instead of filling the
    // available height. Used on scrollable screens (e.g. Edit Player) so the writing area
    // matches the shape the notes were saved at — keeping them faithful while editable.
    noteAspectRatio: Float? = null,
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
        val canvasModifier = if (noteAspectRatio != null && noteAspectRatio > 0f) {
            Modifier.fillMaxWidth().aspectRatio(noteAspectRatio)
        } else {
            Modifier.fillMaxWidth().weight(1f)
        }
        PaperNoteCanvasContainer(modifier = canvasModifier) {
            InkCanvas(
                strokes = strokes,
                tool = tool,
                onAddStroke = onAddStroke,
                onErase = onErase,
                modifier = Modifier.fillMaxSize(),
                onCanvasSizeChanged = onCanvasAspectRatio,
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
