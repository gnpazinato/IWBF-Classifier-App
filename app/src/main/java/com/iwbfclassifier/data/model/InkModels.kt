package com.iwbfclassifier.data.model

import kotlinx.serialization.Serializable

/**
 * Handwritten notes per Player. Points are stored NORMALIZED (0..1 of the canvas
 * width/height) and stroke width in dp, so notes render correctly regardless of
 * screen size or orientation (docs/04 — local, human-inspectable JSON).
 */

@Serializable
enum class InkTool { PEN, HIGHLIGHTER }

@Serializable
data class InkPoint(val x: Float, val y: Float)

@Serializable
data class InkStroke(
    val tool: InkTool = InkTool.PEN,
    val color: Long = 0xFF111111L,
    val widthDp: Float = 2.5f,
    val points: List<InkPoint> = emptyList(),
)

@Serializable
data class NotePage(
    val playerId: String,
    val strokes: List<InkStroke> = emptyList(),
    val updatedAt: String = "",
)
