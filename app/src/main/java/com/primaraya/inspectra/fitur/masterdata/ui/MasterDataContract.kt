package com.primaraya.inspectra.fitur.masterdata.ui

import com.primaraya.inspectra.core.ui.UserMessage
import com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto
import com.primaraya.inspectra.fitur.masterdata.domain.MasterMaterialDto
import com.primaraya.inspectra.fitur.masterdata.domain.MasterPartDto

/**
 * Kontrak MVI untuk layar Master Data.
 */
object MasterDataContract {

    /**
     * Tab aktif pada Master Data.
     */
    enum class TabMasterData {
        PART,
        MATERIAL,
        DEFECT
    }

    /**
     * State tunggal yang dirender oleh MasterDataScreen.
     */
    data class State(
        val loading: Boolean = false,
        val menyimpan: Boolean = false,
        val tabAktif: TabMasterData = TabMasterData.PART,
        val daftarPart: List<MasterPartDto> = emptyList(),
        val daftarMaterial: List<MasterMaterialDto> = emptyList(),
        val daftarDefect: List<MasterDefectDto> = emptyList(),
        val kataKunci: String = "",
        val dialogForm: DialogForm? = null,
        val userMessage: UserMessage? = null
    )

    /**
     * Intent dari UI ke ViewModel.
     */
    sealed interface Intent {
        data object MuatAwal : Intent
        data class PilihTab(val tab: TabMasterData) : Intent
        data class Cari(val keyword: String) : Intent

        data object TambahPart : Intent
        data class EditPart(val data: MasterPartDto) : Intent
        data class SimpanPart(val data: MasterPartDto) : Intent
        data class HapusPart(val data: MasterPartDto) : Intent

        data object TambahMaterial : Intent
        data class EditMaterial(val data: MasterMaterialDto) : Intent
        data class SimpanMaterial(val data: MasterMaterialDto) : Intent
        data class HapusMaterial(val data: MasterMaterialDto) : Intent

        data object TambahDefect : Intent
        data class EditDefect(val data: MasterDefectDto) : Intent
        data class SimpanDefect(val data: MasterDefectDto) : Intent
        data class HapusDefect(val data: MasterDefectDto) : Intent

        data object TutupDialog : Intent
        data object ClearUserMessage : Intent
        data object Retry : Intent
    }

    /**
     * Side-effect satu kali, seperti snackbar, dialog, atau navigasi.
     */
    sealed interface Effect {
        data class TampilPesan(val pesan: String) : Effect
        data class TampilError(val judul: String, val pesan: String) : Effect
        data object DataTersimpan : Effect
        data object DataDihapus : Effect
    }

    /**
     * Dialog form aktif.
     */
    sealed interface DialogForm {
        data class FormPart(val data: MasterPartDto? = null) : DialogForm
        data class FormMaterial(val data: MasterMaterialDto? = null) : DialogForm
        data class FormDefect(val data: MasterDefectDto? = null) : DialogForm
    }
}
