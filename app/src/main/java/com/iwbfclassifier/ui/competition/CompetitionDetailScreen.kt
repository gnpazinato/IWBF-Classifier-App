package com.iwbfclassifier.ui.competition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iwbfclassifier.data.model.Competition
import com.iwbfclassifier.data.model.Team
import com.iwbfclassifier.data.repository.CompetitionRepository
import com.iwbfclassifier.ui.LocalAppContainer
import com.iwbfclassifier.ui.components.AppTextField
import com.iwbfclassifier.ui.components.AppTopBar
import com.iwbfclassifier.ui.components.ConfirmDialog
import com.iwbfclassifier.ui.components.DateField
import com.iwbfclassifier.ui.components.DestructiveButton
import com.iwbfclassifier.ui.components.EmptyState
import com.iwbfclassifier.ui.components.PrimaryButton
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

class CompetitionDetailViewModel(
    private val repo: CompetitionRepository,
    private val competitionId: String,
) : ViewModel() {

    val competition = repo.competitions
        .map { list -> list.firstOrNull { it.id == competitionId } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            repo.competitions.value.firstOrNull { it.id == competitionId },
        )

    val teams = repo.teams
        .map { list -> list.filter { it.competitionId == competitionId } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            repo.teams.value.filter { it.competitionId == competitionId },
        )

    val players = repo.players
        .map { list -> list.filter { it.competitionId == competitionId } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            repo.players.value.filter { it.competitionId == competitionId },
        )

    fun addTeam(name: String, code: String?, gender: String?) {
        viewModelScope.launch { repo.createTeam(competitionId, name, code, gender) }
    }

    fun setTeamActive(teamId: String, active: Boolean) {
        viewModelScope.launch { repo.setTeamActive(teamId, active) }
    }

    fun deleteTeam(teamId: String) {
        viewModelScope.launch { repo.deleteTeamPermanently(teamId) }
    }

    fun updateCompetition(updated: Competition) {
        viewModelScope.launch { repo.updateCompetition(updated) }
    }

    fun deleteCompetition(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteCompetition(competitionId)
            onDone()
        }
    }
}

@Composable
fun CompetitionDetailScreen(
    competitionId: String,
    onBack: () -> Unit,
    onOpenTeam: (String) -> Unit,
    onOpenObservation: () -> Unit,
    onOpenImport: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: CompetitionDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CompetitionDetailViewModel(container.competitionRepository, competitionId) }
        },
    )
    val competition by vm.competition.collectAsStateWithLifecycle()
    val teams by vm.teams.collectAsStateWithLifecycle()
    val players by vm.players.collectAsStateWithLifecycle()

    var showAddTeam by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }

    val activeTeams = teams.filter { it.active }
    val archivedTeams = teams.filter { !it.active }

    Column(Modifier.fillMaxSize().background(AppColors.InkBlack)) {
        AppTopBar(
            title = competition?.name ?: "Competition",
            subtitle = competition?.location,
            onBack = onBack,
            actions = {
                SecondaryButton("Edit", onClick = { showEdit = true })
                Spacer(Modifier.width(AppSpacing.sm))
            },
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            item {
                PrimaryButton(
                    "Open Observation",
                    onClick = onOpenObservation,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                SecondaryButton(
                    "Import Roster Files (ZIP / Word / Excel / PDF)",
                    onClick = onOpenImport,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel("Teams", modifier = Modifier.weight(1f))
                    SecondaryButton("Add Team", onClick = { showAddTeam = true })
                }
            }

            if (activeTeams.isEmpty() && archivedTeams.isEmpty()) {
                item { EmptyState("No teams yet", "Add a team manually, or import an entry list later.") }
            }

            items(activeTeams, key = { it.id }) { team ->
                TeamRow(
                    team = team,
                    playerCount = players.count { it.teamId == team.id && it.active },
                    onClick = { onOpenTeam(team.id) },
                    onArchive = { vm.setTeamActive(team.id, false) },
                    onRestore = null,
                )
            }

            if (archivedTeams.isNotEmpty()) {
                item { SectionLabel("Archived", modifier = Modifier.padding(top = AppSpacing.sm)) }
                items(archivedTeams, key = { it.id }) { team ->
                    TeamRow(
                        team = team,
                        playerCount = players.count { it.teamId == team.id },
                        onClick = { onOpenTeam(team.id) },
                        onArchive = null,
                        onRestore = { vm.setTeamActive(team.id, true) },
                    )
                }
            }
        }
    }

    if (showAddTeam) {
        AddTeamDialog(
            onDismiss = { showAddTeam = false },
            onAdd = { name, code, gender ->
                vm.addTeam(name, code, gender)
                showAddTeam = false
            },
        )
    }

    if (showEdit) {
        competition?.let { current ->
            EditCompetitionDialog(
                competition = current,
                onDismiss = { showEdit = false },
                onSave = { updated ->
                    vm.updateCompetition(updated)
                    showEdit = false
                },
                onDelete = {
                    showEdit = false
                    vm.deleteCompetition(onDone = onBack)
                },
            )
        }
    }
}

@Composable
private fun TeamRow(
    team: Team,
    playerCount: Int,
    onClick: () -> Unit,
    onArchive: (() -> Unit)?,
    onRestore: (() -> Unit)?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.CardCharcoal)
            .border(1.dp, AppColors.DividerGray, AppShapes.card)
            .clickable(onClick = onClick)
            .alpha(if (team.active) 1f else 0.5f)
            .padding(AppSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(team.name, style = AppTypography.header, color = AppColors.TextPrimary)
            val subtitle = listOfNotNull(team.code, team.gender, "$playerCount Players").joinToString(" · ")
            Text(subtitle, style = AppTypography.microLabel, color = AppColors.TextMuted)
        }
        if (onArchive != null) TextButton(onClick = onArchive) { Text("Archive", color = AppColors.TextSecondary) }
        if (onRestore != null) TextButton(onClick = onRestore) { Text("Restore", color = AppColors.Gold) }
    }
}

@Composable
private fun AddTeamDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, code: String?, gender: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.CardCharcoal,
        title = { Text("Add Team", color = AppColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                AppTextField(name, { name = it }, "Team name")
                AppTextField(code, { code = it }, "Code (optional)")
                AppTextField(gender, { gender = it }, "Gender (optional)")
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, code, gender) }, enabled = name.isNotBlank()) {
                Text("Add", color = if (name.isNotBlank()) AppColors.Gold else AppColors.TextMuted)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.TextSecondary) } },
    )
}

@Composable
private fun EditCompetitionDialog(
    competition: Competition,
    onDismiss: () -> Unit,
    onSave: (Competition) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(competition.name) }
    var location by remember { mutableStateOf(competition.location.orEmpty()) }
    var startDate by remember { mutableStateOf(competition.startDate.orEmpty()) }
    var endDate by remember { mutableStateOf(competition.endDate.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.CardCharcoal,
        title = { Text("Edit Competition", color = AppColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                AppTextField(name, { name = it }, "Name")
                AppTextField(location, { location = it }, "Location")
                DateField("Start Date", startDate.ifBlank { null }, { startDate = it.orEmpty() })
                DateField("End Date", endDate.ifBlank { null }, { endDate = it.orEmpty() })
                Spacer(Modifier.width(AppSpacing.sm))
                DestructiveButton("Delete Competition", onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        competition.copy(
                            name = name.ifBlank { competition.name },
                            location = location.ifBlank { null },
                            startDate = startDate.ifBlank { null },
                            endDate = endDate.ifBlank { null },
                        ),
                    )
                },
            ) { Text("Save", color = AppColors.Gold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.TextSecondary) } },
    )

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete competition?",
            message = "This permanently removes the competition and all of its teams, players and notes. This cannot be undone.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
