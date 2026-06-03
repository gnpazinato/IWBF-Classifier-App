package com.iwbfclassifier.ui.navigation

object Routes {
    const val CompetitionList = "competitions"
    const val CompetitionDetail = "competition/{competitionId}"
    const val TeamRoster = "competition/{competitionId}/team/{teamId}"
    const val PlayerEdit = "player/{playerId}"
    const val Observation = "competition/{competitionId}/observe"
    const val ImportNew = "import"
    const val ImportInto = "competition/{competitionId}/import"

    const val ARG_COMPETITION_ID = "competitionId"
    const val ARG_TEAM_ID = "teamId"
    const val ARG_PLAYER_ID = "playerId"

    fun competitionDetail(competitionId: String) = "competition/$competitionId"
    fun teamRoster(competitionId: String, teamId: String) = "competition/$competitionId/team/$teamId"
    fun playerEdit(playerId: String) = "player/$playerId"
    fun observation(competitionId: String) = "competition/$competitionId/observe"
    fun importInto(competitionId: String) = "competition/$competitionId/import"
}
