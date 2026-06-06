package com.iwbfclassifier.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iwbfclassifier.data.model.displayJersey
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppShapes
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography

/**
 * Pen-first uniform number picker (user request): tap the field to open a 00–99 grid and
 * pick the jersey with the S Pen — no on-screen keyboard during a game. Rows are decades
 * (00–09, 10–19 …) so a number is easy to find. [onValueChange] gets the two-digit string,
 * or null when "No number" is chosen.
 */
@Composable
fun UniformNumberField(
    value: String?,
    onValueChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Uniform Number",
) {
    var open by remember { mutableStateOf(false) }

    Column(modifier) {
        SectionLabel(label)
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(AppShapes.button)
                .background(AppColors.PanelBlack)
                .border(1.dp, AppColors.DividerGray, AppShapes.button)
                .clickable { open = true }
                .padding(horizontal = AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val display = displayJersey(value)
            Text(
                text = display?.let { "#$it" } ?: "Tap to choose",
                style = AppTypography.body,
                color = if (display != null) AppColors.TextPrimary else AppColors.TextMuted,
                modifier = Modifier.weight(1f),
                fontWeight = if (display != null) FontWeight.Bold else FontWeight.Normal,
            )
            Text("▾", style = AppTypography.body, color = AppColors.TextSecondary)
        }
    }

    if (open) {
        UniformNumberPickerDialog(
            current = value,
            onPick = { picked -> onValueChange(picked); open = false },
            onDismiss = { open = false },
        )
    }
}

/**
 * Compact variant of [UniformNumberField] for an inline roster table cell: same 00–99 pen
 * picker, styled to match the neighbouring class/status dropdown cells (no label).
 */
@Composable
fun UniformNumberCell(
    value: String?,
    onValueChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier
            .height(44.dp)
            .clip(AppShapes.button)
            .background(AppColors.PanelBlack)
            .border(1.dp, AppColors.DividerGray, AppShapes.button)
            .clickable { open = true }
            .padding(horizontal = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val display = displayJersey(value)
        Text(
            text = display?.let { "#$it" } ?: "—",
            style = AppTypography.body,
            color = if (display != null) AppColors.TextPrimary else AppColors.TextSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Text("▾", style = AppTypography.microLabel, color = AppColors.TextSecondary)
    }
    if (open) {
        UniformNumberPickerDialog(
            current = value,
            onPick = { picked -> onValueChange(picked); open = false },
            onDismiss = { open = false },
        )
    }
}

@Composable
private fun UniformNumberPickerDialog(
    current: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentInt = current?.trim()?.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.CardCharcoal,
        title = { Text("Uniform Number", color = AppColors.TextPrimary) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(10),
                modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                items(count = 100) { i ->
                    val num = i.toString().padStart(2, '0')
                    NumberCell(
                        text = num,
                        selected = currentInt == i,
                        onClick = { onPick(num) },
                    )
                }
            }
        },
        // "No number" clears the jersey (placeholder players like name-only are allowed).
        confirmButton = { TextButton(onClick = { onPick(null) }) { Text("No number", color = AppColors.Gold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.TextSecondary) } },
    )
}

@Composable
private fun NumberCell(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(AppShapes.button)
            .background(if (selected) AppColors.Gold else AppColors.PanelBlack)
            .border(1.dp, if (selected) AppColors.Gold else AppColors.DividerGray, AppShapes.button)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = AppTypography.chip,
            color = if (selected) AppColors.InkBlack else AppColors.TextPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
