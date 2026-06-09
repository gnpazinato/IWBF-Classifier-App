package com.iwbfclassifier.core

import java.time.Instant
import java.util.UUID

/** Stable random id for local records. */
fun newId(): String = UUID.randomUUID().toString()

/** ISO-8601 UTC timestamp, e.g. 2026-05-31T12:00:00Z. */
fun nowIso(): String = Instant.now().toString()

/**
 * Normalize a Team/Player name to a single line. Imports (DOCX/PDF) can capture a name that
 * wrapped across two lines, producing an embedded line break — the single-line edit fields
 * then clip it so the hidden remainder can't be seen or removed, while the observation chip
 * (maxLines=2) and header render it as a stray "second name". Collapsing every run of
 * whitespace (newlines/tabs/multiple spaces) to one space keeps the text faithful — nothing
 * is dropped — while guaranteeing the name is always one line. Blank → null.
 */
fun cleanName(value: String?): String? =
    value?.replace(Regex("\\s+"), " ")?.trim()?.ifBlank { null }
