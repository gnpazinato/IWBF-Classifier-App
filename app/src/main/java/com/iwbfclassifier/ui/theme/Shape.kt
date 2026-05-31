package com.iwbfclassifier.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Restrained rounding (docs/12): buttons/chips/canvas 6dp, cards/panels 8dp. */
object AppShapes {
    val button = RoundedCornerShape(6.dp)
    val chip = RoundedCornerShape(6.dp)
    val canvas = RoundedCornerShape(6.dp)
    val card = RoundedCornerShape(8.dp)
    val panel = RoundedCornerShape(8.dp)
}

val Material3Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
)
