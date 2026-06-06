package com.iwbfclassifier.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.iwbfclassifier.data.model.InkStroke

/**
 * Snapshot-based undo/redo for handwritten notes.
 *
 * Every change records a full snapshot of the page, so Undo restores the previous state —
 * including after **Clear**, which used to wipe the notes irreversibly (user request: it
 * must be possible to undo Clear and get the writing back). Redo reverses an Undo.
 *
 * One eraser gesture = one Undo step: [erase] takes a snapshot only on the first change of
 * the gesture (`firstOfGesture = true`) and updates live afterwards, so rubbing out a word
 * comes back in a single tap instead of point-by-point.
 *
 * Owned by a screen via `remember`; [reset] seeds saved notes as the clean baseline without
 * creating undo history or marking the page dirty. Undo history intentionally survives an
 * autosave ([markSaved] only clears the dirty flag) so a stroke can still be undone after it
 * has been written to disk.
 */
class InkEditor {
    var strokes by mutableStateOf<List<InkStroke>>(emptyList())
        private set

    /** True when the page differs from the last persisted state — drives debounced autosave. */
    var dirty by mutableStateOf(false)
        private set

    private var past by mutableStateOf<List<List<InkStroke>>>(emptyList())
    private var future by mutableStateOf<List<List<InkStroke>>>(emptyList())

    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()
    val canClear: Boolean get() = strokes.isNotEmpty()

    /** Load saved strokes as the clean baseline: clears history and the dirty flag. */
    fun reset(initial: List<InkStroke>) {
        strokes = initial
        past = emptyList()
        future = emptyList()
        dirty = false
    }

    /** A finished pen/highlighter stroke — one Undo step. */
    fun addStroke(stroke: InkStroke) = commit(strokes + stroke)

    /**
     * Live eraser update. [firstOfGesture] is true for the first change of an erase gesture,
     * which is when the pre-gesture snapshot is recorded so the whole gesture undoes at once.
     */
    fun erase(next: List<InkStroke>, firstOfGesture: Boolean) {
        if (firstOfGesture) {
            past = (past + listOf(strokes)).takeLast(MAX_HISTORY)
            future = emptyList()
        }
        strokes = next
        dirty = true
    }

    /** Wipe the page — undoable, unlike before (user request). */
    fun clear() {
        if (strokes.isNotEmpty()) commit(emptyList())
    }

    fun undo() {
        if (past.isEmpty()) return
        future = future + listOf(strokes)
        strokes = past.last()
        past = past.dropLast(1)
        dirty = true
    }

    fun redo() {
        if (future.isEmpty()) return
        past = past + listOf(strokes)
        strokes = future.last()
        future = future.dropLast(1)
        dirty = true
    }

    /** Mark the current page as persisted; keeps undo history intact. */
    fun markSaved() {
        dirty = false
    }

    private fun commit(next: List<InkStroke>) {
        past = (past + listOf(strokes)).takeLast(MAX_HISTORY)
        future = emptyList()
        strokes = next
        dirty = true
    }

    companion object {
        // Plenty for a handwritten note; bounds memory if someone scribbles for a long time.
        private const val MAX_HISTORY = 100
    }
}
