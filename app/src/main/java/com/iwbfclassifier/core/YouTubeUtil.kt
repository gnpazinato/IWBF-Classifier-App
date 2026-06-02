package com.iwbfclassifier.core

/** Helpers to turn a pasted YouTube URL + a timestamp into a canonical evidence link. */

private val ID_PATTERNS = listOf(
    Regex("""youtu\.be/([A-Za-z0-9_-]{11})"""),
    Regex("""[?&]v=([A-Za-z0-9_-]{11})"""),
    Regex("""youtube\.com/(?:live|embed|shorts)/([A-Za-z0-9_-]{11})"""),
)

/** Extracts the 11-char YouTube video id from any common URL form. */
fun extractYoutubeId(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val u = url.trim()
    for (p in ID_PATTERNS) p.find(u)?.let { return it.groupValues[1] }
    // Bare id pasted on its own.
    if (Regex("""^[A-Za-z0-9_-]{11}$""").matches(u)) return u
    return null
}

/** Reads a t=/start= seconds value already present in a URL (supports 1h2m3s and plain seconds). */
fun extractYoutubeStartSeconds(url: String?): Int? {
    if (url.isNullOrBlank()) return null
    val m = Regex("""[?&](?:t|start)=([0-9hms]+)""").find(url.trim()) ?: return null
    return parseTimestampToSeconds(m.groupValues[1])
}

/**
 * Parses a timestamp the user typed: "mm:ss", "h:mm:ss", "90", "1m30s", "75s".
 * Returns total seconds, or null if blank/unparseable.
 */
fun parseTimestampToSeconds(raw: String?): Int? {
    if (raw.isNullOrBlank()) return null
    val s = raw.trim().lowercase()
    if (s.contains(":")) {
        val parts = s.split(":").mapNotNull { it.trim().toIntOrNull() }
        if (parts.isEmpty()) return null
        return when (parts.size) {
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            2 -> parts[0] * 60 + parts[1]
            else -> parts[0]
        }
    }
    s.toIntOrNull()?.let { return it }
    // 1h2m3s style
    val h = Regex("""(\d+)h""").find(s)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val m = Regex("""(\d+)m""").find(s)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val sec = Regex("""(\d+)s""").find(s)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val total = h * 3600 + m * 60 + sec
    return if (total > 0) total else null
}

/** Builds a canonical youtu.be link at [seconds]; falls back to the original URL. */
fun buildYoutubeUrl(videoId: String?, originalUrl: String, seconds: Int?): String {
    if (videoId == null) return originalUrl.trim()
    return if (seconds != null && seconds > 0) "https://youtu.be/$videoId?t=$seconds"
    else "https://youtu.be/$videoId"
}

/** seconds -> h:mm:ss / m:ss for display. */
fun formatSeconds(seconds: Int?): String? {
    if (seconds == null || seconds < 0) return null
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
