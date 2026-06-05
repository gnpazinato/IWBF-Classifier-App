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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.iwbfclassifier.core.nowIso
import com.iwbfclassifier.data.model.InkStroke
import com.iwbfclassifier.data.model.NotePage
import com.iwbfclassifier.data.model.ObservationStatus
import com.iwbfclassifier.data.model.Player
import com.iwbfclassifier.data.model.SportClass
import com.iwbfclassifier.data.model.SportClassStatus
import com.iwbfclassifier.data.repository.CompetitionRepository
import com.iwbfclassifier.ui.LocalAppContainer
import com.iwbfclassifier.ui.components.AppTextField
import com.iwbfclassifier.ui.components.AppTopBar
import com.iwbfclassifier.ui.components.ClassStatusRow
import com.iwbfclassifier.ui.components.ConfirmDialog
import com.iwbfclassifier.ui.components.DestructiveButton
import com.iwbfclassifier.ui.components.EmptyState
import com.iwbfclassifier.ui.components.NoteCanvasPanel
import com.iwbfclassifier.ui.components.SaveIndicator
import com.iwbfclassifier.ui.components.SecondaryButton
import com.iwbfclassifier.ui.components.SectionLabel
import com.iwbfclassifier.ui.components.VideoEvidenceSection
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

    fun edit(transform: (Player) -> Player) {
        draft = transform(draft)
        vm.save(draft)
    }

    // Editable handwritten notes — the SAME per-player file the Observation screen uses, so
    // anything written here shows up the next time this athlete is observed (user request).
    // null = still loading.
    var strokes by remember(loaded.id) { mutableStateOf<List<InkStroke>?>(null) }
    var undone by remember(loaded.id) { mutableStateOf(emptyList<InkStroke>()) }
    var notesDirty by remember(loaded.id) { mutableStateOf(false) }

    // Seed the editable strokes once the saved page has loaded (without marking dirty).
    LaunchedEffect(notePage) {
        if (strokes == null) notePage?.let { strokes = it.strokes }
    }
    // Draw at the ratio the notes were saved with so they stay faithful (no stretching);
    // a sensible default for brand-new notes.
    val displayRatio = (notePage?.aspectRatio)?.takeIf { it > 0f } ?: 1.5f
    // Debounced autosave (docs/03 — autosave everything).
    LaunchedEffect(strokes, notesDirty) {
        if (!notesDirty) return@LaunchedEffect
        val s = strokes ?: return@LaunchedEffect
        delay(400)
        vm.saveNotes(s, displayRatio)
    }

    val onAddStroke: (InkStroke) -> Unit = { st -> strokes = (strokes ?: emptyList()) + st; undone = emptyList(); notesDirty = true }
    val onErase: (List<InkStroke>) -> Unit = { newList -> strokes = newList; undone = emptyList(); notesDirty = true }
    val onUndo: () -> Unit = { val s = strokes; if (!s.isNullOrEmpty()) { undone = undone + s.last(); strokes = s.dropLast(1); notesDirty = true } }
    val onRedo: () -> Unit = { if (undone.isNotEmpty()) { strokes = (strokes ?: emptyList()) + undone.last(); undone = undone.dropLast(1); notesDirty = true } }
    val onClear: () -> Unit = { if (!strokes.isNullOrEmpty()) { undone = emptyList(); strokes = emptyList(); notesDirty = true } }

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
            AppTextField(draft.uniformNumber.orEmpty(), { v -> edit { it.copy(uniformNumber = v.ifBlank { null }) } }, "Uniform Number", keyboardType = KeyboardType.Number)
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
            val editStrokes = strokes
            if (editStrokes == null) {
                EmptyState("Loading notes…")
            } else {
                NoteCanvasPanel(
                    strokes = editStrokes,
                    onAddStroke = onAddStroke,
                    onErase = onErase,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onClear = onClear,
                    canUndo = editStrokes.isNotEmpty(),
                    canRedo = undone.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    noteAspectRatio = displayRatio,
                )
            }

            // Observation status
            ObservationStatusField(draft.observationStatus) { status -> edit { it.copy(observationStatus = status) } }

            // Video evidence (YouTube links + timestamps)
            VideoEvidenceSection(
                evidence = draft.videoEvidence,
                onAdd = { ev -> edit { it.copy(videoEvidence = it.videoEvidence + ev) } },
                onRemove = { ev -> edit { it.copy(videoEvidence = it.videoEvidence.filterNot { x -> x.id == ev.id }) } },
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
