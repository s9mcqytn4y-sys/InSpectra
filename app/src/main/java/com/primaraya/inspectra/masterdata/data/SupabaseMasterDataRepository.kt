package com.primaraya.inspectra.masterdata.data

import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.network.SupabaseProvider
import com.primaraya.inspectra.core.network.runNetworkCatching
import com.primaraya.inspectra.masterdata.domain.ChecksheetPartDefectViewDto
import com.primaraya.inspectra.masterdata.domain.MasterDefectDto
import com.primaraya.inspectra.masterdata.domain.MasterMaterialDto
import com.primaraya.inspectra.masterdata.domain.MasterPartDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseMasterDataRepository : MasterDataRepository {

    private val client = SupabaseProvider.client

    override suspend fun getChecksheetData(
        komoditas: String
    ): NetworkResult<List<ChecksheetPartDefectViewDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.postgrest["v_checksheet_part_defect"]
                .select {
                    filter {
                        eq("komoditas", komoditas)
                    }
                }
                .decodeList<ChecksheetPartDefectViewDto>()
        }
    }

    override suspend fun getParts(): NetworkResult<List<MasterPartDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.postgrest["m_part"]
                .select {
                    filter { eq("aktif", true) }
                    order("uniq_no", Order.ASCENDING)
                }
                .decodeList<MasterPartDto>()
        }
    }

    override suspend fun upsertPart(part: MasterPartDto): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.postgrest["m_part"].upsert(part)
            Unit
        }
    }

    override suspend fun deletePartSoft(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.postgrest["m_part"]
                .update(mapOf("aktif" to false)) {
                    filter { eq("id", id) }
                }
            Unit
        }
    }

    override suspend fun getMaterials(): NetworkResult<List<MasterMaterialDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.postgrest["m_material"]
                .select {
                    filter { eq("aktif", true) }
                    order("nama_material", Order.ASCENDING)
                }
                .decodeList<MasterMaterialDto>()
        }
    }

    override suspend fun upsertMaterial(material: MasterMaterialDto): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.postgrest["m_material"].upsert(material)
            Unit
        }
    }

    override suspend fun deleteMaterialSoft(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.postgrest["m_material"]
                .update(mapOf("aktif" to false)) {
                    filter { eq("id", id) }
                }
            Unit
        }
    }

    override suspend fun getDefects(): NetworkResult<List<MasterDefectDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.postgrest["m_defect"]
                .select {
                    filter { eq("aktif", true) }
                    order("nama_defect", Order.ASCENDING)
                }
                .decodeList<MasterDefectDto>()
        }
    }

    override suspend fun upsertDefect(defect: MasterDefectDto): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.postgrest["m_defect"].upsert(defect)
            Unit
        }
    }

    override suspend fun deleteDefectSoft(idDefect: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.postgrest["m_defect"]
                .update(mapOf("aktif" to false)) {
                    filter { eq("id_defect", idDefect) }
                }
            Unit
        }
    }
}
