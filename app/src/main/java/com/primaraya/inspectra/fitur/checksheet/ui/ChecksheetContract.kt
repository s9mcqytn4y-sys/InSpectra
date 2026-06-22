package com.primaraya.inspectra.fitur.checksheet.ui

import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.fitur.checksheet.domain.PartPickerItem
import com.primaraya.inspectra.fitur.checksheet.domain.PayloadChecksheet
import com.primaraya.inspectra.fitur.checksheet.domain.RingkasanPartChecksheet
import com.primaraya.inspectra.fitur.checksheet.domain.TipeProses

/**
 * Kontrak MVI untuk E-Checksheet.
 */
object ChecksheetContract {

    /**
     * State tunggal layar checksheet.
     */
    data class State(
        val mengirim: Boolean = false,
        val tipeProses: TipeProses = TipeProses.PRESS,
        val step: Step = Step.PILIH_PART,
        val dataPicker: AsyncData<List<PartPickerItem>> = AsyncData.Idle,
        val dataChecksheet: AsyncData<List<RingkasanPartChecksheet>> = AsyncData.Idle,
        val partTerpilih: Set<String> = emptySet(),
        val pencarian: String = "",
        val preview: PayloadChecksheet? = null
    ) {
        enum class Step {
            PILIH_PART,
            ISI_FORM
        }

        val daftarPart: List<RingkasanPartChecksheet>
            get() = (dataChecksheet as? AsyncData.Success)?.data ?: emptyList()

        val totalDiperiksa: Int get() = daftarPart.sumOf { it.jumlahDiperiksa }
        val totalNg: Int get() = daftarPart.sumOf { it.jumlahNg }
        val totalOk: Int get() = (totalDiperiksa - totalNg).coerceAtLeast(0)
        val rasioNg: Float get() = if (totalDiperiksa > 0) totalNg.toFloat() / totalDiperiksa * 100f else 0f
        val adaInput: Boolean get() = daftarPart.any { it.jumlahDiperiksa > 0 || it.jumlahNg > 0 }
        val adaQtyTidakValid: Boolean get() = daftarPart.any { it.kuantitasTidakValid }
        val adaSlotTidakMatch: Boolean get() = daftarPart.any { p -> p.daftarDefect.any { !it.slotMatch } }
        
        val pickerFiltered: List<PartPickerItem>
            get() = (dataPicker as? AsyncData.Success)?.data?.filter {
                it.uniq_no.contains(pencarian, ignoreCase = true) || 
                it.nama_part.contains(pencarian, ignoreCase = true)
            } ?: emptyList()
    }

    /**
     * Intent dari UI checksheet.
     */
    sealed interface Intent {
        data class Muat(val tipeProses: TipeProses) : Intent
        data class CariPart(val query: String) : Intent
        data class PilihPart(val uniqNo: String, val pilih: Boolean) : Intent
        data object LanjutKeForm : Intent
        data object KembaliKePicker : Intent

        data class TogglePart(val uniqNo: String) : Intent
        data class UbahJumlahDiperiksa(val uniqNo: String, val jumlah: Int) : Intent
        data class UbahJumlahDefect(val uniqNo: String, val idDefect: String, val jumlah: Int) : Intent
        data class UbahJumlahSlotDefect(val uniqNo: String, val idDefect: String, val slotId: String, val jumlah: Int) : Intent
        data class TambahDefect(val uniqNo: String, val idDefect: String) : Intent
        data class KurangiDefect(val uniqNo: String, val idDefect: String) : Intent

        data object Tinjau : Intent
        data object TutupPreview : Intent
        data object Kirim : Intent
        data object Retry : Intent
    }

    /**
     * Efek sekali jalan untuk feedback UI.
     */
    sealed interface Effect {
        data class PesanSukses(val pesan: String) : Effect
        data class PesanError(val judul: String, val pesan: String) : Effect
        data class KirimBerhasil(val idSesi: String) : Effect
    }
}
