package com.iwbfclassifier.data.model

import kotlinx.serialization.Serializable

/** Local data model — mirrors docs/04_data_model.md. All fields editable (docs/05). */

@Serializable
data class Competition(
    val id: String,
    val name: String,
    val location: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class Team(
    val id: String,
    val competitionId: String,
    val name: String,
    val code: String? = null,
    val gender: String? = null,
    val active: Boolean = true,
    val source: SourceInfo? = null,
)

/** Team name with its W/M gender suffix for display, e.g. "Argentina W". */
fun Team.displayName(): String = if (gender.isNullOrBlank()) name else "$name $gender"

@Serializable
data class Player(
    val id: String,
    val competitionId: String,
    val teamId: String,
    val uniformNumber: String? = null,
    val name: String? = null,
    val iwbfId: String? = null,
    val dateOfBirth: String? = null,
    val importedSportClass: SportClass? = null,
    // The athlete's single official Sport Class + Status (from import or manual entry).
    // [startingSportClass] + [sportClassStatus] are shown/edited on the roster preview, the
    // Edit Player screen, and the observation "Initial" line — they are the same value.
    val sportClassStatus: SportClassStatus? = null,
    val startingSportClass: SportClass? = null,
    // Working values that only live on the Observation screen (each its own class + status).
    val myOpinionSportClass: SportClass? = null,
    val myOpinionSportClassStatus: SportClassStatus? = null,
    val finalSportClass: SportClass? = null,
    val finalSportClassStatus: SportClassStatus? = null,
    val observationStatus: ObservationStatus = ObservationStatus.NotObserved,
    val mic: MicInfo = MicInfo(),
    val videoEvidence: List<VideoEvidence> = emptyList(),
    val active: Boolean = true,
    val source: SourceInfo? = null,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * A YouTube moment attached to a Player (docs/06). A one-tap "Flag Moment" captures
 * the current player time and stores a slow-motion window around it: [startSeconds]
 * (≈ tap − 5s), [endSeconds] (≈ tap + 5s) and [playbackRate] (0.5x by default). The
 * embedded player replays exactly this window; [url] is a plain link fallback.
 */
@Serializable
data class VideoEvidence(
    val id: String,
    val url: String,
    val videoId: String? = null,
    val startSeconds: Int? = null,
    val endSeconds: Int? = null,
    val playbackRate: Double = 1.0,
    val label: String? = null,
    val createdAt: String,
)

@Serializable
data class MicInfo(
    val healthCondition: String? = null,
    val impairment: String? = null,
    val notes: String? = null,
    val panel: String? = null,
)

@Serializable
data class SourceInfo(
    val type: String,
    val fileName: String? = null,
    val confidence: Double? = null,
)

@Serializable
data class Game(
    val id: String,
    val competitionId: String,
    val name: String,
    val teamAId: String? = null,
    val teamBId: String? = null,
    val date: String? = null,
    val youtube: YoutubeInfo? = null,
)

@Serializable
data class YoutubeInfo(
    val enabled: Boolean = false,
    val url: String? = null,
    val videoId: String? = null,
    val streamDelaySeconds: Int? = null,
    val delayCalibrated: Boolean = false,
)

@Serializable
data class GameClock(
    val period: String? = null,
    val clock: String? = null,
)

@Serializable
data class ObservationEvent(
    val id: String,
    val competitionId: String,
    val gameId: String,
    val playerId: String,
    val type: String,
    val createdAtDeviceTime: String,
    val gameClock: GameClock? = null,
    val videoMarkerId: String? = null,
    val freehandNoteRef: String? = null,
)

@Serializable
data class VideoMarker(
    val id: String,
    val competitionId: String,
    val gameId: String,
    val playerId: String,
    val source: String,
    val videoId: String? = null,
    val startSeconds: Double? = null,
    val endSeconds: Double? = null,
    val playbackRate: Double = 1.0,
    val label: String? = null,
    val linkedFromEventId: String? = null,
    val createdAt: String,
)

@Serializable
data class Attachment(
    val id: String,
    val competitionId: String,
    val gameId: String? = null,
    val playerId: String,
    val source: String,
    val fileName: String,
    val durationSeconds: Double? = null,
    val label: String? = null,
    val createdAt: String,
)
