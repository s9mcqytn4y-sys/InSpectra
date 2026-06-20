package com.primaraya.inspectra.fitur.masterdata.data

import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.fitur.masterdata.domain.*

interface MasterDataRepository {
    suspend fun getChecksheetData(komoditas: String): NetworkResult<List<ChecksheetPartDefectViewDto>>

    suspend fun getSuppliers(): NetworkResult<List<MasterSupplierDto>>
    suspend fun upsertSupplier(supplier: MasterSupplierDto): NetworkResult<Unit>
    suspend fun deleteSupplierSoft(id: String): NetworkResult<Unit>

    suspend fun getParts(): NetworkResult<List<MasterPartDto>>
    suspend fun upsertPart(part: MasterPartDto): NetworkResult<Unit>
    suspend fun deletePartSoft(id: String): NetworkResult<Unit>

    suspend fun getMaterials(): NetworkResult<List<MasterMaterialDto>>
    suspend fun upsertMaterial(material: MasterMaterialDto): NetworkResult<Unit>
    suspend fun deleteMaterialSoft(id: String): NetworkResult<Unit>

    suspend fun getDefects(): NetworkResult<List<MasterDefectDto>>
    suspend fun upsertDefect(defect: MasterDefectDto): NetworkResult<Unit>
    suspend fun deleteDefectSoft(idDefect: String): NetworkResult<Unit>

    suspend fun getPartDefects(uniqNo: String): NetworkResult<List<MasterPartDefectDto>>
    suspend fun upsertPartDefect(data: MasterPartDefectDto): NetworkResult<Unit>
    suspend fun deletePartDefect(id: String): NetworkResult<Unit>

    suspend fun getPartMaterials(uniqNo: String): NetworkResult<List<MasterPartMaterialDto>>
    suspend fun upsertPartMaterial(data: MasterPartMaterialDto): NetworkResult<Unit>
    suspend fun deletePartMaterial(id: String): NetworkResult<Unit>
}
