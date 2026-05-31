package com.iwbfclassifier.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppShapes

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = AppShapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.Gold,
            contentColor = AppColors.InkBlack,
            disabledContainerColor = AppColors.CardCharcoal,
            disabledContentColor = AppColors.TextMuted,
        ),
        modifier = modifier.heightIn(min = 48.dp),
    ) { Text(text) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = AppShapes.button,
        border = BorderStroke(1.dp, AppColors.GoldBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppColors.PanelBlack,
            contentColor = AppColors.TextPrimary,
        ),
        modifier = modifier.heightIn(min = 48.dp),
    ) { Text(text) }
}

@Composable
fun DestructiveButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = AppShapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.AlertRed,
            contentColor = AppColors.TextPrimary,
        ),
        modifier = modifier.heightIn(min = 48.dp),
    ) { Text(text) }
}

/** Text chevron back affordance (avoids a Material icons dependency). */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Text("‹", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 28.sp)
    }
}
