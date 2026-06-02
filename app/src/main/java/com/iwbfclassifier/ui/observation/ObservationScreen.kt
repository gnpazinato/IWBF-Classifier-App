package com.iwbfclassifier.ui.observation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iwbfclassifier.core.nowIso
import com.iwbfclassifier.data.model.InkStroke
import com.iwbfclassifier.data.model.NotePage
import com.iwbfclassifier.data.model.Player
import com.iwbfclassifier.data.model.SportClass
import com.iwbfclassifier.data.model.Team
import com.iwbfclassifier.data.model.VideoEvidence
import com.iwbfclassifier.data.repository.CompetitionRepository
import com.iwbfclassifier.ui.LocalAppContainer
import com.iwbfclassifier.ui.components.ClassSelector
import com.iwbfclassifier.ui.components.ClassTarget
import com.iwbfclassifier.ui.components.EmptyState
import com.iwbfclassifier.ui.components.NoteCanvasPanel
import com.iwbfclassifier.ui.components.ObservationTopBar
import com.iwbfclassifier.ui.components.PlayerChip
import com.iwbfclassifier.ui.components.SecondaryButton
import com.iwbfclassifier.ui.components.SectionLabel
import com.iwbfclassifier.ui.components.VideoEvidenceSection
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography
import kotlinx.coroutines.delay
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

    suspend fun loadNotes(playerId: String): NotePage = repo.loadNotePage(competitionId, playerId)

    fun saveNotes(playerId: String, strokes: List<InkStroke>) {
        viewModelScope.launch {
            _saving.value = true
            repo.saveNotePage(competitionId, NotePage(playerId, strokes, nowIso()))
            _saving.value = false
        }
    }

    fun addVideo(player: Player, ev: VideoEvidence) =
        save(player.copy(videoEvidence = player.videoEvidence + ev))

    fun removeVideo(player: Player, ev: VideoEvidence) =
        save(player.copy(videoEvidence = player.videoEvidence.filterNot { it.id == ev.id }))
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

    // Handwritten notes, loaded per player and autosaved.
    var strokes by remember(selectedPlayerId) { mutableStateOf(emptyList<InkStroke>()) }
    var undone by remember(selectedPlayerId) { mutableStateOf(emptyList<InkStroke>()) }
    var notesDirty by remember(selectedPlayerId) { mutableStateOf(false) }

    LaunchedEffect(selectedPlayerId) {
        val pid = selectedPlayerId
        strokes = if (pid != null) vm.loadNotes(pid).strokes else emptyList()
        undone = emptyList()
        notesDirty = false
    }
    LaunchedEffect(selectedPlayerId, strokes, notesDirty) {
        if (!notesDirty) return@LaunchedEffect
        val pid = selectedPlayerId ?: return@LaunchedEffect
        delay(400)
        vm.saveNotes(pid, strokes)
    }

    fun selectPlayer(id: String) {
        val current = selectedPlayerId
        if (current != null && current != id && notesDirty) vm.saveNotes(current, strokes)
        selectedPlayerId = id
    }

    val onAddStroke: (InkStroke) -> Unit = { s -> strokes = strokes + s; undone = emptyList(); notesDirty = true }
    val onErase: (Set<Int>) -> Unit = { idxs -> strokes = strokes.filterIndexed { i, _ -> i !in idxs }; undone = emptyList(); notesDirty = true }
    val onUndo: () -> Unit = { if (strokes.isNotEmpty()) { undone = undone + strokes.last(); strokes = strokes.dropLast(1); notesDirty = true } }
    val onRedo: () -> Unit = { if (undone.isNotEmpty()) { strokes = strokes + undone.last(); undone = undone.dropLast(1); notesDirty = true } }
    val onClear: () -> Unit = { if (strokes.isNotEmpty()) { undone = emptyList(); strokes = emptyList(); notesDirty = true } }

    val portrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    Column(Modifier.fillMaxSize().background(AppColors.InkBlack)) {
        ObservationTopBar(
            competitionName = competition?.name ?: "Observation",
            gameName = null,
            saving = saving,
            onBack = onBack,
        )

        if (portrait) {
            Column(Modifier.fillMaxSize()) {
                ObservationWorkArea(
                    player = selectedPlayer,
                    target = target,
                    onTargetChange = { target = it },
                    onSelectClass = { sc -> selectedPlayer?.let { vm.save(applyClass(it, target, sc)) } },
                    onOpenPlayer = onOpenPlayer,
                    strokes = strokes,
                    undone = undone,
                    onAddStroke = onAddStroke,
                    onErase = onErase,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onClear = onClear,
                    onAddVideo = { ev -> selectedPlayer?.let { vm.addVideo(it, ev) } },
                    onRemoveVideo = { ev -> selectedPlayer?.let { vm.removeVideo(it, ev) } },
                    modifier = Modifier.fillMaxWidth().weight(0.6f),
                )
                Row(Modifier.fillMaxWidth().weight(0.4f)) {
                    PlayerRail(teamA, players, selectedPlayerId, ::selectPlayer, Modifier.weight(1f).fillMaxHeight())
                    PlayerRail(teamB, players, selectedPlayerId, ::selectPlayer, Modifier.weight(1f).fillMaxHeight())
                }
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                PlayerRail(teamA, players, selectedPlayerId, ::selectPlayer, Modifier.weight(0.24f).fillMaxHeight())
                ObservationWorkArea(
                    player = selectedPlayer,
                    target = target,
                    onTargetChange = { target = it },
                    onSelectClass = { sc -> selectedPlayer?.let { vm.save(applyClass(it, target, sc)) } },
                    onOpenPlayer = onOpenPlayer,
                    strokes = strokes,
                    undone = undone,
                    onAddStroke = onAddStroke,
                    onErase = onErase,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onClear = onClear,
                    onAddVideo = { ev -> selectedPlayer?.let { vm.addVideo(it, ev) } },
                    onRemoveVideo = { ev -> selectedPlayer?.let { vm.removeVideo(it, ev) } },
                    modifier = Modifier.weight(0.52f).fillMaxHeight(),
                )
                PlayerRail(teamB, players, selectedPlayerId, ::selectPlayer, Modifier.weight(0.24f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun ObservationWorkArea(
    player: Player?,
    target: ClassTarget,
    onTargetChange: (ClassTarget) -> Unit,
    onSelectClass: (SportClass) -> Unit,
    onOpenPlayer: (String) -> Unit,
    strokes: List<InkStroke>,
    undone: List<InkStroke>,
    onAddStroke: (InkStroke) -> Unit,
    onErase: (Set<Int>) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onAddVideo: (VideoEvidence) -> Unit,
    onRemoveVideo: (VideoEvidence) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        if (player == null) {
            EmptyState("Select a player", "Tap a player chip to start observing.")
        } else {
            PlayerHeader(player = player, onEditDetails = { onOpenPlayer(player.id) })
            ClassSelector(
                target = target,
                onTargetChange = onTargetChange,
                valueForTarget = when (target) {
                    ClassTarget.Starting -> player.startingSportClass
                    ClassTarget.MyOpinion -> player.myOpinionSportClass
                    ClassTarget.Final -> player.finalSportClass
                },
                onSelectClass = onSelectClass,
                modifier = Modifier.fillMaxWidth(),
            )
            VideoEvidenceSection(
                evidence = player.videoEvidence,
                onAdd = onAddVideo,
                onRemove = onRemoveVideo,
                compact = true,
            )
            NoteCanvasPanel(
                strokes = strokes,
                onAddStroke = onAddStroke,
                onEraseStrokes = onErase,
                onUndo = onUndo,
                onRedo = onRedo,
                onClear = onClear,
                canUndo = strokes.isNotEmpty(),
                canRedo = undone.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().weight(1f),
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
                // Rails show number + name only; the class shows in the header above (user request).
                PlayerChip(
                    number = player.uniformNumber,
                    name = player.name,
                    classText = null,
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
