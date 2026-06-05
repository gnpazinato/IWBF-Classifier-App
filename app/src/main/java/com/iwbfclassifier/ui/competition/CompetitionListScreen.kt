package com.iwbfclassifier.ui.competition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iwbfclassifier.core.isoToDisplayDate
import com.iwbfclassifier.data.model.Competition
import com.iwbfclassifier.data.repository.CompetitionRepository
import com.iwbfclassifier.ui.LocalAppContainer
import com.iwbfclassifier.ui.components.AppTextField
import com.iwbfclassifier.ui.components.AppTopBar
import com.iwbfclassifier.ui.components.EmptyState
import com.iwbfclassifier.ui.components.PrimaryButton
import com.iwbfclassifier.ui.components.SecondaryButton
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppShapes
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography
import kotlinx.coroutines.launch

class CompetitionListViewModel(private val repo: CompetitionRepository) : ViewModel() {
    val competitions = repo.competitions
    val teams = repo.teams
    val players = repo.players

    fun create(name: String, location: String?, startDate: String?) {
        viewModelScope.launch { repo.createCompetition(name, location, startDate) }
    }
}

@Composable
fun CompetitionListScreen(
    onOpenCompetition: (String) -> Unit,
    onImport: () -> Unit,
    onOpenBackup: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: CompetitionListViewModel = viewModel(
        factory = viewModelFactory { initializer { CompetitionListViewModel(container.competitionRepository) } },
    )
    val competitions by vm.competitions.collectAsStateWithLifecycle()
    val teams by vm.teams.collectAsStateWithLifecycle()
    val players by vm.players.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AppColors.InkBlack)) {
        AppTopBar(
            title = "IWBF Classifier App",
            subtitle = "Wheelchair Basketball Observation",
            actions = {
                SecondaryButton("Backup", onClick = onOpenBackup)
                Spacer(Modifier.width(AppSpacing.sm))
            },
        )

        // Two primary entry points, always on the home screen (user request).
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            HomeActionCard(
                title = "New Competition",
                subtitle = "Start an empty competition and add teams/players.",
                primary = true,
                onClick = { showCreate = true },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            HomeActionCard(
                title = "Import Players",
                subtitle = "Upload the Excel template (.xlsx) with the players' information.",
                primary = false,
                onClick = onImport,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        if (competitions.isEmpty()) {
            EmptyState(
                "No competitions yet",
                "Create a competition or import entry lists to get started.",
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                items(competitions, key = { it.id }) { competition ->
                    CompetitionCard(
                        competition = competition,
                        teamCount = teams.count { it.competitionId == competition.id && it.active },
                        playerCount = players.count { it.competitionId == competition.id && it.active },
                        onClick = { onOpenCompetition(competition.id) },
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateCompetitionDialog(
            onDismiss = { showCreate = false },
            onCreate = { name ->
                vm.create(name, null, null)
                showCreate = false
            },
        )
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(AppShapes.card)
            .background(if (primary) AppColors.Gold else AppColors.CardCharcoal)
            .border(1.dp, if (primary) AppColors.Gold else AppColors.GoldBorder, AppShapes.card)
            .clickable(onClick = onClick)
            .padding(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        Text(
            title,
            style = AppTypography.header,
            color = if (primary) AppColors.InkBlack else AppColors.TextPrimary,
        )
        Text(
            subtitle,
            style = AppTypography.body,
            color = if (primary) AppColors.InkBlack else AppColors.TextSecondary,
        )
    }
}

@Composable
private fun CompetitionCard(
    competition: Competition,
    teamCount: Int,
    playerCount: Int,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.CardCharcoal)
            .border(1.dp, AppColors.DividerGray, AppShapes.card)
            .clickable(onClick = onClick)
            .padding(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        Text(competition.name, style = AppTypography.header, color = AppColors.TextPrimary)
        if (!competition.location.isNullOrBlank()) {
            Text(competition.location, style = AppTypography.body, color = AppColors.TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            Text("$teamCount Teams", style = AppTypography.microLabel, color = AppColors.Gold)
            Text("$playerCount Players", style = AppTypography.microLabel, color = AppColors.Gold)
            val dates = listOfNotNull(
                isoToDisplayDate(competition.startDate),
                isoToDisplayDate(competition.endDate),
            ).joinToString(" – ")
            if (dates.isNotBlank()) {
                Text(dates, style = AppTypography.microLabel, color = AppColors.TextMuted)
            }
        }
    }
}

@Composable
private fun CreateCompetitionDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.CardCharcoal,
        title = { Text("New Competition", color = AppColors.TextPrimary) },
        text = {
            // Only the name is needed — date/location aren't part of the roster template.
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                AppTextField(name, { name = it }, "Competition name")
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) {
                Text("Create", color = if (name.isNotBlank()) AppColors.Gold else AppColors.TextMuted)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.TextSecondary) } },
    )
}
