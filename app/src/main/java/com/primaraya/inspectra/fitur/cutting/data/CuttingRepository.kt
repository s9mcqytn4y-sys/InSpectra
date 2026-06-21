package com.primaraya.inspectra.fitur.cutting.data

import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.fitur.cutting.domain.InputBatchCutting
import com.primaraya.inspectra.fitur.cutting.domain.OpsiMaterialCutting
import com.primaraya.inspectra.fitur.cutting.domain.OpsiPartUkuranCutting
import com.primaraya.inspectra.fitur.cutting.domain.RingkasanHarianCutting

interface CuttingRepository {
    suspend fun bacaOpsiMaterial(): NetworkResult<List<OpsiMaterialCutting>>
    suspend fun bacaOpsiPartUkuran(): NetworkResult<List<OpsiPartUkuranCutting>>
    suspend fun simpanBatch(input: InputBatchCutting): NetworkResult<String>
    suspend fun bacaRingkasanHarian(tanggalPemeriksaan: String): NetworkResult<List<RingkasanHarianCutting>>
}
