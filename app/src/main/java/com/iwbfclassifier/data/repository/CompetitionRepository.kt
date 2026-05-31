package com.iwbfclassifier.data.repository

import com.iwbfclassifier.data.model.Competition
import com.iwbfclassifier.data.model.Player
import com.iwbfclassifier.data.model.Team
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for local competition data. Implementations persist to
 * local storage but expose observable in-memory state for the UI.
 *
 * Soft delete = active flag (Teams/Players) so notes are never lost (docs/04).
 * Permanent delete is reserved for explicit management actions.
 */
interface CompetitionRepository {
    val competitions: StateFlow<List<Competition>>
    val teams: StateFlow<List<Team>>
    val players: StateFlow<List<Player>>

    suspend fun createCompetition(name: String, location: String? = null, startDate: String? = null): Competition
    suspend fun updateCompetition(competition: Competition)
    suspend fun deleteCompetition(competitionId: String)

    suspend fun createTeam(competitionId: String, name: String, code: String? = null, gender: String? = null): Team
    suspend fun updateTeam(team: Team)
    suspend fun setTeamActive(teamId: String, active: Boolean)
    suspend fun deleteTeamPermanently(teamId: String)

    suspend fun createPlayer(
        competitionId: String,
        teamId: String,
        uniformNumber: String? = null,
        name: String? = null,
    ): Player
    suspend fun updatePlayer(player: Player)
    suspend fun setPlayerActive(playerId: String, active: Boolean)
    suspend fun deletePlayerPermanently(playerId: String)
}
