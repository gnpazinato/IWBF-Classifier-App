package com.iwbfclassifier.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.iwbfclassifier.ui.LocalAppContainer
import com.iwbfclassifier.ui.components.AppTopBar
import com.iwbfclassifier.ui.components.ConfirmDialog
import com.iwbfclassifier.ui.components.PrimaryButton
import com.iwbfclassifier.ui.components.SecondaryButton
import com.iwbfclassifier.ui.components.SectionLabel
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Backup & Restore (user request): export every competition/player/note to a single `.zip`
 * — savable to Google Drive via the Android picker — and import it back on this or another
 * tablet. Fully local: the picker (Storage Access Framework) does the Drive part, the app
 * never talks to a server.
 */
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true; status = null; error = false
            val result = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { container.exportBackup(it) }
                    ?: error("Couldn't open the chosen location.")
            }
            busy = false
            result.fold(
                onSuccess = { status = "Backup saved. Keep this .zip safe — e.g. in Google Drive." },
                onFailure = { error = true; status = "Export failed: ${it.message}" },
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? -> if (uri != null) pendingImport = uri }

    Column(Modifier.fillMaxSize().background(AppColors.InkBlack)) {
        AppTopBar(title = "Backup & Restore", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
        ) {
            SectionLabel("Backup")
            Text(
                "Save a .zip with every competition, team, player, sport class and handwritten " +
                    "note. Choose Google Drive (or any folder) as the destination to keep it safe " +
                    "and to move your data to another tablet.",
                style = AppTypography.body,
                color = AppColors.TextSecondary,
            )
            PrimaryButton(
                text = "Export backup (.zip)",
                onClick = { exportLauncher.launch("iwbf-backup-${LocalDate.now()}.zip") },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Restore")
            Text(
                "Import a .zip backup. Competitions in the backup are added or updated; nothing " +
                    "already on this tablet is deleted.",
                style = AppTypography.body,
                color = AppColors.TextSecondary,
            )
            SecondaryButton(
                text = "Import backup (.zip)",
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )

            if (busy) {
                Text("Working…", style = AppTypography.body, color = AppColors.Gold)
            }
            status?.let { msg ->
                Text(msg, style = AppTypography.body, color = if (error) AppColors.AlertRed else AppColors.TextPrimary)
            }
        }
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            title = "Restore from backup?",
            message = "This adds or updates competitions from the selected .zip. Data already on " +
                "this tablet is kept. Continue?",
            confirmText = "Restore",
            onConfirm = {
                pendingImport = null
                scope.launch {
                    busy = true; status = null; error = false
                    val result = runCatching {
                        context.contentResolver.openInputStream(uri)?.use { container.importBackup(it) }
                            ?: error("Couldn't open the selected file.")
                    }
                    busy = false
                    result.fold(
                        onSuccess = { status = "Restore complete." },
                        onFailure = { error = true; status = "Import failed: ${it.message}" },
                    )
                }
            },
            onDismiss = { pendingImport = null },
        )
    }
}
