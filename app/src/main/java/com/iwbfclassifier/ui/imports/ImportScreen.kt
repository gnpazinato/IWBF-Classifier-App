package com.iwbfclassifier.ui.imports

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.iwbfclassifier.core.importer.ParsedRoster
import com.iwbfclassifier.core.importer.ParsedTeam
import com.iwbfclassifier.core.importer.RosterParser
import com.iwbfclassifier.ui.LocalAppContainer
import com.iwbfclassifier.ui.components.AppTextField
import com.iwbfclassifier.ui.components.AppTopBar
import com.iwbfclassifier.ui.components.EmptyState
import com.iwbfclassifier.ui.components.PrimaryButton
import com.iwbfclassifier.ui.components.SecondaryButton
import com.iwbfclassifier.ui.components.SectionLabel
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppShapes
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val IMPORT_MIME_TYPES = arrayOf(
    "application/zip",
    "application/x-zip-compressed",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
    "text/csv",
    "text/comma-separated-values",
    "application/pdf",
    "application/octet-stream",
)

/**
 * Official single-sheet roster template (user request). Filling this in and importing
 * it gives a clean roster — no coaching staff, no misread jersey numbers. Columns are
 * matched by header name, so order can change and extra example rows can be deleted.
 */
private const val TEMPLATE_FILE_NAME = "iwbf_roster_template.csv"
private const val TEMPLATE_CSV =
    "team,number,initial_class,class_status,full_name\n" +
        "Argentina,4,1.0,C,\"VINCI, Sarah\"\n" +
        "Argentina,7,3.5,N,\"GOMEZ, Maria\"\n" +
        "Australia,5,2.0,R,\"SMITH, Jane\"\n" +
        "Australia,11,4.0,C,\"BROWN, Emily\"\n"

/**
 * Roster import: pick a ZIP/Word/Excel/PDF, review what was detected, then create
 * teams + players in one shot. [competitionId] = null imports into a brand new
 * competition; otherwise imports into the existing one.
 */
@Composable
fun ImportScreen(
    competitionId: String?,
    onBack: () -> Unit,
    onImported: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    val repo = container.competitionRepository
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var parsing by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var roster by remember { mutableStateOf<ParsedRoster?>(null) }
    var excluded by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var competitionName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val templateSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(TEMPLATE_CSV.toByteArray()) }
                }
            }.onFailure { error = it.message ?: "Could not save the template." }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        parsing = true
        error = null
        roster = null
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val name = queryDisplayName(context, uri) ?: "import"
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("Could not open the file.")
                    name to RosterParser.parse(name, bytes)
                }
            }
            parsing = false
            result.onSuccess { (name, parsed) ->
                roster = parsed
                excluded = emptySet()
                if (competitionId == null && competitionName.isBlank()) {
                    competitionName = name.substringBeforeLast('.').substringBefore(" - ").trim()
                }
            }.onFailure { error = it.message ?: "Could not read the file." }
        }
    }

    Column(Modifier.fillMaxSize().background(AppColors.InkBlack)) {
        AppTopBar(title = "Import Roster", onBack = onBack)

        val current = roster
        when {
            parsing -> CenterMessage("Reading file…")
            current == null -> PickPrompt(
                error = error,
                onDownloadTemplate = { templateSaver.launch(TEMPLATE_FILE_NAME) },
                onPick = { picker.launch(IMPORT_MIME_TYPES) },
            )
            else -> ReviewContent(
                roster = current,
                excluded = excluded,
                onToggleTeam = { idx -> excluded = if (idx in excluded) excluded - idx else excluded + idx },
                isNewCompetition = competitionId == null,
                competitionName = competitionName,
                onNameChange = { competitionName = it },
                importing = importing,
                onChooseAnother = { picker.launch(IMPORT_MIME_TYPES) },
                onImport = {
                    val includedTeams = current.teams.filterIndexed { i, _ -> i !in excluded }
                    if (includedTeams.isEmpty()) {
                        error = "Select at least one team to import."
                    } else {
                        importing = true
                        scope.launch {
                            val cid = competitionId ?: repo.createCompetition(
                                name = competitionName.ifBlank { "Imported Competition" },
                            ).id
                            repo.importRoster(cid, includedTeams)
                            importing = false
                            onImported(cid)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun PickPrompt(error: String?, onDownloadTemplate: () -> Unit, onPick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyState(
            "Import a roster",
            "Best results: download the spreadsheet template, fill one row per athlete " +
                "(team, number, initial_class, class_status, full_name) and import it back. " +
                "You can also import a ZIP/Word/Excel/PDF, but unstandardised files may misread numbers or pull staff.",
        )
        Spacer(Modifier.height(AppSpacing.md))
        SecondaryButton("Download Template (.csv)", onClick = onDownloadTemplate, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(AppSpacing.md))
        PrimaryButton("Choose File", onClick = onPick, modifier = Modifier.fillMaxWidth())
        if (error != null) {
            Spacer(Modifier.height(AppSpacing.md))
            Text(error, style = AppTypography.body, color = AppColors.AlertRed)
        }
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = AppTypography.header, color = AppColors.TextSecondary)
    }
}

@Composable
private fun ReviewContent(
    roster: ParsedRoster,
    excluded: Set<Int>,
    onToggleTeam: (Int) -> Unit,
    isNewCompetition: Boolean,
    competitionName: String,
    onNameChange: (String) -> Unit,
    importing: Boolean,
    onChooseAnother: () -> Unit,
    onImport: () -> Unit,
) {
    val includedPlayers = roster.teams
        .filterIndexed { i, _ -> i !in excluded }
        .sumOf { it.players.size }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        item {
            Column(
                Modifier.fillMaxWidth().clip(AppShapes.card).background(AppColors.CardCharcoal).padding(AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                Text("Parse Summary", style = AppTypography.header, color = AppColors.TextPrimary)
                Text(
                    "${roster.teamCount} teams · ${roster.playerCount} players · ${roster.filesFound.size} files read",
                    style = AppTypography.body,
                    color = AppColors.TextSecondary,
                )
                if (roster.filesFailed.isNotEmpty()) {
                    Text("Failed: ${roster.filesFailed.joinToString(", ")}", style = AppTypography.microLabel, color = AppColors.AlertRed)
                }
                roster.warnings.forEach { w ->
                    Text("• $w", style = AppTypography.microLabel, color = AppColors.TextMuted)
                }
            }
        }

        if (isNewCompetition) {
            item {
                AppTextField(competitionName, onNameChange, "New competition name")
            }
        }

        item { SectionLabel("Teams to import") }

        if (roster.teams.isEmpty()) {
            item {
                EmptyState(
                    "No teams detected",
                    "If this was a PDF, table reading is limited — create teams/players manually after import.",
                )
            }
        }

        items(roster.teams.size) { idx ->
            val team = roster.teams[idx]
            TeamReviewRow(
                team = team,
                included = idx !in excluded,
                onToggle = { onToggleTeam(idx) },
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                SecondaryButton("Choose Another File", onClick = onChooseAnother, modifier = Modifier.weight(1f))
                PrimaryButton(
                    if (importing) "Importing…" else "Import $includedPlayers Players",
                    onClick = onImport,
                    enabled = !importing && includedPlayers > 0,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TeamReviewRow(team: ParsedTeam, included: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.CardCharcoal)
            .border(1.dp, if (included) AppColors.GoldBorder else AppColors.DividerGray, AppShapes.card)
            .clickable(onClick = onToggle)
            .padding(AppSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(team.name, style = AppTypography.header, color = AppColors.TextPrimary)
            val sub = listOfNotNull(team.code, "${team.players.size} players", team.sourceFile).joinToString(" · ")
            Text(sub, style = AppTypography.microLabel, color = AppColors.TextMuted)
        }
        Text(
            if (included) "Included" else "Skipped",
            style = AppTypography.chip,
            color = if (included) AppColors.Gold else AppColors.TextMuted,
        )
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) return c.getString(idx)
        }
    }
    return uri.lastPathSegment
}
