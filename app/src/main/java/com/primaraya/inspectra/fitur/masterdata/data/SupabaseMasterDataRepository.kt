package com.primaraya.inspectra.fitur.masterdata.data

import com.primaraya.inspectra.core.database.SupabaseRestClient
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.network.runNetworkCatching
import com.primaraya.inspectra.fitur.masterdata.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseMasterDataRepository(
    private val client: SupabaseRestClient = SupabaseRestClient()
) : MasterDataRepository {

    override suspend fun getChecksheetData(
        komoditas: String
    ): NetworkResult<List<ChecksheetPartDefectViewDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.getList<List<ChecksheetPartDefectViewDto>>(
                table = "v_checksheet_part_defect",
                query = "select=*&komoditas=eq.$komoditas&order=uniq_no.asc"
            )
        }
    }

    override suspend fun getSuppliers(): NetworkResult<List<MasterSupplierDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.getList<List<MasterSupplierDto>>(
                table = "m_supplier",
                query = "select=*&aktif=eq.true&order=nama_supplier.asc"
            )
        }
    }

    override suspend fun upsertSupplier(supplier: MasterSupplierDto): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.upsert(table = "m_supplier", body = supplier)
        }
    }

    override suspend fun deleteSupplierSoft(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.softDelete(table = "m_supplier", idColumn = "id", id = id)
        }
    }

    override suspend fun getParts(): NetworkResult<List<MasterPartDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.getList<List<MasterPartDto>>(
                table = "m_part",
                query = "select=*&aktif=eq.true&order=uniq_no.asc&limit=100"
            )
        }
    }

    override suspend fun upsertPart(part: MasterPartDto): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.upsert(table = "m_part", body = part)
        }
    }

    override suspend fun deletePartSoft(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.softDelete(table = "m_part", idColumn = "id", id = id)
        }
    }

    override suspend fun getMaterials(): NetworkResult<List<MasterMaterialDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.getList<List<MasterMaterialDto>>(
                table = "m_material",
                query = "select=*&aktif=eq.true&order=nama_material.asc&limit=100"
            )
        }
    }

    override suspend fun upsertMaterial(material: MasterMaterialDto): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.upsert(table = "m_material", body = material)
        }
    }

    override suspend fun deleteMaterialSoft(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.softDelete(table = "m_material", idColumn = "id", id = id)
        }
    }

    override suspend fun getDefects(): NetworkResult<List<MasterDefectDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.getList<List<MasterDefectDto>>(
                table = "m_defect",
                query = "select=*&aktif=eq.true&order=nama_defect.asc&limit=100"
            )
        }
    }

    override suspend fun upsertDefect(defect: MasterDefectDto): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.upsert(table = "m_defect", body = defect)
        }
    }

    override suspend fun deleteDefectSoft(idDefect: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.softDelete(table = "m_defect", idColumn = "id_defect", id = idDefect)
        }
    }

    override suspend fun getPartDefects(uniqNo: String): NetworkResult<List<MasterPartDefectDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.getList<List<MasterPartDefectDto>>(
                table = "m_part_defect",
                query = "select=*&uniq_no=eq.$uniqNo&aktif=eq.true&order=urutan.asc"
            )
        }
    }

    override suspend fun upsertPartDefect(data: MasterPartDefectDto): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching { client.upsert("m_part_defect", data) }
    }

    override suspend fun deletePartDefect(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching { client.softDelete("m_part_defect", "id", id) }
    }

    override suspend fun getPartMaterials(uniqNo: String): NetworkResult<List<MasterPartMaterialDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.getList<List<MasterPartMaterialDto>>(
                table = "m_part_material",
                query = "select=*&uniq_no=eq.$uniqNo&aktif=eq.true&order=urutan.asc"
            )
        }
    }

    override suspend fun upsertPartMaterial(data: MasterPartMaterialDto): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching { client.upsert("m_part_material", data) }
    }

    override suspend fun deletePartMaterial(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching { client.softDelete("m_part_material", "id", id) }
    }
}
