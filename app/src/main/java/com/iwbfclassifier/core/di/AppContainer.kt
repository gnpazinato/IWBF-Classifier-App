package com.iwbfclassifier.core.di

import android.content.Context
import com.iwbfclassifier.data.repository.CompetitionRepository
import com.iwbfclassifier.data.repository.JsonCompetitionRepository
import com.iwbfclassifier.data.storage.FileStorage

/**
 * Tiny manual DI container (chosen over Hilt for the MVP — no annotation
 * processing, simplest cloud builds). Migrate to Hilt later if it grows.
 */
class AppContainer(context: Context) {
    private val storage = FileStorage(context.applicationContext.filesDir)
    private val jsonRepository = JsonCompetitionRepository(storage)

    val competitionRepository: CompetitionRepository = jsonRepository

    /** Load persisted data into memory once at startup. */
    suspend fun initialize() {
        jsonRepository.load()
    }
}
