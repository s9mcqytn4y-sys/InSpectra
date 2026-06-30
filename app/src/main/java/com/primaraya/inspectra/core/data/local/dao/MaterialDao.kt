package com.primaraya.inspectra.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.primaraya.inspectra.core.data.local.entity.MaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Query("SELECT * FROM m_material")
    fun getAllMaterials(): Flow<List<MaterialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterials(materials: List<MaterialEntity>)

    @Query("SELECT MAX(terakhirDisinkronkan) FROM m_material")
    suspend fun getLastSyncTimestamp(): Long?
}
