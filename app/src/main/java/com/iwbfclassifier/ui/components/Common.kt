package com.iwbfclassifier.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppShapes
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = AppTypography.microLabel, color = AppColors.TextMuted, modifier = modifier)
}

@Composable
fun SaveIndicator(saving: Boolean, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (saving) AppColors.Gold else AppColors.TextMuted))
        Spacer(Modifier.width(6.dp))
        Text(if (saving) "Saving…" else "Saved", style = AppTypography.microLabel, color = AppColors.TextSecondary)
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { p -> { Text(p) } },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = AppShapes.button,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Gold,
            unfocusedBorderColor = AppColors.DividerGray,
            focusedLabelColor = AppColors.Gold,
            unfocusedLabelColor = AppColors.TextMuted,
            focusedTextColor = AppColors.TextPrimary,
            unfocusedTextColor = AppColors.TextPrimary,
            cursorColor = AppColors.Gold,
            focusedContainerColor = AppColors.PanelBlack,
            unfocusedContainerColor = AppColors.PanelBlack,
        ),
        modifier = modifier,
    )
}

@Composable
fun EmptyState(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = AppTypography.header, color = AppColors.TextSecondary, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Spacer(Modifier.height(AppSpacing.sm))
            Text(subtitle, style = AppTypography.body, color = AppColors.TextMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = AppColors.TextPrimary) },
        text = { Text(message, color = AppColors.TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = if (destructive) AppColors.AlertRed else AppColors.Gold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText, color = AppColors.TextSecondary) }
        },
        containerColor = AppColors.CardCharcoal,
    )
}
