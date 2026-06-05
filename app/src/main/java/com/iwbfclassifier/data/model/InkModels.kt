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
    // Aspect ratio (width / height) of the canvas the strokes were drawn on. Because the
    // observation note canvas resizes with the video panel, storing the ratio lets the
    // notes be re-rendered faithfully (no stretching) anywhere, e.g. the Edit Player
    // screen (user request). Null for notes saved before this was tracked.
    val aspectRatio: Float? = null,
)
