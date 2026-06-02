package com.iwbfclassifier.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iwbfclassifier.data.model.ObservationStatus
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppShapes
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography

/** Status accent color (docs/12). */
fun observationStatusColor(status: ObservationStatus): Color = when (status) {
    ObservationStatus.Discuss -> AppColors.AlertRed
    ObservationStatus.Observe -> AppColors.Gold
    ObservationStatus.QuickCheck -> AppColors.InfoBlue
    ObservationStatus.Finalized -> AppColors.TextMuted
    ObservationStatus.NotObserved -> AppColors.DividerGray
}

/**
 * Roster chip — one-tap player switching (docs/03). Shows number, name and a
 * class+status hint with a status-colored left rail. Designed to stay compact so
 * up to 24 players fit on screen.
 */
@Composable
fun PlayerChip(
    number: String?,
    name: String?,
    classText: String?,
    status: ObservationStatus,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .clip(AppShapes.chip)
            .background(if (selected) AppColors.GoldSoft else AppColors.CardCharcoal)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) AppColors.Gold else AppColors.DividerGray, AppShapes.chip)
            .clickable(onClick = onClick)
            .alpha(if (active) 1f else 0.45f)
            .padding(vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).height(32.dp).background(observationStatusColor(status)))
        Spacer(Modifier.width(AppSpacing.sm))
        Text(
            text = number?.let { "#$it" } ?: "#–",
            style = AppTypography.chip,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(AppSpacing.sm))
        Text(
            text = (name ?: "Unknown").uppercase(),
            style = AppTypography.chip,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (!classText.isNullOrBlank()) {
            Spacer(Modifier.width(AppSpacing.sm))
            Text(classText, style = AppTypography.microLabel, color = AppColors.Gold)
        }
        Spacer(Modifier.width(AppSpacing.sm))
    }
}
