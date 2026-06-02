package com.iwbfclassifier.ui.roster

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iwbfclassifier.data.model.Player
import com.iwbfclassifier.data.model.SportClass
import com.iwbfclassifier.data.model.SportClassStatus
import com.iwbfclassifier.data.model.Team
import com.iwbfclassifier.data.repository.CompetitionRepository
import com.iwbfclassifier.ui.LocalAppContainer
import com.iwbfclassifier.ui.components.AppTextField
import com.iwbfclassifier.ui.components.AppTopBar
import com.iwbfclassifier.ui.components.ClassButtonRow
import com.iwbfclassifier.ui.components.EmptyState
import com.iwbfclassifier.ui.components.SecondaryButton
import com.iwbfclassifier.ui.components.SectionLabel
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppShapes
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RosterOverviewViewModel(
    private val repo: CompetitionRepository,
    private val competitionId: String,
) : ViewModel() {

    val teams = repo.teams
        .map { list -> list.filter { it.competitionId == competitionId && it.active } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.teams.value.filter { it.competitionId == competitionId && it.active })

    val players = repo.players
        .map { list -> list.filter { it.competitionId == competitionId && it.active } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.players.value.filter { it.competitionId == competitionId && it.active })

    fun update(player: Player) {
        viewModelScope.launch { repo.updatePlayer(player) }
    }
}

/**
 * At-a-glance roster across all teams with fast inline editing (user request): scroll
 * every athlete and see number, name, Initial/Opinion/Final class and status without
 * opening each one; tap a row to edit those fields inline. Autosaves on every change.
 */
@Composable
fun RosterOverviewScreen(
    competitionId: String,
    onBack: () -> Unit,
    onOpenPlayer: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: RosterOverviewViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RosterOverviewViewModel(container.competitionRepository, competitionId) }
        },
    )
    val teams by vm.teams.collectAsStateWithLifecycle()
    val players by vm.players.collectAsStateWithLifecycle()

    var expandedId by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(AppColors.InkBlack)) {
        AppTopBar(title = "Players Overview", subtitle = "${players.size} players · tap to edit", onBack = onBack)

        if (teams.isEmpty()) {
            EmptyState("No teams yet", "Import the roster template or add teams to see players here.")
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            teams.forEach { team ->
                val teamPlayers = players
                    .filter { it.teamId == team.id }
                    .sortedBy { it.uniformNumber?.toIntOrNull() ?: Int.MAX_VALUE }

                item(key = "team-${team.id}") {
                    Text(
                        "${team.name}  ·  ${teamPlayers.size}",
                        style = AppTypography.header,
                        color = AppColors.Gold,
                        modifier = Modifier.padding(top = AppSpacing.md, bottom = AppSpacing.xs),
                    )
                }

                items(teamPlayers, key = { it.id }) { player ->
                    PlayerOverviewRow(
                        player = player,
                        expanded = expandedId == player.id,
                        onToggle = { expandedId = if (expandedId == player.id) null else player.id },
                        onChange = vm::update,
                        onOpenFull = { onOpenPlayer(player.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerOverviewRow(
    player: Player,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (Player) -> Unit,
    onOpenFull: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.CardCharcoal)
            .border(1.dp, if (expanded) AppColors.GoldBorder else AppColors.DividerGray, AppShapes.card)
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        // Collapsed summary — readable at a glance.
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                player.uniformNumber?.let { "#$it" } ?: "#—",
                style = AppTypography.header,
                color = AppColors.TextPrimary,
                modifier = Modifier.widthIn(min = 52.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(player.name ?: "Unknown", style = AppTypography.body, color = AppColors.TextPrimary)
                val summary = buildList {
                    add("Initial ${player.startingSportClass?.code ?: "—"}")
                    add("Opinion ${player.myOpinionSportClass?.code ?: "—"}")
                    add("Final ${player.finalSportClass?.code ?: "—"}")
                    player.sportClassStatus?.let { add(it.code) }
                }.joinToString("  ·  ")
                Text(summary, style = AppTypography.microLabel, color = AppColors.Gold)
            }
            Text(if (expanded) "▴" else "▾", style = AppTypography.header, color = AppColors.TextSecondary)
        }

        if (expanded) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                AppTextField(
                    player.uniformNumber.orEmpty(),
                    { v -> onChange(player.copy(uniformNumber = v.ifBlank { null })) },
                    "Number",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.widthIn(max = 120.dp),
                )
                AppTextField(
                    player.name.orEmpty(),
                    { v -> onChange(player.copy(name = v.ifBlank { null })) },
                    "Full name",
                    modifier = Modifier.weight(1f),
                )
            }
            QuickClass("Initial Class", player.startingSportClass) { sc ->
                onChange(player.copy(startingSportClass = if (sc == player.startingSportClass) null else sc))
            }
            QuickClass("My Opinion Class", player.myOpinionSportClass) { sc ->
                onChange(player.copy(myOpinionSportClass = if (sc == player.myOpinionSportClass) null else sc))
            }
            QuickClass("Final Class", player.finalSportClass) { sc ->
                onChange(player.copy(finalSportClass = if (sc == player.finalSportClass) null else sc))
            }
            QuickStatus(player.sportClassStatus) { st ->
                onChange(player.copy(sportClassStatus = if (st == player.sportClassStatus) null else st))
            }
            SecondaryButton("Open full player details", onClick = onOpenFull, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun QuickClass(label: String, value: SportClass?, onSelect: (SportClass) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        SectionLabel(label)
        ClassButtonRow(selected = value, onSelect = onSelect)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickStatus(value: SportClassStatus?, onSelect: (SportClassStatus) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        SectionLabel("Sport Class Status")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            SportClassStatus.selectable.forEach { status ->
                val selected = status == value
                Box(
                    Modifier
                        .height(44.dp)
                        .widthIn(min = 52.dp)
                        .clip(AppShapes.chip)
                        .background(if (selected) AppColors.Gold else AppColors.CardCharcoal)
                        .border(1.dp, if (selected) AppColors.Gold else AppColors.DividerGray, AppShapes.chip)
                        .clickable { onSelect(status) }
                        .padding(horizontal = AppSpacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(status.code, style = AppTypography.chip, color = if (selected) AppColors.InkBlack else AppColors.TextPrimary)
                }
            }
        }
    }
}
