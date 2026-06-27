package com.primaraya.inspectra.fitur.masterdata.data

import com.primaraya.inspectra.core.data.PageRequest
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.fitur.checksheet.domain.PartPickerItem
import com.primaraya.inspectra.fitur.masterdata.domain.*

interface MasterDataRepository {
    suspend fun healthCheck(): NetworkResult<Unit>

    suspend fun getChecksheetData(komoditas: String): NetworkResult<List<ChecksheetPartDefectViewDto>>

    suspend fun getPartPickerItems(tipeProses: String): NetworkResult<List<PartPickerItem>>

    suspend fun getPartsPage(
        page: PageRequest,
        filter: FilterDataInduk
    ): NetworkResult<List<MasterPartDto>>
    suspend fun getSuppliersPage(page: PageRequest): NetworkResult<List<MasterSupplierDto>>
    suspend fun getMaterialsPage(page: PageRequest): NetworkResult<List<MasterMaterialDto>>
    suspend fun getDefectsPage(page: PageRequest): NetworkResult<List<MasterDefectDto>>

    suspend fun getSlotWaktu(tipeProses: String): NetworkResult<List<MasterSlotWaktuDto>>

    suspend fun upsertSupplier(supplier: MasterSupplierDto): NetworkResult<Unit>
    suspend fun deleteSupplierSoft(id: String): NetworkResult<Unit>

    suspend fun upsertPart(part: MasterPartDto): NetworkResult<Unit>
    suspend fun deletePartSoft(id: String): NetworkResult<Unit>
    suspend fun uploadPartImage(uniqNo: String, file: java.io.File): NetworkResult<String>

    suspend fun upsertMaterial(material: MasterMaterialDto): NetworkResult<Unit>
    suspend fun deleteMaterialSoft(id: String): NetworkResult<Unit>

    suspend fun upsertDefect(defect: MasterDefectDto): NetworkResult<Unit>
    suspend fun deleteDefectSoft(idDefect: String): NetworkResult<Unit>

    suspend fun getPartDefects(uniqNo: String): NetworkResult<List<MasterPartDefectDto>>
    suspend fun getPartEffectiveDefects(uniqNo: String): NetworkResult<List<MasterPartEffectiveDefectDto>>
    suspend fun upsertPartDefect(data: MasterPartDefectDto): NetworkResult<Unit>
    suspend fun deletePartDefect(id: String): NetworkResult<Unit>

    suspend fun getPartMaterials(uniqNo: String): NetworkResult<List<MasterPartMaterialDto>>
    suspend fun upsertPartMaterial(data: MasterPartMaterialDto): NetworkResult<Unit>
    suspend fun deletePartMaterial(id: String): NetworkResult<Unit>
    suspend fun getMaterialUsages(materialId: String): NetworkResult<List<MasterPartMaterialDto>>
    suspend fun getMaterialDefects(materialId: String): NetworkResult<List<MasterMaterialDefectDto>>
    suspend fun upsertMaterialDefect(data: MasterMaterialDefectDto): NetworkResult<Unit>
    suspend fun deleteMaterialDefect(id: String): NetworkResult<Unit>
}
