package com.primaraya.inspectra.masterdata.data

import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.masterdata.domain.MasterDefectDto
import com.primaraya.inspectra.masterdata.domain.MasterMaterialDto
import com.primaraya.inspectra.masterdata.domain.MasterPartDto

interface MasterDataRepository {
    suspend fun getParts(): NetworkResult<List<MasterPartDto>>
    suspend fun upsertPart(part: MasterPartDto): NetworkResult<Unit>
    suspend fun deletePartSoft(id: String): NetworkResult<Unit>

    suspend fun getMaterials(): NetworkResult<List<MasterMaterialDto>>
    suspend fun upsertMaterial(material: MasterMaterialDto): NetworkResult<Unit>
    suspend fun deleteMaterialSoft(id: String): NetworkResult<Unit>

    suspend fun getDefects(): NetworkResult<List<MasterDefectDto>>
    suspend fun upsertDefect(defect: MasterDefectDto): NetworkResult<Unit>
    suspend fun deleteDefectSoft(idDefect: String): NetworkResult<Unit>
}
