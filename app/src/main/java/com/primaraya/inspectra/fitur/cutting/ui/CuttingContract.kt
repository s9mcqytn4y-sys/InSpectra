package com.primaraya.inspectra.fitur.cutting.ui

import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.fitur.checksheet.domain.SlotNg
import com.primaraya.inspectra.fitur.cutting.domain.InputBatchCutting
import com.primaraya.inspectra.fitur.cutting.domain.OpsiMaterialCutting
import com.primaraya.inspectra.fitur.cutting.domain.RingkasanHarianCutting
import com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto

object CuttingContract {
    data class State(
        val input: InputBatchCutting,
        val material: AsyncData<List<OpsiMaterialCutting>> = AsyncData.Idle,
        val defect: AsyncData<List<MasterDefectDto>> = AsyncData.Idle,
        val slotWaktu: List<SlotNg> = emptyList(),
        val ringkasan: AsyncData<List<RingkasanHarianCutting>> = AsyncData.Idle,
        val daftarPesanValidasi: List<String> = emptyList(),
        val menyimpan: Boolean = false,
        val pesan: String? = null
    )

    sealed interface Intent {
        data object Muat : Intent
        data class UbahInput(val input: InputBatchCutting) : Intent
        data class PilihMaterial(val material: OpsiMaterialCutting) : Intent
        data class TambahDefect(val defect: MasterDefectDto) : Intent
        data class UbahJumlahDefect(val idDefect: String, val jumlah: String) : Intent
        data class UbahPanjangDefect(val idDefect: String, val panjang: String) : Intent
        data class UbahSlotDefect(val idDefect: String, val idSlot: String?) : Intent
        data class HapusDefect(val idDefect: String) : Intent
        data object Simpan : Intent
        data object HapusPesan : Intent
    }
}
