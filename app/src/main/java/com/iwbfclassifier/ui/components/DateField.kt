package com.iwbfclassifier.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iwbfclassifier.core.epochMillisToIso
import com.iwbfclassifier.core.isoToDisplayDate
import com.iwbfclassifier.core.isoToEpochMillis
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppShapes
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography

/**
 * Tap-to-pick date field. Stores ISO (yyyy-MM-dd) via [onIsoChange] but shows the
 * value as dd/MM/yyyy. Avoids typing during game flow (docs/03).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    isoValue: String?,
    onIsoChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val display = isoToDisplayDate(isoValue)

    Column(modifier = modifier) {
        SectionLabel(label)
        Spacer(Modifier.height(AppSpacing.xs))
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .border(1.dp, AppColors.DividerGray, AppShapes.button)
                .clickable { showPicker = true }
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = display ?: "dd/mm/yyyy — tap to choose",
                style = AppTypography.body,
                color = if (display != null) AppColors.TextPrimary else AppColors.TextMuted,
            )
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = isoToEpochMillis(isoValue))
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            colors = DatePickerDefaults.colors(containerColor = AppColors.CardCharcoal),
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onIsoChange(epochMillisToIso(it)) }
                    showPicker = false
                }) { Text("OK", color = AppColors.Gold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    onIsoChange(null)
                    showPicker = false
                }) { Text("Clear", color = AppColors.TextSecondary) }
            },
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}
