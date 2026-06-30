package com.primaraya.inspectra.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.primaraya.inspectra.core.data.local.entity.ChecksheetQueueEntity

@Dao
interface ChecksheetQueueDao {
    @Insert
    suspend fun insert(entity: ChecksheetQueueEntity): Long

    @Query("SELECT * FROM checksheet_queue WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY createdAt ASC")
    suspend fun getPendingQueue(): List<ChecksheetQueueEntity>

    @Update
    suspend fun update(entity: ChecksheetQueueEntity)

    @Query("UPDATE checksheet_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM checksheet_queue WHERE id = :id")
    suspend fun delete(id: Long)
}
