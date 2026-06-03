package com.iwbfclassifier.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iwbfclassifier.core.buildYoutubeUrl
import com.iwbfclassifier.core.extractYoutubeId
import com.iwbfclassifier.core.extractYoutubeStartSeconds
import com.iwbfclassifier.core.formatSeconds
import com.iwbfclassifier.core.newId
import com.iwbfclassifier.core.nowIso
import com.iwbfclassifier.core.parseTimestampToSeconds
import com.iwbfclassifier.data.model.VideoEvidence
import com.iwbfclassifier.ui.theme.AppColors
import com.iwbfclassifier.ui.theme.AppShapes
import com.iwbfclassifier.ui.theme.AppSpacing
import com.iwbfclassifier.ui.theme.AppTypography

/**
 * Attach YouTube moments (link + timestamp) to a Player. We store only the
 * link/timestamp — never the video (docs/06). Tapping opens it in YouTube/browser.
 */
@Composable
fun VideoEvidenceSection(
    evidence: List<VideoEvidence>,
    onAdd: (VideoEvidence) -> Unit,
    onRemove: (VideoEvidence) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showAddButton: Boolean = true,
    onReplay: ((VideoEvidence) -> Unit)? = null,
) {
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val title = if (evidence.isEmpty()) "Key Moments (YouTube)"
            else "Key Moments (${evidence.size})"
            SectionLabel(title, modifier = Modifier.weight(1f))
            if (showAddButton) SecondaryButton("Add Moment", onClick = { showAdd = true })
        }

        if (compact) {
            // Header only; full list lives on the player edit screen.
        } else if (evidence.isEmpty()) {
            Text(
                "No video moments yet. Paste a YouTube link and a timestamp to keep it as evidence.",
                style = AppTypography.body,
                color = AppColors.TextMuted,
            )
        } else {
            evidence.forEach { ev ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.card)
                        .background(AppColors.CardCharcoal)
                        .border(1.dp, AppColors.DividerGray, AppShapes.card)
                        .padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).clickable { onReplay?.invoke(ev) ?: openLink(context, ev.url) }) {
                        Text(
                            ev.label?.takeIf { it.isNotBlank() } ?: "YouTube moment",
                            style = AppTypography.body,
                            color = AppColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val window = formatSeconds(ev.startSeconds)?.let { s ->
                            formatSeconds(ev.endSeconds)?.let { e -> "$s–$e" } ?: "@ $s"
                        }
                        val rate = if (ev.playbackRate != 1.0) "${ev.playbackRate}x" else null
                        val sub = listOfNotNull(window, rate, ev.url).joinToString("  ·  ")
                        Text(sub, style = AppTypography.microLabel, color = AppColors.Gold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (onReplay != null) {
                        TextButton(onClick = { onReplay(ev) }) { Text("Replay", color = AppColors.Gold) }
                    }
                    TextButton(onClick = { openLink(context, ev.url) }) { Text("Open", color = AppColors.Gold) }
                    TextButton(onClick = { onRemove(ev) }) { Text("Remove", color = AppColors.TextSecondary) }
                }
            }
        }
    }

    if (showAdd) {
        AddVideoMomentDialog(
            onDismiss = { showAdd = false },
            onAdd = { ev ->
                onAdd(ev)
                showAdd = false
            },
        )
    }
}

private fun openLink(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

/**
 * A player's key moments in a dialog (user request): opening it from the observation
 * screen keeps the moment list off the note canvas. Replay runs in the embedded player;
 * Open launches YouTube; manual add is available when [onAdd] is provided.
 */
@Composable
fun KeyMomentsDialog(
    evidence: List<VideoEvidence>,
    onReplay: ((VideoEvidence) -> Unit)?,
    onRemove: (VideoEvidence) -> Unit,
    onAdd: ((VideoEvidence) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var showManual by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.CardCharcoal,
        title = { Text("Key Moments (${evidence.size})", color = AppColors.TextPrimary) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                if (evidence.isEmpty()) {
                    Text(
                        "No moments yet. Tap Add Moment while watching to capture a slow-motion clip.",
                        style = AppTypography.body,
                        color = AppColors.TextMuted,
                    )
                } else {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    ) {
                        evidence.forEach { ev ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(AppShapes.card)
                                    .background(AppColors.PanelBlack)
                                    .border(1.dp, AppColors.DividerGray, AppShapes.card)
                                    .padding(AppSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        ev.label?.takeIf { it.isNotBlank() } ?: "YouTube moment",
                                        style = AppTypography.body,
                                        color = AppColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    val window = formatSeconds(ev.startSeconds)?.let { s ->
                                        formatSeconds(ev.endSeconds)?.let { e -> "$s–$e" } ?: "@ $s"
                                    }
                                    val rate = if (ev.playbackRate != 1.0) "${ev.playbackRate}x" else null
                                    Text(
                                        listOfNotNull(window, rate).joinToString("  ·  "),
                                        style = AppTypography.microLabel,
                                        color = AppColors.Gold,
                                        maxLines = 1,
                                    )
                                }
                                if (onReplay != null) TextButton(onClick = { onReplay(ev) }) { Text("Replay", color = AppColors.Gold) }
                                TextButton(onClick = { openLink(context, ev.url) }) { Text("Open", color = AppColors.Gold) }
                                TextButton(onClick = { onRemove(ev) }) { Text("Remove", color = AppColors.TextSecondary) }
                            }
                        }
                    }
                }
                if (onAdd != null) SecondaryButton("Add link manually", onClick = { showManual = true }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = AppColors.Gold) } },
    )
    if (showManual && onAdd != null) {
        AddVideoMomentDialog(onDismiss = { showManual = false }, onAdd = { onAdd(it); showManual = false })
    }
}

/**
 * Manual time entry for "Add Moment" when the embedded player's position can't be read
 * (the stream is embed-blocked and playing in the YouTube app, so the in-app player
 * reports 0s). The classifier types the moment's time and we keep a ±5s window.
 */
@Composable
fun CaptureMomentDialog(
    initialSeconds: Int,
    onConfirm: (centerSeconds: Int, label: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var time by remember { mutableStateOf(formatSeconds(initialSeconds.takeIf { it > 0 }).orEmpty()) }
    var label by remember { mutableStateOf("") }
    val seconds = parseTimestampToSeconds(time)
    val canAdd = seconds != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.CardCharcoal,
        title = { Text("Add Moment", color = AppColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                Text(
                    "Couldn't read the player time automatically (the stream is playing outside the app). " +
                        "Type the moment's time — we'll keep a ±5s slow-motion window around it.",
                    style = AppTypography.microLabel,
                    color = AppColors.TextMuted,
                )
                AppTextField(time, { time = it }, "Time — mm:ss", placeholder = "12:34")
                AppTextField(label, { label = it }, "Label (optional)")
            }
        },
        confirmButton = {
            TextButton(enabled = canAdd, onClick = { onConfirm(seconds!!, label.ifBlank { null }) }) {
                Text("Add", color = if (canAdd) AppColors.Gold else AppColors.TextMuted)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.TextSecondary) } },
    )
}

@Composable
private fun AddVideoMomentDialog(
    onDismiss: () -> Unit,
    onAdd: (VideoEvidence) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }

    val videoId = extractYoutubeId(url)
    val canAdd = url.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.CardCharcoal,
        title = { Text("Add YouTube Moment", color = AppColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                AppTextField(url, { url = it }, "YouTube link")
                AppTextField(time, { time = it }, "Timestamp — mm:ss (optional)", placeholder = "1:23")
                AppTextField(label, { label = it }, "Label (optional)")
                if (url.isNotBlank() && videoId == null) {
                    Text(
                        "Couldn't detect a YouTube video id — the link will be saved as-is.",
                        style = AppTypography.microLabel,
                        color = AppColors.AlertRed,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canAdd,
                onClick = {
                    val seconds = parseTimestampToSeconds(time) ?: extractYoutubeStartSeconds(url)
                    onAdd(
                        VideoEvidence(
                            id = newId(),
                            url = buildYoutubeUrl(videoId, url, seconds),
                            videoId = videoId,
                            startSeconds = seconds,
                            label = label.ifBlank { null },
                            createdAt = nowIso(),
                        ),
                    )
                },
            ) { Text("Add", color = if (canAdd) AppColors.Gold else AppColors.TextMuted) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.TextSecondary) } },
    )
}
