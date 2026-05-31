package com.iwbfclassifier.core

import java.time.Instant
import java.util.UUID

/** Stable random id for local records. */
fun newId(): String = UUID.randomUUID().toString()

/** ISO-8601 UTC timestamp, e.g. 2026-05-31T12:00:00Z. */
fun nowIso(): String = Instant.now().toString()
