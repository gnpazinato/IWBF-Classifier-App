package com.iwbfclassifier.core.importer

import com.iwbfclassifier.data.model.SportClass
import com.iwbfclassifier.data.model.SportClassStatus

/** A player parsed from an entry list / MIC list (docs/05). All fields optional. */
data class ParsedPlayer(
    val number: String? = null,
    val name: String? = null,
    val importedClass: SportClass? = null,
    val scs: SportClassStatus? = null,
    val iwbfId: String? = null,
    val dob: String? = null,
    val healthCondition: String? = null,
    val impairment: String? = null,
    val notes: String? = null,
    val panel: String? = null,
    val sourceFile: String? = null,
)

/** A team parsed from a source file. [gender] is normalized to "W"/"M" (or null). */
data class ParsedTeam(
    val name: String,
    val code: String? = null,
    val gender: String? = null,
    val sourceFile: String? = null,
    val players: List<ParsedPlayer> = emptyList(),
)

/** Full result of parsing an uploaded file (a single doc or a ZIP of docs). */
data class ParsedRoster(
    val teams: List<ParsedTeam> = emptyList(),
    val filesFound: List<String> = emptyList(),
    val filesFailed: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    /** Raw text recovered from best-effort sources (e.g. PDF) for manual entry. */
    val rawTextByFile: Map<String, String> = emptyMap(),
) {
    val teamCount get() = teams.size
    val playerCount get() = teams.sumOf { it.players.size }
}

/** Counts returned after committing an import into a competition. */
data class ImportResult(val teamsCreated: Int, val playersCreated: Int)
