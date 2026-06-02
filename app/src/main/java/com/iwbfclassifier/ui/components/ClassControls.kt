package com.iwbfclassifier.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.iwbfclassifier.data.model.SportClass
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppShapes
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography

/** Which decision field the class buttons currently edit (docs/02). */
enum class ClassTarget(val label: String) {
    Starting("Initial"),
    MyOpinion("My Opinion"),
    Final("Final"),
}

@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.clip(AppShapes.button).border(1.dp, AppColors.DividerGray, AppShapes.button)) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(if (isSelected) AppColors.Gold else AppColors.PanelBlack)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = AppTypography.chip,
                    color = if (isSelected) AppColors.InkBlack else AppColors.TextSecondary,
                )
            }
            if (index < options.lastIndex) {
                Box(Modifier.width(1.dp).height(44.dp).background(AppColors.DividerGray))
            }
        }
    }
}

/** Tappable Sport Class buttons (docs/03, docs/12). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassButtonRow(
    selected: SportClass?,
    onSelect: (SportClass) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        SportClass.selectable.forEach { sportClass ->
            val isSelected = sportClass == selected
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .widthIn(min = 52.dp)
                    .clip(AppShapes.button)
                    .background(if (isSelected) AppColors.Gold else AppColors.CardCharcoal)
                    .border(1.dp, if (isSelected) AppColors.Gold else AppColors.DividerGray, AppShapes.button)
                    .clickable { onSelect(sportClass) }
                    .padding(horizontal = AppSpacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    sportClass.code,
                    style = AppTypography.chip,
                    color = if (isSelected) AppColors.InkBlack else AppColors.TextPrimary,
                )
            }
        }
    }
}

/** Choose the target decision field, then tap a class to set it (docs/03). */
@Composable
fun ClassSelector(
    target: ClassTarget,
    onTargetChange: (ClassTarget) -> Unit,
    valueForTarget: SportClass?,
    onSelectClass: (SportClass) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        SegmentedControl(
            options = ClassTarget.entries.toList(),
            selected = target,
            label = { it.label },
            onSelect = onTargetChange,
            modifier = Modifier.fillMaxWidth(),
        )
        ClassButtonRow(selected = valueForTarget, onSelect = onSelectClass)
    }
}
