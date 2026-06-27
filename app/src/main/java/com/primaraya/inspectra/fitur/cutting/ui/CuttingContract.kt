package com.primaraya.inspectra.fitur.cutting.ui

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.fitur.checksheet.domain.SlotNg
import com.primaraya.inspectra.fitur.cutting.domain.InputBatchCutting
import com.primaraya.inspectra.fitur.cutting.domain.OpsiMaterialCutting
import com.primaraya.inspectra.fitur.cutting.domain.OpsiPartUkuranCutting
import com.primaraya.inspectra.fitur.cutting.domain.RingkasanHarianCutting
import com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto

object CuttingContract {
    @Immutable
    data class State(
        val input: InputBatchCutting,
        val material: AsyncData<ImmutableList<OpsiMaterialCutting>> = AsyncData.Idle,
        val partUkuran: AsyncData<ImmutableList<OpsiPartUkuranCutting>> = AsyncData.Idle,
        val defect: AsyncData<ImmutableList<MasterDefectDto>> = AsyncData.Idle,
        val slotWaktu: ImmutableList<SlotNg> = persistentListOf(),
        val ringkasan: AsyncData<ImmutableList<RingkasanHarianCutting>> = AsyncData.Idle,
        val daftarPesanValidasi: ImmutableList<String> = persistentListOf(),
        val menampilkanPreview: Boolean = false,
        val menyimpan: Boolean = false,
        val berhasil: Boolean = false, // Success state
        val pesan: String? = null
    )

    sealed interface Intent {
        data object Muat : Intent
        data class UbahInput(val input: InputBatchCutting) : Intent
        data class PilihMaterial(val material: OpsiMaterialCutting) : Intent
        data class PilihPartAcuanUkuran(val part: OpsiPartUkuranCutting) : Intent
        data class TambahDefect(val defect: MasterDefectDto) : Intent
        data class UbahJumlahDefect(val idDefect: String, val jumlah: String) : Intent
        data class UbahPanjangDefect(val idDefect: String, val panjang: String) : Intent
        data class UbahSlotDefect(val idDefect: String, val idSlot: String?) : Intent
        data class HapusDefect(val idDefect: String) : Intent
        data object BukaPreview : Intent
        data object TutupPreview : Intent
        data object Simpan : Intent
        data object TutupBerhasil : Intent
        data object HapusPesan : Intent
    }
}
