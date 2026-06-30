package com.primaraya.inspectra.core.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.primaraya.inspectra.core.data.local.InspectraDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncMasterDataWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = InspectraDatabase.getDatabase(context)
    // private val partDao = db.partDao()
    // private val materialDao = db.materialDao()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // TODO: Implement pull delta data from Supabase using driver
            // and insert into Room Dao.
            // val lastSync = partDao.getLastSyncTimestamp() ?: 0L
            // driver.getList("m_part", "terakhir_disinkronkan=gt.$lastSync") ...
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
