package com.iwbfclassifier.ui.roster

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iwbfclassifier.core.extractYoutubeId
import com.iwbfclassifier.core.formatSeconds
import com.iwbfclassifier.core.nowIso
import com.iwbfclassifier.data.model.InkStroke
import com.iwbfclassifier.data.model.NotePage
import com.iwbfclassifier.data.model.ObservationStatus
import com.iwbfclassifier.data.model.Player
import com.iwbfclassifier.data.model.SportClass
import com.iwbfclassifier.data.model.SportClassStatus
import com.iwbfclassifier.data.model.VideoEvidence
import com.iwbfclassifier.data.repository.CompetitionRepository
import com.iwbfclassifier.ui.LocalAppContainer
import com.iwbfclassifier.ui.components.AppTextField
import com.iwbfclassifier.ui.components.AppTopBar
import com.iwbfclassifier.ui.components.ClassStatusRow
import com.iwbfclassifier.ui.components.ConfirmDialog
import com.iwbfclassifier.ui.components.DestructiveButton
import com.iwbfclassifier.ui.components.EmptyState
import com.iwbfclassifier.ui.components.InkEditor
import com.iwbfclassifier.ui.components.NoteCanvasPanel
import com.iwbfclassifier.ui.components.SaveIndicator
import com.iwbfclassifier.ui.components.SecondaryButton
import com.iwbfclassifier.ui.components.SectionLabel
import com.iwbfclassifier.ui.components.UniformNumberField
import com.iwbfclassifier.ui.components.VideoEvidenceSection
import com.iwbfclassifier.ui.components.YouTubePlayerController
import com.iwbfclassifier.ui.components.YouTubePlayerPanel
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppShapes
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerEditViewModel(
    private val repo: CompetitionRepository,
    private val playerId: String,
) : ViewModel() {

    val player = repo.players
        .map { list -> list.firstOrNull { it.id == playerId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.players.value.firstOrNull { it.id == playerId })

    val teamName = combine(repo.teams, player) { teams, current ->
        teams.firstOrNull { it.id == current?.teamId }?.name
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()

    // Handwritten notes for this player, loaded on demand. null = still loading; the
    // strokes are written on the Observation screen and shown read-only here so every
    // pen note appears in the player record (user request). The page carries the canvas
    // aspect ratio so the notes re-render faithfully (no stretching).
    private val _notePage = MutableStateFlow<NotePage?>(null)
    val notePage = _notePage.asStateFlow()

    init {
        viewModelScope.launch {
            val current = player.filterNotNull().first()
            _notePage.value = repo.loadNotePage(current.competitionId, current.id)
        }
    }

    /** Autosave: persist immediately on every change (docs/03 — autosave everything). */
    fun save(updated: Player) {
        viewModelScope.launch {
            _saving.value = true
            repo.updatePlayer(updated)
            _saving.value = false
        }
    }

    /**
     * Persist edited handwritten notes to the SAME per-player note file the Observation
     * screen reads, so notes written here show up the next time this athlete is observed —
     * and vice versa. Scoped to this competition + player, so other competitions are never
     * affected (user request: each competition is unique).
     */
    fun saveNotes(strokes: List<InkStroke>, aspectRatio: Float?) {
        val current = player.value ?: return
        viewModelScope.launch {
            _saving.value = true
            repo.saveNotePage(current.competitionId, NotePage(current.id, strokes, nowIso(), aspectRatio))
            _saving.value = false
        }
    }

    fun setActive(active: Boolean) {
        viewModelScope.launch { repo.setPlayerActive(playerId, active) }
    }

    fun deletePermanently(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deletePlayerPermanently(playerId)
            onDone()
        }
    }
}

@Composable
fun PlayerEditScreen(playerId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: PlayerEditViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PlayerEditViewModel(container.competitionRepository, playerId) }
        },
    )
    val playerState by vm.player.collectAsStateWithLifecycle()
    val teamName by vm.teamName.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val notePage by vm.notePage.collectAsStateWithLifecycle()

    val loaded = playerState
    if (loaded == null) {
        Column(Modifier.fillMaxSize().background(AppColors.InkBlack)) {
            AppTopBar(title = "Edit Player", onBack = onBack)
            EmptyState("Loading…")
        }
        return
    }

    // Local editable draft, seeded once per player id.
    var draft by remember(loaded.id) { mutableStateOf(loaded) }
    var confirmDelete by remember(loaded.id) { mutableStateOf(false) }

    // Embedded player for replaying a moment in-app (user request: stay in the app, open only
    // when the replay link is tapped). Each moment is its own video, so we load+play it directly.
    val replayController = remember(loaded.id) { YouTubePlayerController() }
    var replayTarget by remember(loaded.id) { mutableStateOf<VideoEvidence?>(null) }
    LaunchedEffect(replayTarget?.id) {
        val ev = replayTarget ?: return@LaunchedEffect
        replayController.loadAndPlay(
            videoId = ev.videoId ?: extractYoutubeId(ev.url),
            startSeconds = ev.startSeconds ?: 0,
            endSeconds = ev.endSeconds,
            rate = if (ev.playbackRate > 0.0) ev.playbackRate else 0.5,
        )
    }

    fun edit(transform: (Player) -> Player) {
        draft = transform(draft)
        vm.save(draft)
    }

    // Editable handwritten notes — the SAME per-player file the Observation screen uses, so
    // anything written here shows up the next time this athlete is observed (user request).
    // The editor owns strokes + a snapshot undo/redo history (pen, eraser AND Clear are all
    // undoable). `seeded` gates the UI until the saved page has loaded.
    val editor = remember(loaded.id) { InkEditor() }
    var seeded by remember(loaded.id) { mutableStateOf(false) }

    // Draw at the ratio the notes were saved with so they stay faithful (no stretching);
    // a sensible default for brand-new notes.
    val displayRatio = (notePage?.aspectRatio)?.takeIf { it > 0f } ?: 1.5f

    // Seed the editor once the saved page has loaded, as the clean (non-dirty) baseline.
    LaunchedEffect(loaded.id, notePage) {
        if (!seeded) notePage?.let { editor.reset(it.strokes); seeded = true }
    }
    // Debounced autosave (docs/03 — autosave everything).
    LaunchedEffect(editor.strokes, editor.dirty) {
        if (!editor.dirty) return@LaunchedEffect
        delay(400)
        vm.saveNotes(editor.strokes, displayRatio)
        editor.markSaved()
    }

    Column(Modifier.fillMaxSize().background(AppColors.InkBlack)) {
        AppTopBar(
            title = "Edit Player",
            subtitle = listOfNotNull(teamName, draft.name).joinToString(" · ").ifBlank { null },
            onBack = onBack,
            actions = {
                SaveIndicator(saving)
                Spacer(Modifier.width(AppSpacing.md))
            },
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
        ) {
            // Identity
            SectionLabel("Player Identity")
            UniformNumberField(
                value = draft.uniformNumber,
                onValueChange = { v -> edit { it.copy(uniformNumber = v) } },
                modifier = Modifier.fillMaxWidth(),
            )
            AppTextField(draft.name.orEmpty(), { v -> edit { it.copy(name = v.ifBlank { null }) } }, "Player Name")

            // The full classification decision — Initial (the athlete's single official class
            // + status), My Opinion and Final — shown exactly like the Observation screen so
            // these values are visible/editable outside a game too (user request).
            SectionLabel("Sport Class & Status")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                Box(Modifier.weight(1.2f))
                SectionLabel("Class", Modifier.weight(1f))
                SectionLabel("Status", Modifier.weight(1.6f))
            }
            ClassStatusRow(
                label = "Initial",
                sportClass = draft.startingSportClass,
                onSportClass = { sc -> edit { it.copy(startingSportClass = sc) } },
                status = draft.sportClassStatus,
                onStatus = { st -> edit { it.copy(sportClassStatus = st) } },
            )
            ClassStatusRow(
                label = "My Opinion",
                sportClass = draft.myOpinionSportClass,
                onSportClass = { sc -> edit { it.copy(myOpinionSportClass = sc) } },
                status = draft.myOpinionSportClassStatus,
                onStatus = { st -> edit { it.copy(myOpinionSportClassStatus = st) } },
            )
            ClassStatusRow(
                label = "Final",
                sportClass = draft.finalSportClass,
                onSportClass = { sc -> edit { it.copy(finalSportClass = sc) } },
                status = draft.finalSportClassStatus,
                onStatus = { st -> edit { it.copy(finalSportClassStatus = st) } },
            )

            // Editable handwritten notes — same per-player notes shown during a game. What
            // you write here is saved to the athlete's record and appears the next time they
            // are observed, and vice versa (user request). S Pen writes; finger navigates.
            SectionLabel("Handwritten Notes")
            if (!seeded) {
                EmptyState("Loading notes…")
            } else {
                NoteCanvasPanel(
                    editor = editor,
                    modifier = Modifier.fillMaxWidth(),
                    noteAspectRatio = displayRatio,
                )
            }

            // Observation status
            ObservationStatusField(draft.observationStatus) { status -> edit { it.copy(observationStatus = status) } }

            // Video evidence (YouTube links + timestamps). Tapping Replay opens the embedded
            // player inline (below) instead of leaving the app for YouTube (user request); the
            // "Open" button still launches YouTube on demand.
            replayTarget?.let { ev ->
                MomentReplayPanel(
                    controller = replayController,
                    evidence = ev,
                    onClose = { replayTarget = null },
                )
            }
            VideoEvidenceSection(
                evidence = draft.videoEvidence,
                onAdd = { ev -> edit { it.copy(videoEvidence = it.videoEvidence + ev) } },
                onRemove = { ev ->
                    if (replayTarget?.id == ev.id) replayTarget = null
                    edit { it.copy(videoEvidence = it.videoEvidence.filterNot { x -> x.id == ev.id }) }
                },
                onReplay = { ev -> replayTarget = ev },
            )

            // Management (reversible remove + permanent delete)
            SectionLabel("Manage")
            if (draft.active) {
                SecondaryButton("Archive Player", onClick = { vm.setActive(false); draft = draft.copy(active = false) }, modifier = Modifier.fillMaxWidth())
            } else {
                SecondaryButton("Restore Player", onClick = { vm.setActive(true); draft = draft.copy(active = true) }, modifier = Modifier.fillMaxWidth())
            }
            DestructiveButton("Delete Permanently", onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(AppSpacing.xl))
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete player permanently?",
            message = "This removes the player and any notes for good. Use Archive instead if you might restore them.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = { confirmDelete = false; vm.deletePermanently(onDone = onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
}

/**
 * In-app replay of a moment, shown inline on the player record only while a moment is selected
 * (user request: stay in the app, open the player only when the replay link is tapped). Reuses
 * the same embedded player as the observation screen; the [controller] is driven to load this
 * moment's own video and play its slow-motion window.
 */
@Composable
private fun MomentReplayPanel(
    controller: YouTubePlayerController,
    evidence: VideoEvidence,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.PanelBlack)
            .border(1.dp, AppColors.DividerGray, AppShapes.card)
            .padding(AppSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    evidence.label?.takeIf { it.isNotBlank() } ?: "YouTube moment",
                    style = AppTypography.body,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val window = formatSeconds(evidence.startSeconds)?.let { s ->
                    formatSeconds(evidence.endSeconds)?.let { e -> "$s–$e" } ?: "@ $s"
                }
                val rate = if (evidence.playbackRate != 1.0) "${evidence.playbackRate}x" else null
                val sub = listOfNotNull(window, rate).joinToString("  ·  ")
                if (sub.isNotBlank()) {
                    Text(sub, style = AppTypography.microLabel, color = AppColors.Gold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            TextButton(onClick = onClose) { Text("Close", color = AppColors.Gold) }
        }
        YouTubePlayerPanel(
            controller = controller,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(AppShapes.card),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ObservationStatusField(value: ObservationStatus, onSelect: (ObservationStatus) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        SectionLabel("Observation Status")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            ObservationStatus.selectable.forEach { status ->
                SelectableChip(
                    text = status.label,
                    selected = status == value,
                    onClick = { onSelect(status) },
                )
            }
        }
    }
}

@Composable
private fun SelectableChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .widthIn(min = 52.dp)
            .clip(AppShapes.chip)
            .background(if (selected) AppColors.Gold else AppColors.CardCharcoal)
            .border(1.dp, if (selected) AppColors.Gold else AppColors.DividerGray, AppShapes.chip)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppTypography.chip, color = if (selected) AppColors.InkBlack else AppColors.TextPrimary)
    }
}
