package com.iwbfclassifier.ui.competition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
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
import com.iwbfclassifier.core.extractYoutubeId
import com.iwbfclassifier.core.newId
import com.iwbfclassifier.data.model.Competition
import com.iwbfclassifier.data.model.Game
import com.iwbfclassifier.data.model.Player
import com.iwbfclassifier.data.model.SportClass
import com.iwbfclassifier.data.model.SportClassStatus
import com.iwbfclassifier.data.model.Team
import com.iwbfclassifier.data.model.YoutubeInfo
import com.iwbfclassifier.data.model.displayName
import com.iwbfclassifier.data.repository.CompetitionRepository
import com.iwbfclassifier.ui.LocalAppContainer
import com.iwbfclassifier.ui.components.AppTextField
import com.iwbfclassifier.ui.components.AppTopBar
import com.iwbfclassifier.ui.components.ClassDropdownCell
import com.iwbfclassifier.ui.components.StatusDropdownCell
import com.iwbfclassifier.ui.components.ConfirmDialog
import com.iwbfclassifier.ui.components.DestructiveButton
import com.iwbfclassifier.ui.components.EmptyState
import com.iwbfclassifier.ui.components.PrimaryButton
import com.iwbfclassifier.ui.components.SecondaryButton
import com.iwbfclassifier.ui.components.SectionLabel
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppShapes
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
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

    private val _game = MutableStateFlow<Game?>(null)
    val game = _game.asStateFlow()

    init {
        viewModelScope.launch { _game.value = repo.loadGame(competitionId) }
    }

    fun addTeam(name: String, code: String?, gender: String?) {
        viewModelScope.launch { repo.createTeam(competitionId, name, code, gender) }
    }

    /** Persist the chosen matchup + livestream, then open observation. */
    fun startGame(teamAId: String, teamBId: String, youtubeUrl: String?, onReady: () -> Unit) {
        viewModelScope.launch {
            val existing = repo.loadGame(competitionId)
            val url = youtubeUrl?.trim()?.ifBlank { null }
            val game = (existing ?: Game(id = newId(), competitionId = competitionId, name = "Game")).copy(
                teamAId = teamAId,
                teamBId = teamBId,
                youtube = YoutubeInfo(enabled = url != null, url = url, videoId = extractYoutubeId(url)),
            )
            repo.saveGame(game)
            _game.value = game
            onReady()
        }
    }

    fun setTeamActive(teamId: String, active: Boolean) {
        viewModelScope.launch { repo.setTeamActive(teamId, active) }
    }

    fun deleteTeam(teamId: String) {
        viewModelScope.launch { repo.deleteTeamPermanently(teamId) }
    }

    fun addPlayer(teamId: String, number: String, name: String) {
        viewModelScope.launch {
            repo.createPlayer(competitionId, teamId, number.ifBlank { null }, name.ifBlank { null })
        }
    }

    fun updatePlayer(player: Player) {
        viewModelScope.launch { repo.updatePlayer(player) }
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
    onOpenPlayer: (String) -> Unit,
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
    val game by vm.game.collectAsStateWithLifecycle()

    var showAddTeam by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showGameSetup by remember { mutableStateOf(false) }
    // Multi-expand: each team the user opens stays open until they tap it again, so several
    // rosters can be viewed at once for an overview (user request).
    var expandedTeamIds by remember { mutableStateOf(emptySet<String>()) }

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
                    "Start Observation",
                    onClick = { showGameSetup = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                SecondaryButton(
                    "Upload Excel template file",
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
                item { EmptyState("No teams yet", "Add a team manually, or upload the Excel template.") }
            }

            items(activeTeams, key = { it.id }) { team ->
                ExpandableTeamCard(
                    team = team,
                    teamPlayers = players
                        .filter { it.teamId == team.id && it.active }
                        .sortedBy { it.uniformNumber?.toIntOrNull() ?: Int.MAX_VALUE },
                    expanded = team.id in expandedTeamIds,
                    onToggle = {
                        expandedTeamIds = if (team.id in expandedTeamIds) {
                            expandedTeamIds - team.id
                        } else {
                            expandedTeamIds + team.id
                        }
                    },
                    onAddPlayer = { number, name -> vm.addPlayer(team.id, number, name) },
                    onArchive = { vm.setTeamActive(team.id, false) },
                    onUpdatePlayer = { vm.updatePlayer(it) },
                    onOpenPlayer = onOpenPlayer,
                )
            }

            if (archivedTeams.isNotEmpty()) {
                item { SectionLabel("Archived", modifier = Modifier.padding(top = AppSpacing.sm)) }
                items(archivedTeams, key = { it.id }) { team ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(AppShapes.card)
                            .background(AppColors.CardCharcoal)
                            .border(1.dp, AppColors.DividerGray, AppShapes.card)
                            .padding(AppSpacing.lg)
                            .alpha(0.6f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(team.displayName(), style = AppTypography.body, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.setTeamActive(team.id, true) }) { Text("Restore", color = AppColors.Gold) }
                    }
                }
            }
        }
    }

    if (showGameSetup) {
        GameSetupDialog(
            teams = activeTeams,
            initialTeamAId = game?.teamAId,
            initialTeamBId = game?.teamBId,
            initialYoutubeUrl = game?.youtube?.url,
            onDismiss = { showGameSetup = false },
            onStart = { aId, bId, url ->
                showGameSetup = false
                vm.startGame(aId, bId, url) { onOpenObservation() }
            },
        )
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
private fun ExpandableTeamCard(
    team: Team,
    teamPlayers: List<Player>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAddPlayer: (number: String, name: String) -> Unit,
    onArchive: () -> Unit,
    onUpdatePlayer: (Player) -> Unit,
    onOpenPlayer: (String) -> Unit,
) {
    var showAddPlayer by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.CardCharcoal)
            .border(1.dp, if (expanded) AppColors.GoldBorder else AppColors.DividerGray, AppShapes.card),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(AppSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(team.displayName(), style = AppTypography.header, color = AppColors.TextPrimary)
                Text("${teamPlayers.size} players", style = AppTypography.microLabel, color = AppColors.TextMuted)
            }
            Text(if (expanded) "▴" else "▾", style = AppTypography.header, color = AppColors.TextSecondary)
        }
        if (expanded) {
            Column(
                Modifier.fillMaxWidth().padding(start = AppSpacing.md, end = AppSpacing.md, bottom = AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = AppSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    Text("Nº", style = AppTypography.microLabel, color = AppColors.TextMuted, modifier = Modifier.weight(1f))
                    Text("FULL NAME", style = AppTypography.microLabel, color = AppColors.TextMuted, modifier = Modifier.weight(3.5f))
                    Text("CLASS", style = AppTypography.microLabel, color = AppColors.TextMuted, modifier = Modifier.weight(1.5f))
                    Text("STATUS", style = AppTypography.microLabel, color = AppColors.TextMuted, modifier = Modifier.weight(1.7f))
                    // Column for the per-player details (magnifying glass) button.
                    Spacer(Modifier.width(44.dp))
                }
                if (teamPlayers.isEmpty()) {
                    Text("No players yet.", style = AppTypography.body, color = AppColors.TextMuted)
                }
                teamPlayers.forEach { player ->
                    PlayerInlineRow(player, onUpdatePlayer, onOpenDetails = { onOpenPlayer(player.id) })
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = AppSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SecondaryButton("Add Player", onClick = { showAddPlayer = true }, modifier = Modifier.weight(1f))
                    TextButton(onClick = onArchive) { Text("Archive", color = AppColors.TextSecondary) }
                }
            }
        }
    }

    if (showAddPlayer) {
        AddPlayerDialog(
            onDismiss = { showAddPlayer = false },
            onAdd = { number, name ->
                onAddPlayer(number, name)
                showAddPlayer = false
            },
        )
    }
}

@Composable
private fun PlayerInlineRow(
    player: Player,
    onUpdate: (Player) -> Unit,
    onOpenDetails: () -> Unit,
) {
    // The athlete's single official class + status (from import or manual entry). My Opinion
    // and Final live only on the Observation screen (user request). The magnifying glass
    // opens the full player record in one tap (user request).
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        InlineField(player.uniformNumber.orEmpty(), { onUpdate(player.copy(uniformNumber = it.ifBlank { null })) }, Modifier.weight(1f), KeyboardType.Number)
        InlineField(player.name.orEmpty(), { onUpdate(player.copy(name = it.ifBlank { null })) }, Modifier.weight(3.5f), KeyboardType.Text)
        ClassDropdownCell(player.startingSportClass, { onUpdate(player.copy(startingSportClass = it)) }, Modifier.weight(1.5f))
        StatusDropdownCell(player.sportClassStatus, { onUpdate(player.copy(sportClassStatus = it)) }, Modifier.weight(1.7f))
        PlayerDetailsButton(onClick = onOpenDetails)
    }
}

/** One-tap magnifying glass that opens the full player record (Edit Player). */
@Composable
private fun PlayerDetailsButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(AppShapes.button)
            .background(AppColors.PanelBlack)
            .border(1.dp, AppColors.DividerGray, AppShapes.button)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = "View player details",
            tint = AppColors.Gold,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun InlineField(value: String, onValueChange: (String) -> Unit, modifier: Modifier, keyboardType: KeyboardType) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(44.dp)
            .clip(AppShapes.button)
            .background(AppColors.PanelBlack)
            .border(1.dp, AppColors.DividerGray, AppShapes.button)
            .padding(horizontal = AppSpacing.sm),
        singleLine = true,
        textStyle = AppTypography.body.copy(color = AppColors.TextPrimary),
        cursorBrush = SolidColor(AppColors.Gold),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { inner -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) { inner() } },
    )
}

@Composable
private fun GameSetupDialog(
    teams: List<Team>,
    initialTeamAId: String?,
    initialTeamBId: String?,
    initialYoutubeUrl: String?,
    onDismiss: () -> Unit,
    onStart: (teamAId: String, teamBId: String, youtubeUrl: String?) -> Unit,
) {
    var teamAId by remember { mutableStateOf(initialTeamAId ?: teams.getOrNull(0)?.id) }
    var teamBId by remember { mutableStateOf(initialTeamBId ?: teams.getOrNull(1)?.id) }
    var youtube by remember { mutableStateOf(initialYoutubeUrl.orEmpty()) }
    val canStart = teamAId != null && teamBId != null && teamAId != teamBId

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.CardCharcoal,
        title = { Text("Set up game", color = AppColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                if (teams.size < 2) {
                    Text(
                        "Add at least two teams (or import a roster) before observing.",
                        style = AppTypography.body,
                        color = AppColors.TextMuted,
                    )
                } else {
                    Text(
                        "Choose the two teams playing this game.",
                        style = AppTypography.body,
                        color = AppColors.TextSecondary,
                    )
                    TeamPicker("Team A", teams, teamAId) { teamAId = it }
                    TeamPicker("Team B", teams, teamBId) { teamBId = it }
                    if (teamAId != null && teamAId == teamBId) {
                        Text("Pick two different teams.", style = AppTypography.microLabel, color = AppColors.AlertRed)
                    }
                    AppTextField(youtube, { youtube = it }, "YouTube livestream link (optional)", placeholder = "https://youtu.be/…")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = canStart, onClick = { onStart(teamAId!!, teamBId!!, youtube.ifBlank { null }) }) {
                Text("Start", color = if (canStart) AppColors.Gold else AppColors.TextMuted)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.TextSecondary) } },
    )
}

@Composable
private fun TeamPicker(label: String, teams: List<Team>, selectedId: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = teams.firstOrNull { it.id == selectedId }?.name ?: "Select team"
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        SectionLabel(label)
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.button)
                    .border(1.dp, AppColors.DividerGray, AppShapes.button)
                    .clickable { expanded = true }
                    .padding(AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(selectedName, style = AppTypography.body, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                Text("▾", style = AppTypography.body, color = AppColors.TextSecondary)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                teams.forEach { t ->
                    DropdownMenuItem(text = { Text(t.name) }, onClick = { onSelect(t.id); expanded = false })
                }
            }
        }
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
private fun AddPlayerDialog(
    onDismiss: () -> Unit,
    onAdd: (number: String, name: String) -> Unit,
) {
    var number by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.CardCharcoal,
        title = { Text("Add Player", color = AppColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                AppTextField(number, { number = it }, "Uniform number", keyboardType = KeyboardType.Number)
                AppTextField(name, { name = it }, "Player name (optional)")
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(number, name) }, enabled = number.isNotBlank() || name.isNotBlank()) {
                Text("Add", color = if (number.isNotBlank() || name.isNotBlank()) AppColors.Gold else AppColors.TextMuted)
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
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.CardCharcoal,
        title = { Text("Edit Competition", color = AppColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                AppTextField(name, { name = it }, "Competition name")
                Spacer(Modifier.width(AppSpacing.sm))
                DestructiveButton("Delete Competition", onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(competition.copy(name = name.ifBlank { competition.name })) },
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
