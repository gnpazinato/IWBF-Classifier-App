package com.iwbfclassifier.ui.observation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iwbfclassifier.data.model.Player
import com.iwbfclassifier.data.model.SportClass
import com.iwbfclassifier.data.model.Team
import com.iwbfclassifier.data.repository.CompetitionRepository
import com.iwbfclassifier.ui.LocalAppContainer
import com.iwbfclassifier.ui.components.ClassSelector
import com.iwbfclassifier.ui.components.ClassTarget
import com.iwbfclassifier.ui.components.EmptyState
import com.iwbfclassifier.ui.components.ObservationTopBar
import com.iwbfclassifier.ui.components.PaperNoteCanvasContainer
import com.iwbfclassifier.ui.components.PlayerChip
import com.iwbfclassifier.ui.components.SecondaryButton
import com.iwbfclassifier.ui.components.SectionLabel
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ObservationViewModel(
    private val repo: CompetitionRepository,
    private val competitionId: String,
) : ViewModel() {

    val competition = repo.competitions
        .map { list -> list.firstOrNull { it.id == competitionId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.competitions.value.firstOrNull { it.id == competitionId })

    val teams = repo.teams
        .map { list -> list.filter { it.competitionId == competitionId && it.active } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.teams.value.filter { it.competitionId == competitionId && it.active })

    val players = repo.players
        .map { list -> list.filter { it.competitionId == competitionId && it.active } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.players.value.filter { it.competitionId == competitionId && it.active })

    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()

    fun save(updated: Player) {
        viewModelScope.launch {
            _saving.value = true
            repo.updatePlayer(updated)
            _saving.value = false
        }
    }
}

@Composable
fun ObservationScreen(
    competitionId: String,
    onBack: () -> Unit,
    onOpenPlayer: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: ObservationViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ObservationViewModel(container.competitionRepository, competitionId) }
        },
    )
    val competition by vm.competition.collectAsStateWithLifecycle()
    val teams by vm.teams.collectAsStateWithLifecycle()
    val players by vm.players.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()

    var selectedPlayerId by remember { mutableStateOf<String?>(null) }
    var target by remember { mutableStateOf(ClassTarget.MyOpinion) }

    val selectedPlayer = players.firstOrNull { it.id == selectedPlayerId }
    val teamA = teams.getOrNull(0)
    val teamB = teams.getOrNull(1)

    Column(Modifier.fillMaxSize().background(AppColors.InkBlack)) {
        ObservationTopBar(
            competitionName = competition?.name ?: "Observation",
            gameName = null,
            saving = saving,
            onBack = onBack,
        )

        Row(Modifier.fillMaxSize()) {
            PlayerRail(
                team = teamA,
                players = players,
                selectedPlayerId = selectedPlayerId,
                onSelect = { selectedPlayerId = it },
                modifier = Modifier.weight(0.24f).fillMaxHeight(),
            )

            Column(
                Modifier
                    .weight(0.52f)
                    .fillMaxHeight()
                    .padding(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                if (selectedPlayer == null) {
                    EmptyState("Select a player", "Tap a player chip to start observing.")
                } else {
                    PlayerHeader(player = selectedPlayer, onEditDetails = { onOpenPlayer(selectedPlayer.id) })
                    ClassSelector(
                        target = target,
                        onTargetChange = { target = it },
                        valueForTarget = when (target) {
                            ClassTarget.Starting -> selectedPlayer.startingSportClass
                            ClassTarget.MyOpinion -> selectedPlayer.myOpinionSportClass
                            ClassTarget.Final -> selectedPlayer.finalSportClass
                        },
                        onSelectClass = { sportClass ->
                            vm.save(applyClass(selectedPlayer, target, sportClass))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                PaperNoteCanvasContainer(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "S Pen note canvas — Phase 3",
                            style = AppTypography.body,
                            color = AppColors.TextMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            PlayerRail(
                team = teamB,
                players = players,
                selectedPlayerId = selectedPlayerId,
                onSelect = { selectedPlayerId = it },
                modifier = Modifier.weight(0.24f).fillMaxHeight(),
            )
        }
    }
}

private fun applyClass(player: Player, target: ClassTarget, sportClass: SportClass): Player =
    when (target) {
        ClassTarget.Starting -> player.copy(startingSportClass = sportClass)
        ClassTarget.MyOpinion -> player.copy(myOpinionSportClass = sportClass)
        ClassTarget.Final -> player.copy(finalSportClass = sportClass)
    }

@Composable
private fun PlayerRail(
    team: Team?,
    players: List<Player>,
    selectedPlayerId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.background(AppColors.PanelBlack).padding(AppSpacing.sm)) {
        SectionLabel(team?.name ?: "—", modifier = Modifier.padding(AppSpacing.sm))
        val teamPlayers = players
            .filter { it.teamId == team?.id }
            .sortedBy { it.uniformNumber?.toIntOrNull() ?: Int.MAX_VALUE }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = AppSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            items(teamPlayers, key = { it.id }) { player ->
                val sportClass = player.myOpinionSportClass ?: player.startingSportClass ?: player.importedSportClass
                PlayerChip(
                    number = player.uniformNumber,
                    name = player.name,
                    classText = listOfNotNull(sportClass?.code, player.sportClassStatus?.code).joinToString(" ").ifBlank { null },
                    status = player.observationStatus,
                    selected = player.id == selectedPlayerId,
                    onClick = { onSelect(player.id) },
                )
            }
        }
    }
}

@Composable
private fun PlayerHeader(player: Player, onEditDetails: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                listOfNotNull(player.uniformNumber?.let { "#$it" }, (player.name ?: "Unknown")).joinToString(" "),
                style = AppTypography.header,
                color = AppColors.TextPrimary,
            )
            val summary = buildList {
                player.importedSportClass?.let { add("Imported ${it.code}") }
                player.startingSportClass?.let { add("Start ${it.code}") }
                player.myOpinionSportClass?.let { add("Opinion ${it.code}") }
                player.finalSportClass?.let { add("Final ${it.code}") }
            }.joinToString("  ·  ")
            if (summary.isNotBlank()) {
                Text(summary, style = AppTypography.microLabel, color = AppColors.Gold)
            }
        }
        SecondaryButton("Edit details", onClick = onEditDetails)
    }
}
