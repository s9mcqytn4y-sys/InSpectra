package com.primaraya.inspectra.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.primaraya.inspectra.core.data.local.entity.PartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartDao {
    @Query("SELECT * FROM m_part")
    fun getAllParts(): Flow<List<PartEntity>>

    @Query("SELECT * FROM m_part WHERE komoditas = :komoditas")
    fun getPartsByKomoditas(komoditas: String): Flow<List<PartEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParts(parts: List<PartEntity>)

    @Query("SELECT MAX(terakhirDisinkronkan) FROM m_part")
    suspend fun getLastSyncTimestamp(): Long?
}
