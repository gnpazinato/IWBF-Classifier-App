package com.iwbfclassifier.data.repository

import com.iwbfclassifier.core.newId
import com.iwbfclassifier.core.nowIso
import com.iwbfclassifier.data.model.Competition
import com.iwbfclassifier.data.model.Player
import com.iwbfclassifier.data.model.Team
import com.iwbfclassifier.data.serialization.AppJson
import com.iwbfclassifier.data.storage.FileStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * Local JSON-file-backed repository (Phase 1, docs/08). Data lives in memory for
 * fast observation and is persisted to the human-inspectable folder tree on every
 * mutation.
 */
class JsonCompetitionRepository(
    private val storage: FileStorage,
) : CompetitionRepository {

    private val _competitions = MutableStateFlow<List<Competition>>(emptyList())
    override val competitions: StateFlow<List<Competition>> = _competitions.asStateFlow()

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    override val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    override val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val mutex = Mutex()

    /** Load everything from disk into memory. Call once at startup. */
    suspend fun load() = withContext(Dispatchers.IO) {
        val comps = mutableListOf<Competition>()
        val allTeams = mutableListOf<Team>()
        val allPlayers = mutableListOf<Player>()
        for (cid in storage.listCompetitionIds()) {
            val cf = storage.competitionFile(cid)
            if (cf.exists()) runCatching { comps += AppJson.decodeFromString<Competition>(cf.readText()) }
            storage.listTeamFiles(cid).forEach { f ->
                runCatching { allTeams += AppJson.decodeFromString<Team>(f.readText()) }
            }
            storage.listPlayerFiles(cid).forEach { f ->
                runCatching { allPlayers += AppJson.decodeFromString<Player>(f.readText()) }
            }
        }
        _competitions.value = comps.sortedByDescending { it.createdAt }
        _teams.value = allTeams
        _players.value = allPlayers
    }

    // --- Competition ---

    override suspend fun createCompetition(name: String, location: String?, startDate: String?): Competition =
        mutex.withLock {
            val now = nowIso()
            val competition = Competition(
                id = newId(),
                name = name.ifBlank { "Untitled Competition" },
                location = location?.ifBlank { null },
                startDate = startDate?.ifBlank { null },
                createdAt = now,
                updatedAt = now,
            )
            persist(competition)
            _competitions.value = (_competitions.value + competition).sortedByDescending { it.createdAt }
            competition
        }

    override suspend fun updateCompetition(competition: Competition) = mutex.withLock {
        val updated = competition.copy(updatedAt = nowIso())
        persist(updated)
        _competitions.value = _competitions.value.map { if (it.id == updated.id) updated else it }
    }

    override suspend fun deleteCompetition(competitionId: String) = mutex.withLock {
        withContext(Dispatchers.IO) { storage.deleteCompetition(competitionId) }
        _competitions.value = _competitions.value.filterNot { it.id == competitionId }
        _teams.value = _teams.value.filterNot { it.competitionId == competitionId }
        _players.value = _players.value.filterNot { it.competitionId == competitionId }
    }

    // --- Team ---

    override suspend fun createTeam(competitionId: String, name: String, code: String?, gender: String?): Team =
        mutex.withLock {
            val team = Team(
                id = newId(),
                competitionId = competitionId,
                name = name.ifBlank { "New Team" },
                code = code?.ifBlank { null },
                gender = gender?.ifBlank { null },
            )
            persist(team)
            _teams.value = _teams.value + team
            team
        }

    override suspend fun updateTeam(team: Team) = mutex.withLock {
        persist(team)
        _teams.value = _teams.value.map { if (it.id == team.id) team else it }
    }

    override suspend fun setTeamActive(teamId: String, active: Boolean) = mutex.withLock {
        val team = _teams.value.firstOrNull { it.id == teamId } ?: return@withLock
        val updated = team.copy(active = active)
        persist(updated)
        _teams.value = _teams.value.map { if (it.id == teamId) updated else it }
    }

    override suspend fun deleteTeamPermanently(teamId: String) = mutex.withLock {
        val team = _teams.value.firstOrNull { it.id == teamId } ?: return@withLock
        val teamPlayers = _players.value.filter { it.teamId == teamId }
        withContext(Dispatchers.IO) {
            storage.teamFile(team.competitionId, team.id).delete()
            teamPlayers.forEach { storage.playerFile(it.competitionId, it.id).delete() }
        }
        _teams.value = _teams.value.filterNot { it.id == teamId }
        _players.value = _players.value.filterNot { it.teamId == teamId }
    }

    // --- Player ---

    override suspend fun createPlayer(
        competitionId: String,
        teamId: String,
        uniformNumber: String?,
        name: String?,
    ): Player = mutex.withLock {
        val now = nowIso()
        val player = Player(
            id = newId(),
            competitionId = competitionId,
            teamId = teamId,
            uniformNumber = uniformNumber?.ifBlank { null },
            name = name?.ifBlank { null },
            createdAt = now,
            updatedAt = now,
        )
        persist(player)
        _players.value = _players.value + player
        player
    }

    override suspend fun updatePlayer(player: Player) = mutex.withLock {
        val updated = player.copy(updatedAt = nowIso())
        persist(updated)
        _players.value = _players.value.map { if (it.id == updated.id) updated else it }
    }

    override suspend fun setPlayerActive(playerId: String, active: Boolean) = mutex.withLock {
        val player = _players.value.firstOrNull { it.id == playerId } ?: return@withLock
        val updated = player.copy(active = active, updatedAt = nowIso())
        persist(updated)
        _players.value = _players.value.map { if (it.id == playerId) updated else it }
    }

    override suspend fun deletePlayerPermanently(playerId: String) = mutex.withLock {
        val player = _players.value.firstOrNull { it.id == playerId } ?: return@withLock
        withContext(Dispatchers.IO) { storage.playerFile(player.competitionId, player.id).delete() }
        _players.value = _players.value.filterNot { it.id == playerId }
    }

    // --- persistence helpers ---

    private suspend fun persist(competition: Competition) = withContext(Dispatchers.IO) {
        storage.writeText(storage.competitionFile(competition.id), AppJson.encodeToString(competition))
    }

    private suspend fun persist(team: Team) = withContext(Dispatchers.IO) {
        storage.writeText(storage.teamFile(team.competitionId, team.id), AppJson.encodeToString(team))
    }

    private suspend fun persist(player: Player) = withContext(Dispatchers.IO) {
        storage.writeText(storage.playerFile(player.competitionId, player.id), AppJson.encodeToString(player))
    }
}
