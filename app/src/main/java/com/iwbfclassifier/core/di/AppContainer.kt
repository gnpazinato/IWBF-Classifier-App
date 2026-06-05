package com.iwbfclassifier.core.di

import android.content.Context
import com.iwbfclassifier.core.backup.BackupManager
import com.iwbfclassifier.data.repository.CompetitionRepository
import com.iwbfclassifier.data.repository.JsonCompetitionRepository
import com.iwbfclassifier.data.storage.FileStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

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

    /** Write a full `.zip` backup of all local data to [out] (e.g. a Drive document). */
    suspend fun exportBackup(out: OutputStream) = withContext(Dispatchers.IO) {
        BackupManager.export(storage.root, out)
    }

    /** Restore data from a `.zip` backup [input], then reload everything into memory. */
    suspend fun importBackup(input: InputStream) {
        withContext(Dispatchers.IO) { BackupManager.import(storage.root, input) }
        jsonRepository.load()
    }
}
