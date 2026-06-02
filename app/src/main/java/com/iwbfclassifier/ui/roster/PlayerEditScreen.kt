package com.iwbfclassifier.ui.roster

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.iwbfclassifier.data.model.ObservationStatus
import com.iwbfclassifier.data.model.Player
import com.iwbfclassifier.data.model.SportClass
import com.iwbfclassifier.data.model.SportClassStatus
import com.iwbfclassifier.data.repository.CompetitionRepository
import com.iwbfclassifier.ui.LocalAppContainer
import com.iwbfclassifier.ui.components.AppTextField
import com.iwbfclassifier.ui.components.AppTopBar
import com.iwbfclassifier.ui.components.ClassButtonRow
import com.iwbfclassifier.ui.components.ConfirmDialog
import com.iwbfclassifier.ui.components.DestructiveButton
import com.iwbfclassifier.ui.components.EmptyState
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    /** Autosave: persist immediately on every change (docs/03 — autosave everything). */
    fun save(updated: Player) {
        viewModelScope.launch {
            _saving.value = true
            repo.updatePlayer(updated)
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

            // Class status from the entry sheet
            StatusField(
                value = draft.sportClassStatus,
                onSelect = { status -> edit { it.copy(sportClassStatus = status) } },
            )

            // Decision fields (docs/02)
            ClassField("Initial Class", draft.startingSportClass) { sc -> edit { it.copy(startingSportClass = sc) } }
            ClassField("My Opinion Class", draft.myOpinionSportClass) { sc -> edit { it.copy(myOpinionSportClass = sc) } }
            ClassField("Final Class", draft.finalSportClass) { sc -> edit { it.copy(finalSportClass = sc) } }

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

@Composable
private fun ClassField(label: String, value: SportClass?, onSelect: (SportClass?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        SectionLabel(label)
        ClassButtonRow(selected = value, onSelect = { selected -> onSelect(if (selected == value) null else selected) })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusField(value: SportClassStatus?, onSelect: (SportClassStatus?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        SectionLabel("Sport Class Status")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            SportClassStatus.selectable.forEach { status ->
                SelectableChip(
                    text = status.code,
                    selected = status == value,
                    onClick = { onSelect(if (status == value) null else status) },
                )
            }
        }
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
