package com.iwbfclassifier.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.iwbfclassifier.ui.competition.CompetitionDetailScreen
import com.iwbfclassifier.ui.competition.CompetitionListScreen
import com.iwbfclassifier.ui.imports.ImportScreen
import com.iwbfclassifier.ui.observation.ObservationScreen
import com.iwbfclassifier.ui.roster.PlayerEditScreen
import com.iwbfclassifier.ui.roster.RosterOverviewScreen
import com.iwbfclassifier.ui.roster.TeamRosterScreen

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.CompetitionList) {
        composable(Routes.CompetitionList) {
            CompetitionListScreen(
                onOpenCompetition = { id -> nav.navigate(Routes.competitionDetail(id)) },
                onImport = { nav.navigate(Routes.ImportNew) },
            )
        }
        composable(Routes.ImportNew) {
            ImportScreen(
                competitionId = null,
                onBack = { nav.popBackStack() },
                onImported = { cid ->
                    nav.navigate(Routes.competitionDetail(cid)) {
                        popUpTo(Routes.CompetitionList) { inclusive = false }
                    }
                },
            )
        }
        composable(
            Routes.ImportInto,
            arguments = listOf(navArgument(Routes.ARG_COMPETITION_ID) { type = NavType.StringType }),
        ) { entry ->
            val competitionId = entry.arguments?.getString(Routes.ARG_COMPETITION_ID).orEmpty()
            ImportScreen(
                competitionId = competitionId,
                onBack = { nav.popBackStack() },
                onImported = { nav.popBackStack() },
            )
        }
        composable(
            Routes.CompetitionDetail,
            arguments = listOf(navArgument(Routes.ARG_COMPETITION_ID) { type = NavType.StringType }),
        ) { entry ->
            val competitionId = entry.arguments?.getString(Routes.ARG_COMPETITION_ID).orEmpty()
            CompetitionDetailScreen(
                competitionId = competitionId,
                onBack = { nav.popBackStack() },
                onOpenTeam = { teamId -> nav.navigate(Routes.teamRoster(competitionId, teamId)) },
                onOpenObservation = { nav.navigate(Routes.observation(competitionId)) },
                onOpenImport = { nav.navigate(Routes.importInto(competitionId)) },
                onOpenOverview = { nav.navigate(Routes.rosterOverview(competitionId)) },
            )
        }
        composable(
            Routes.TeamRoster,
            arguments = listOf(
                navArgument(Routes.ARG_COMPETITION_ID) { type = NavType.StringType },
                navArgument(Routes.ARG_TEAM_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val competitionId = entry.arguments?.getString(Routes.ARG_COMPETITION_ID).orEmpty()
            val teamId = entry.arguments?.getString(Routes.ARG_TEAM_ID).orEmpty()
            TeamRosterScreen(
                competitionId = competitionId,
                teamId = teamId,
                onBack = { nav.popBackStack() },
                onOpenPlayer = { playerId -> nav.navigate(Routes.playerEdit(playerId)) },
            )
        }
        composable(
            Routes.PlayerEdit,
            arguments = listOf(navArgument(Routes.ARG_PLAYER_ID) { type = NavType.StringType }),
        ) { entry ->
            val playerId = entry.arguments?.getString(Routes.ARG_PLAYER_ID).orEmpty()
            PlayerEditScreen(playerId = playerId, onBack = { nav.popBackStack() })
        }
        composable(
            Routes.Observation,
            arguments = listOf(navArgument(Routes.ARG_COMPETITION_ID) { type = NavType.StringType }),
        ) { entry ->
            val competitionId = entry.arguments?.getString(Routes.ARG_COMPETITION_ID).orEmpty()
            ObservationScreen(
                competitionId = competitionId,
                onBack = { nav.popBackStack() },
                onOpenPlayer = { playerId -> nav.navigate(Routes.playerEdit(playerId)) },
            )
        }
        composable(
            Routes.RosterOverview,
            arguments = listOf(navArgument(Routes.ARG_COMPETITION_ID) { type = NavType.StringType }),
        ) { entry ->
            val competitionId = entry.arguments?.getString(Routes.ARG_COMPETITION_ID).orEmpty()
            RosterOverviewScreen(
                competitionId = competitionId,
                onBack = { nav.popBackStack() },
                onOpenPlayer = { playerId -> nav.navigate(Routes.playerEdit(playerId)) },
            )
        }
    }
}
