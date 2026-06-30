package com.primaraya.inspectra.core.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParts(parts: List<PartEntity>)

    @Query("SELECT * FROM m_part_local WHERE aktif = 1 ORDER BY uniq_no ASC")
    fun getAllParts(): Flow<List<PartEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterials(materials: List<MaterialEntity>)

    @Query("SELECT * FROM m_material_local ORDER BY nama_material ASC")
    fun getAllMaterials(): Flow<List<MaterialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefects(defects: List<DefectEntity>)

    @Query("SELECT * FROM m_defect_local WHERE aktif = 1 ORDER BY nama_defect ASC")
    fun getAllDefects(): Flow<List<DefectEntity>>
}
