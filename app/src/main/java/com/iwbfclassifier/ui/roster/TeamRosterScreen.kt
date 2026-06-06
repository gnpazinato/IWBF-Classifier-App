package com.iwbfclassifier.ui.roster

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iwbfclassifier.data.model.Player
import com.iwbfclassifier.data.repository.CompetitionRepository
import com.iwbfclassifier.ui.LocalAppContainer
import com.iwbfclassifier.ui.components.AppTextField
import com.iwbfclassifier.ui.components.AppTopBar
import com.iwbfclassifier.ui.components.EmptyState
import com.iwbfclassifier.ui.components.PlayerChip
import com.iwbfclassifier.ui.components.SecondaryButton
import com.iwbfclassifier.ui.components.SectionLabel
import com.iwbfclassifier.ui.components.UniformNumberField
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppSpacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TeamRosterViewModel(
    private val repo: CompetitionRepository,
    private val competitionId: String,
    private val teamId: String,
) : ViewModel() {

    val team = repo.teams
        .map { list -> list.firstOrNull { it.id == teamId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.teams.value.firstOrNull { it.id == teamId })

    val players = repo.players
        .map { list -> list.filter { it.teamId == teamId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.players.value.filter { it.teamId == teamId })

    fun addPlayer(number: String, name: String) {
        viewModelScope.launch { repo.createPlayer(competitionId, teamId, number, name) }
    }

    fun setPlayerActive(playerId: String, active: Boolean) {
        viewModelScope.launch { repo.setPlayerActive(playerId, active) }
    }
}

/** The athlete's single official class + status for a chip hint (My Opinion / Final live
 *  only on the Observation screen, per user request). */
private fun Player.chipClassText(): String? {
    val sportClass = startingSportClass ?: importedSportClass
    return listOfNotNull(sportClass?.code, sportClassStatus?.code).joinToString(" ").ifBlank { null }
}

@Composable
fun TeamRosterScreen(
    competitionId: String,
    teamId: String,
    onBack: () -> Unit,
    onOpenPlayer: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: TeamRosterViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TeamRosterViewModel(container.competitionRepository, competitionId, teamId) }
        },
    )
    val team by vm.team.collectAsStateWithLifecycle()
    val players by vm.players.collectAsStateWithLifecycle()
    var showAddPlayer by remember { mutableStateOf(false) }

    val active = players
        .filter { it.active }
        .sortedBy { it.uniformNumber?.toIntOrNull() ?: Int.MAX_VALUE }
    val archived = players.filter { !it.active }

    Column(Modifier.fillMaxSize().background(AppColors.InkBlack)) {
        AppTopBar(
            title = team?.name ?: "Team",
            subtitle = team?.code,
            onBack = onBack,
            actions = {
                SecondaryButton("Add Player", onClick = { showAddPlayer = true })
                Spacer(Modifier.width(AppSpacing.sm))
            },
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            if (active.isEmpty() && archived.isEmpty()) {
                item { EmptyState("No players yet", "Add players manually — placeholders like “#8 Unknown” are fine.") }
            }

            items(active, key = { it.id }) { player ->
                PlayerChip(
                    number = player.uniformNumber,
                    name = player.name,
                    classText = player.chipClassText(),
                    status = player.observationStatus,
                    selected = false,
                    onClick = { onOpenPlayer(player.id) },
                )
            }

            if (archived.isNotEmpty()) {
                item { SectionLabel("Archived", modifier = Modifier.padding(top = AppSpacing.sm)) }
                items(archived, key = { it.id }) { player ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerChip(
                            number = player.uniformNumber,
                            name = player.name,
                            classText = player.chipClassText(),
                            status = player.observationStatus,
                            selected = false,
                            active = false,
                            onClick = { onOpenPlayer(player.id) },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { vm.setPlayerActive(player.id, true) }) {
                            Text("Restore", color = AppColors.Gold)
                        }
                    }
                }
            }
        }
    }

    if (showAddPlayer) {
        AddPlayerDialog(
            onDismiss = { showAddPlayer = false },
            onAdd = { number, name ->
                vm.addPlayer(number, name)
                showAddPlayer = false
            },
        )
    }
}

@Composable
private fun AddPlayerDialog(
    onDismiss: () -> Unit,
    onAdd: (number: String, name: String) -> Unit,
) {
    var number by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.CardCharcoal,
        title = { Text("Add Player", color = AppColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                UniformNumberField(value = number, onValueChange = { number = it }, label = "Uniform number")
                AppTextField(name, { name = it }, "Player name (optional)")
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(number.orEmpty(), name) }, enabled = !number.isNullOrBlank() || name.isNotBlank()) {
                Text("Add", color = AppColors.Gold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.TextSecondary) } },
    )
}
