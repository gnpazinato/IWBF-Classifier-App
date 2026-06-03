package com.iwbfclassifier.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.iwbfclassifier.data.model.SportClass
import com.iwbfclassifier.data.model.SportClassStatus
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppShapes
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography

/** Compact tap-to-open dropdown for picking a Sport Class (with a "—" clear option). */
@Composable
fun ClassDropdownCell(
    value: SportClass?,
    onSelect: (SportClass?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        DropdownCellButton(value?.code ?: "—") { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("—") }, onClick = { onSelect(null); expanded = false })
            SportClass.selectable.forEach { sc ->
                DropdownMenuItem(text = { Text(sc.code) }, onClick = { onSelect(sc); expanded = false })
            }
        }
    }
}

/** Compact tap-to-open dropdown for picking a Sport Class Status (with a "—" clear option). */
@Composable
fun StatusDropdownCell(
    value: SportClassStatus?,
    onSelect: (SportClassStatus?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        DropdownCellButton(value?.code ?: "—") { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("—") }, onClick = { onSelect(null); expanded = false })
            SportClassStatus.selectable.forEach { st ->
                DropdownMenuItem(text = { Text("${st.code} · ${st.label}") }, onClick = { onSelect(st); expanded = false })
            }
        }
    }
}

@Composable
private fun DropdownCellButton(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(AppShapes.button)
            .background(AppColors.PanelBlack)
            .border(1.dp, AppColors.DividerGray, AppShapes.button)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = AppTypography.body, color = AppColors.TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
        Text("▾", style = AppTypography.microLabel, color = AppColors.TextSecondary)
    }
}

/**
 * One labeled decision line for the Observation screen: a fixed label, then the Sport
 * Class and the Sport Class Status side by side as dropdowns — so Initial / My Opinion /
 * Final are all visible and editable at a glance, no screen or tab switching (user request).
 */
@Composable
fun ClassStatusRow(
    label: String,
    sportClass: SportClass?,
    onSportClass: (SportClass?) -> Unit,
    status: SportClassStatus?,
    onStatus: (SportClassStatus?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Text(label, style = AppTypography.body, color = AppColors.TextSecondary, modifier = Modifier.weight(1.2f))
        ClassDropdownCell(sportClass, onSportClass, Modifier.weight(1f))
        StatusDropdownCell(status, onStatus, Modifier.weight(1.6f))
    }
}
