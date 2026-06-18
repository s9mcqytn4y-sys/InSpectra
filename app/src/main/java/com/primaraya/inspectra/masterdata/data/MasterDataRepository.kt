package com.primaraya.inspectra.masterdata.data

import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.masterdata.domain.MasterPartDto
import com.primaraya.inspectra.masterdata.domain.MasterMaterialDto

interface MasterDataRepository {
    suspend fun getParts(): NetworkResult<List<MasterPartDto>>
    suspend fun getMaterials(): NetworkResult<List<MasterMaterialDto>>
    suspend fun checkHealth(): NetworkResult<Boolean>
}
