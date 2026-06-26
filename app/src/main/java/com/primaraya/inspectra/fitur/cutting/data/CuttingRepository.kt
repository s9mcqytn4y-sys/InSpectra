package com.primaraya.inspectra.fitur.cutting.data

import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.fitur.cutting.domain.InputBatchCutting
import com.primaraya.inspectra.fitur.cutting.domain.OpsiMaterialCutting
import com.primaraya.inspectra.fitur.cutting.domain.OpsiPartUkuranCutting
import com.primaraya.inspectra.fitur.cutting.domain.RingkasanHarianCutting
import com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto
import com.primaraya.inspectra.fitur.masterdata.domain.MasterSlotWaktuDto

@kotlinx.serialization.Serializable
data class BootstrapCutting(
    val slot_waktu: List<MasterSlotWaktuDto>,
    val material_option: List<OpsiMaterialCutting>,
    val part_size_option: List<OpsiPartUkuranCutting>,
    val defect_option: List<MasterDefectDto>
)

interface CuttingRepository {
    suspend fun bacaBootstrap(): NetworkResult<BootstrapCutting>
    suspend fun bacaOpsiMaterial(): NetworkResult<List<OpsiMaterialCutting>>
    suspend fun bacaOpsiPartUkuran(): NetworkResult<List<OpsiPartUkuranCutting>>
    suspend fun simpanBatch(input: InputBatchCutting): NetworkResult<String>
    suspend fun bacaRingkasanHarian(tanggalPemeriksaan: String): NetworkResult<List<RingkasanHarianCutting>>
}
