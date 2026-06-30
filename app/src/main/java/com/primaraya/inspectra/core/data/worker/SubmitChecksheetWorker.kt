package com.primaraya.inspectra.core.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.primaraya.inspectra.core.data.local.InspectraDatabase
import com.primaraya.inspectra.fitur.checksheet.data.SupabaseChecksheetRepository
import com.primaraya.inspectra.fitur.checksheet.domain.PayloadChecksheet
import com.primaraya.inspectra.core.network.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SubmitChecksheetWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = InspectraDatabase.getDatabase(context)
    private val checksheetQueueDao = db.checksheetQueueDao()
    private val repository = SupabaseChecksheetRepository()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pendingItems = checksheetQueueDao.getPendingQueue()
        if (pendingItems.isEmpty()) return@withContext Result.success()

        var hasFailure = false

        for (item in pendingItems) {
            try {
                // Update status to processing
                checksheetQueueDao.updateStatus(item.id, "PROCESSING")

                val payload = json.decodeFromString(PayloadChecksheet.serializer(), item.payloadJson)
                val result = repository.submitChecksheet(payload)

                if (result is NetworkResult.Success) {
                    checksheetQueueDao.delete(item.id)
                } else {
                    hasFailure = true
                    checksheetQueueDao.update(
                        item.copy(
                            status = "FAILED",
                            retryCount = item.retryCount + 1
                        )
                    )
                }
            } catch (e: Exception) {
                hasFailure = true
                checksheetQueueDao.update(
                    item.copy(
                        status = "FAILED",
                        retryCount = item.retryCount + 1
                    )
                )
            }
        }

        if (hasFailure) Result.retry() else Result.success()
    }
}
