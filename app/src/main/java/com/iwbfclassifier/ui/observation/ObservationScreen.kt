package com.iwbfclassifier.ui.observation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iwbfclassifier.core.buildYoutubeUrl
import com.iwbfclassifier.core.extractYoutubeId
import com.iwbfclassifier.core.formatSeconds
import com.iwbfclassifier.core.newId
import com.iwbfclassifier.core.nowIso
import com.iwbfclassifier.data.model.Game
import com.iwbfclassifier.data.model.InkStroke
import com.iwbfclassifier.data.model.NotePage
import com.iwbfclassifier.data.model.Player
import com.iwbfclassifier.data.model.SportClass
import com.iwbfclassifier.data.model.SportClassStatus
import com.iwbfclassifier.data.model.Team
import com.iwbfclassifier.data.model.VideoEvidence
import com.iwbfclassifier.data.model.displayJersey
import com.iwbfclassifier.data.model.displayName
import com.iwbfclassifier.data.model.teamComparator
import com.iwbfclassifier.data.repository.CompetitionRepository
import com.iwbfclassifier.ui.LocalAppContainer
import com.iwbfclassifier.ui.components.CaptureMomentDialog
import com.iwbfclassifier.ui.components.ClassStatusRow
import com.iwbfclassifier.ui.components.EmptyState
import com.iwbfclassifier.ui.components.InkEditor
import com.iwbfclassifier.ui.components.KeyMomentsDialog
import com.iwbfclassifier.ui.components.NoteCanvasPanel
import com.iwbfclassifier.ui.components.ObservationTopBar
import com.iwbfclassifier.ui.components.PlayerChip
import com.iwbfclassifier.ui.components.PrimaryButton
import com.iwbfclassifier.ui.components.SecondaryButton
import com.iwbfclassifier.ui.components.SectionLabel
import com.iwbfclassifier.ui.components.YouTubePlayerController
import com.iwbfclassifier.ui.components.YouTubePlayerPanel
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppShapes
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
        .map { list -> list.filter { it.competitionId == competitionId && it.active }.sortedWith(teamComparator) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.teams.value.filter { it.competitionId == competitionId && it.active }.sortedWith(teamComparator))

    val players = repo.players
        .map { list -> list.filter { it.competitionId == competitionId && it.active } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.players.value.filter { it.competitionId == competitionId && it.active })

    private val _game = MutableStateFlow<Game?>(null)
    val game = _game.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()

    init {
        viewModelScope.launch { _game.value = repo.loadGame(competitionId) }
    }

    fun save(updated: Player) {
        viewModelScope.launch {
            _saving.value = true
            repo.updatePlayer(updated)
            _saving.value = false
        }
    }

    suspend fun loadNotes(playerId: String): NotePage = repo.loadNotePage(competitionId, playerId)

    fun saveNotes(playerId: String, strokes: List<InkStroke>, aspectRatio: Float?) {
        viewModelScope.launch {
            _saving.value = true
            repo.saveNotePage(competitionId, NotePage(playerId, strokes, nowIso(), aspectRatio))
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
    val game by vm.game.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()

    var selectedPlayerId by remember { mutableStateOf<String?>(null) }
    var showMoments by remember { mutableStateOf(false) }
    // Shown when "Add Moment" can't read the embedded player's time (e.g. the stream is
    // embed-blocked and playing in the YouTube app) — the user types the time instead.
    var showMomentTime by remember { mutableStateOf(false) }

    val selectedPlayer = players.firstOrNull { it.id == selectedPlayerId }
    // Teams come from the chosen game; fall back to the first two active teams.
    val teamA = game?.teamAId?.let { id -> teams.firstOrNull { it.id == id } } ?: teams.getOrNull(0)
    val teamB = game?.teamBId?.let { id -> teams.firstOrNull { it.id == id } } ?: teams.getOrNull(1)

    // Embedded YouTube player + the game's livestream video id.
    val controller = remember { YouTubePlayerController() }
    val videoId = game?.youtube?.videoId ?: extractYoutubeId(game?.youtube?.url)
    LaunchedEffect(videoId) { if (videoId != null) controller.load(videoId) }

    // Resizable video panel height (drag the divider or use − / +).
    val density = LocalDensity.current
    var videoHeight by remember { mutableStateOf(260.dp) }
    fun changeHeight(deltaPx: Float) {
        videoHeight = with(density) { (videoHeight.toPx() + deltaPx).toDp() }.coerceIn(140.dp, 700.dp)
    }

    // Handwritten notes, loaded per player and autosaved. The editor owns the strokes plus a
    // snapshot undo/redo history (pen, eraser AND Clear are all undoable) — recreated per
    // player so undo never reaches across athletes.
    val editor = remember(selectedPlayerId) { InkEditor() }
    // Latest note-canvas shape (width / height); saved with the notes so they re-render
    // faithfully on the Edit Player screen even though that canvas has a different size.
    var canvasAspect by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(selectedPlayerId) {
        val pid = selectedPlayerId
        editor.reset(if (pid != null) vm.loadNotes(pid).strokes else emptyList())
    }
    LaunchedEffect(selectedPlayerId, editor.strokes, editor.dirty) {
        if (!editor.dirty) return@LaunchedEffect
        val pid = selectedPlayerId ?: return@LaunchedEffect
        delay(400)
        vm.saveNotes(pid, editor.strokes, canvasAspect)
        editor.markSaved()
    }

    fun selectPlayer(id: String) {
        val current = selectedPlayerId
        if (current != null && current != id && editor.dirty) vm.saveNotes(current, editor.strokes, canvasAspect)
        selectedPlayerId = id
    }

    // Store a -5s/+5s, 0.5x slow-motion window centered on [center] for the selected player.
    fun createMoment(center: Int, label: String? = null) {
        val player = selectedPlayer ?: return
        val vid = videoId ?: return
        val start = (center - 5).coerceAtLeast(0)
        val original = game?.youtube?.url ?: "https://youtu.be/$vid"
        vm.addVideo(
            player,
            VideoEvidence(
                id = newId(),
                url = buildYoutubeUrl(vid, original, center),
                videoId = vid,
                startSeconds = start,
                endSeconds = center + 5,
                playbackRate = 0.5,
                label = label?.ifBlank { null } ?: "Moment @ ${formatSeconds(center) ?: "${center}s"}",
                createdAt = nowIso(),
            ),
        )
    }
    // One tap captures the embedded player's current second. If that's 0 (the stream is
    // playing outside the app, so we can't read its time), fall back to manual entry
    // instead of silently saving a useless 0–5s clip.
    val onAddMoment: () -> Unit = {
        val t = controller.currentSeconds().toInt()
        if (t > 0) createMoment(t) else showMomentTime = true
    }
    // Replay seeks the embedded player and plays the saved window. The moments dialog is
    // dismissed first so the video (behind it) is actually visible during playback.
    val onReplay: (VideoEvidence) -> Unit = { ev ->
        showMoments = false
        controller.playWindow(
            startSeconds = ev.startSeconds ?: 0,
            endSeconds = ev.endSeconds,
            rate = if (ev.playbackRate > 0.0) ev.playbackRate else 0.5,
        )
    }

    val portrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    Column(Modifier.fillMaxSize().background(AppColors.InkBlack)) {
        ObservationTopBar(
            competitionName = competition?.name ?: "Observation",
            gameName = teamA?.displayName()?.let { a -> teamB?.displayName()?.let { b -> "$a  vs  $b" } },
            saving = saving,
            onBack = onBack,
        )

        if (videoId != null) {
            ObservationVideoBand(
                controller = controller,
                videoHeight = videoHeight,
                onResize = { delta -> changeHeight(delta) },
                onAddMoment = onAddMoment,
                canAddMoment = selectedPlayer != null,
                momentCount = selectedPlayer?.videoEvidence?.size ?: 0,
                onOpenMoments = { showMoments = true },
            )
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (portrait) {
                Column(Modifier.fillMaxSize()) {
                    ObservationWorkArea(
                        player = selectedPlayer,
                        onUpdate = { updated -> vm.save(updated) },
                        onOpenPlayer = onOpenPlayer,
                        editor = editor,
                        onOpenMoments = { showMoments = true },
                        momentCount = selectedPlayer?.videoEvidence?.size ?: 0,
                        showMomentsButton = videoId == null,
                        onCanvasAspectRatio = { canvasAspect = it },
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
                        onUpdate = { updated -> vm.save(updated) },
                        onOpenPlayer = onOpenPlayer,
                        editor = editor,
                        onOpenMoments = { showMoments = true },
                        momentCount = selectedPlayer?.videoEvidence?.size ?: 0,
                        showMomentsButton = videoId == null,
                        onCanvasAspectRatio = { canvasAspect = it },
                        modifier = Modifier.weight(0.52f).fillMaxHeight(),
                    )
                    PlayerRail(teamB, players, selectedPlayerId, ::selectPlayer, Modifier.weight(0.24f).fillMaxHeight())
                }
            }
        }
    }

    if (showMoments && selectedPlayer != null) {
        val player = selectedPlayer
        KeyMomentsDialog(
            evidence = player.videoEvidence,
            onReplay = if (videoId != null) onReplay else null,
            onRemove = { ev -> vm.removeVideo(player, ev) },
            onAdd = { ev -> vm.addVideo(player, ev) },
            onDismiss = { showMoments = false },
        )
    }

    if (showMomentTime && selectedPlayer != null && videoId != null) {
        CaptureMomentDialog(
            initialSeconds = controller.currentSeconds().toInt(),
            onConfirm = { center, label -> createMoment(center, label); showMomentTime = false },
            onDismiss = { showMomentTime = false },
        )
    }
}

@Composable
private fun ObservationVideoBand(
    controller: YouTubePlayerController,
    videoHeight: Dp,
    onResize: (Float) -> Unit,
    onAddMoment: () -> Unit,
    canAddMoment: Boolean,
    momentCount: Int,
    onOpenMoments: () -> Unit,
) {
    val density = LocalDensity.current
    val stepPx = with(density) { 72.dp.toPx() }
    Column(Modifier.fillMaxWidth().background(AppColors.PanelBlack)) {
        YouTubePlayerPanel(
            controller = controller,
            modifier = Modifier.fillMaxWidth().height(videoHeight),
        )
        // Drag this divider (or use − / +) to resize the video so it fills its area.
        Box(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .background(AppColors.PanelBlack)
                .pointerInput(Unit) { detectVerticalDragGestures { _, delta -> onResize(delta) } },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(width = 64.dp, height = 5.dp).clip(AppShapes.button).background(AppColors.Gold))
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            PrimaryButton(
                text = if (canAddMoment) "＋ Add Moment" else "Select a player",
                onClick = onAddMoment,
                enabled = canAddMoment,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton("Moments ($momentCount)", onClick = onOpenMoments)
            SizeStepButton("−") { onResize(-stepPx) }
            SizeStepButton("+") { onResize(stepPx) }
        }
    }
}

@Composable
private fun SizeStepButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(AppShapes.button)
            .background(AppColors.CardCharcoal)
            .border(1.dp, AppColors.DividerGray, AppShapes.button)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = AppTypography.header, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ObservationWorkArea(
    player: Player?,
    onUpdate: (Player) -> Unit,
    onOpenPlayer: (String) -> Unit,
    editor: InkEditor,
    onOpenMoments: () -> Unit,
    momentCount: Int,
    showMomentsButton: Boolean,
    onCanvasAspectRatio: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        if (player == null) {
            EmptyState("Select a player", "Tap a player chip to start observing.")
        } else {
            PlayerHeader(
                player = player,
                onEditDetails = { onOpenPlayer(player.id) },
                showMoments = showMomentsButton,
                momentCount = momentCount,
                onOpenMoments = onOpenMoments,
            )
            ClassDecisionTable(player = player, onUpdate = onUpdate)
            NoteCanvasPanel(
                editor = editor,
                modifier = Modifier.fillMaxWidth().weight(1f),
                onCanvasAspectRatio = onCanvasAspectRatio,
            )
        }
    }
}

/**
 * The three decision lines on one screen (user request): Initial, My Opinion, Final —
 * each showing its Sport Class and Sport Class Status as dropdowns so everything is
 * visible and editable at a glance. "Initial" edits the athlete's single official
 * class + status (the same value shown on the roster preview and Edit Player); My Opinion
 * and Final are observation-only working values.
 */
@Composable
private fun ClassDecisionTable(player: Player, onUpdate: (Player) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Box(Modifier.weight(1.2f))
            SectionLabel("Class", Modifier.weight(1f))
            SectionLabel("Status", Modifier.weight(1.6f))
        }
        ClassStatusRow(
            label = "Initial",
            sportClass = player.startingSportClass,
            onSportClass = { onUpdate(player.copy(startingSportClass = it)) },
            status = player.sportClassStatus,
            onStatus = { onUpdate(player.copy(sportClassStatus = it)) },
        )
        ClassStatusRow(
            label = "My Opinion",
            sportClass = player.myOpinionSportClass,
            onSportClass = { onUpdate(player.copy(myOpinionSportClass = it)) },
            status = player.myOpinionSportClassStatus,
            onStatus = { onUpdate(player.copy(myOpinionSportClassStatus = it)) },
        )
        ClassStatusRow(
            label = "Final",
            sportClass = player.finalSportClass,
            onSportClass = { onUpdate(player.copy(finalSportClass = it)) },
            status = player.finalSportClassStatus,
            onStatus = { onUpdate(player.copy(finalSportClassStatus = it)) },
        )
    }
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
        SectionLabel(team?.displayName() ?: "—", modifier = Modifier.padding(AppSpacing.sm))
        val teamPlayers = players
            .filter { it.teamId == team?.id }
            .sortedBy { it.uniformNumber?.toIntOrNull() ?: Int.MAX_VALUE }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = AppSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            items(teamPlayers, key = { it.id }) { player ->
                // Chip shows number + full name + the athlete's single official class + status.
                val current = player.startingSportClass ?: player.importedSportClass
                val classText = listOfNotNull(
                    current?.let { "Class ${it.code}" },
                    player.sportClassStatus?.code,
                ).joinToString(" · ").ifBlank { null }
                PlayerChip(
                    number = player.uniformNumber,
                    name = player.name,
                    classText = classText,
                    status = player.observationStatus,
                    selected = player.id == selectedPlayerId,
                    onClick = { onSelect(player.id) },
                )
            }
        }
    }
}

@Composable
private fun PlayerHeader(
    player: Player,
    onEditDetails: () -> Unit,
    showMoments: Boolean,
    momentCount: Int,
    onOpenMoments: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                listOfNotNull(displayJersey(player.uniformNumber)?.let { "#$it" }, (player.name ?: "Unknown")).joinToString(" "),
                style = AppTypography.header,
                color = AppColors.TextPrimary,
            )
        }
        if (showMoments) SecondaryButton("Moments ($momentCount)", onClick = onOpenMoments)
        SecondaryButton("Edit details", onClick = onEditDetails)
    }
}
