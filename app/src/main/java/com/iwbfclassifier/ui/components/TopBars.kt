package com.iwbfclassifier.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography

/** Standard dark top bar for CRUD screens. */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(color = AppColors.PanelBlack, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) BackButton(onBack) else Spacer(Modifier.width(AppSpacing.sm))
            Column(Modifier.weight(1f).padding(start = AppSpacing.sm)) {
                Text(title, style = AppTypography.screenTitle, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = AppTypography.microLabel, color = AppColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            actions()
        }
    }
}

/** Compact observation header: competition / game / save indicator (docs/12). */
@Composable
fun ObservationTopBar(
    competitionName: String,
    gameName: String?,
    saving: Boolean,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(color = AppColors.PanelBlack, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) BackButton(onBack) else Spacer(Modifier.width(AppSpacing.sm))
            Column(Modifier.weight(1f).padding(start = AppSpacing.sm)) {
                Text(competitionName, style = AppTypography.header, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!gameName.isNullOrBlank()) {
                    Text(gameName, style = AppTypography.microLabel, color = AppColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            SaveIndicator(saving)
            Spacer(Modifier.width(AppSpacing.sm))
            actions()
        }
    }
}
