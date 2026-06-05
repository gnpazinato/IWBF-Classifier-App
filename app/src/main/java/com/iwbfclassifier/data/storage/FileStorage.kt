package com.iwbfclassifier.data.storage

import java.io.File

/**
 * Human-inspectable, local-first folder layout (docs/04_data_model.md):
 *
 *   <root>/competitions/<competitionId>/
 *       competition.json
 *       teams/<teamId>.json
 *       players/<playerId>.json
 *
 * games/, notes/, video_markers/, attachments/, imports/ arrive in later phases.
 */
class FileStorage(val root: File) {

    private fun File.ensureDir(): File {
        if (!exists()) mkdirs()
        return this
    }

    private val competitionsDir: File
        get() = File(root, "competitions").ensureDir()

    fun competitionDir(competitionId: String): File =
        File(competitionsDir, competitionId).ensureDir()

    fun competitionFile(competitionId: String): File =
        File(competitionDir(competitionId), "competition.json")

    fun teamsDir(competitionId: String): File =
        File(competitionDir(competitionId), "teams").ensureDir()

    fun teamFile(competitionId: String, teamId: String): File =
        File(teamsDir(competitionId), "$teamId.json")

    fun playersDir(competitionId: String): File =
        File(competitionDir(competitionId), "players").ensureDir()

    fun playerFile(competitionId: String, playerId: String): File =
        File(playersDir(competitionId), "$playerId.json")

    fun notesDir(competitionId: String): File =
        File(competitionDir(competitionId), "notes").ensureDir()

    fun noteFile(competitionId: String, playerId: String): File =
        File(notesDir(competitionId), "$playerId.json")

    /** Current game setup (selected teams + YouTube livestream) for a competition. */
    fun gameFile(competitionId: String): File =
        File(competitionDir(competitionId), "game.json")

    fun listCompetitionIds(): List<String> =
        competitionsDir.listFiles { f -> f.isDirectory }?.map { it.name }?.sorted() ?: emptyList()

    fun listTeamFiles(competitionId: String): List<File> =
        teamsDir(competitionId).listFiles { f -> f.isFile && f.extension == "json" }?.toList() ?: emptyList()

    fun listPlayerFiles(competitionId: String): List<File> =
        playersDir(competitionId).listFiles { f -> f.isFile && f.extension == "json" }?.toList() ?: emptyList()

    /** Write-to-temp-then-rename so a crash mid-write cannot corrupt the live file. */
    fun writeText(target: File, content: String) {
        target.parentFile?.ensureDir()
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(content)
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            target.writeText(content)
            tmp.delete()
        }
    }

    fun deleteCompetition(competitionId: String) {
        File(competitionsDir, competitionId).deleteRecursively()
    }
}
