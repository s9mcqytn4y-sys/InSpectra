package com.primaraya.inspectra.fitur.masterdata.data

import com.primaraya.inspectra.core.database.SupabaseRestClient
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.network.runNetworkCatching
import com.primaraya.inspectra.fitur.masterdata.domain.ChecksheetPartDefectViewDto
import com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto
import com.primaraya.inspectra.fitur.masterdata.domain.MasterMaterialDto
import com.primaraya.inspectra.fitur.masterdata.domain.MasterPartDto
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
                query = "komoditas=eq.$komoditas"
            )
        }
    }

    override suspend fun getParts(): NetworkResult<List<MasterPartDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.getList<List<MasterPartDto>>(
                table = "m_part",
                query = "aktif=eq.true&order=uniq_no.asc"
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
                query = "aktif=eq.true&order=nama_material.asc"
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
                query = "aktif=eq.true&order=nama_defect.asc"
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
}
