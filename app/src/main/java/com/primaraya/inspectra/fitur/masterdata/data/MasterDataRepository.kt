package com.primaraya.inspectra.fitur.masterdata.data

import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.fitur.masterdata.domain.ChecksheetPartDefectViewDto
import com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto
import com.primaraya.inspectra.fitur.masterdata.domain.MasterMaterialDto
import com.primaraya.inspectra.fitur.masterdata.domain.MasterPartDto

interface MasterDataRepository {
    suspend fun getChecksheetData(komoditas: String): NetworkResult<List<ChecksheetPartDefectViewDto>>

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
