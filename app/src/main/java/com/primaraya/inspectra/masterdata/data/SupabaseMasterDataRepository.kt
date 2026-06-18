package com.primaraya.inspectra.masterdata.data

import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.network.SupabaseProvider
import com.primaraya.inspectra.core.network.runNetworkCatching
import com.primaraya.inspectra.masterdata.domain.MasterMaterialDto
import com.primaraya.inspectra.masterdata.domain.MasterPartDto
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseMasterDataRepository : MasterDataRepository {
    private val client = SupabaseProvider.client

    override suspend fun getParts(): NetworkResult<List<MasterPartDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.postgrest["m_part"].select().decodeList<MasterPartDto>()
        }
    }

    override suspend fun getMaterials(): NetworkResult<List<MasterMaterialDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.postgrest["m_material"].select().decodeList<MasterMaterialDto>()
        }
    }

    override suspend fun checkHealth(): NetworkResult<Boolean> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            client.postgrest["m_part"].select {
                limit(1)
            }
            true
        }
    }
}
