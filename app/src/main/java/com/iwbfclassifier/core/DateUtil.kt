package com.iwbfclassifier.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Dates are stored internally as ISO (yyyy-MM-dd) for stable sorting, but always
 * shown to the user as dd/MM/yyyy (per field feedback).
 */
private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val DISPLAY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** ISO (yyyy-MM-dd) -> dd/MM/yyyy. Falls back to the raw value if unparseable. */
fun isoToDisplayDate(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return runCatching { LocalDate.parse(iso.trim(), ISO).format(DISPLAY) }.getOrNull()
        ?: runCatching { LocalDate.parse(iso.trim(), DISPLAY).format(DISPLAY) }.getOrNull()
        ?: iso
}

/** Material3 date pickers return UTC millis at midnight -> ISO yyyy-MM-dd. */
fun epochMillisToIso(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().format(ISO)

/** ISO yyyy-MM-dd -> UTC millis at midnight, for seeding a date picker. */
fun isoToEpochMillis(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    return runCatching {
        LocalDate.parse(iso.trim(), ISO).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }.getOrNull()
}
