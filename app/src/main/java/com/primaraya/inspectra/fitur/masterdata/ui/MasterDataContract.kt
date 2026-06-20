package com.primaraya.inspectra.fitur.masterdata.ui

import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.ui.UserMessage
import com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto
import com.primaraya.inspectra.fitur.masterdata.domain.MasterMaterialDto
import com.primaraya.inspectra.fitur.masterdata.domain.MasterPartDto

/**
 * Kontrak MVI untuk layar Master Data.
 */
object MasterDataContract {

    enum class TabMasterData {
        PART,
        MATERIAL,
        DEFECT
    }

    data class State(
        val tabAktif: TabMasterData = TabMasterData.PART,
        val parts: AsyncData<List<MasterPartDto>> = AsyncData.Idle,
        val materials: AsyncData<List<MasterMaterialDto>> = AsyncData.Idle,
        val defects: AsyncData<List<MasterDefectDto>> = AsyncData.Idle,
        val menyimpan: Boolean = false,
        val kataKunci: String = "",
        val dialogForm: DialogForm? = null,
        val userMessage: UserMessage? = null
    )

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

    sealed interface Effect {
        data class TampilPesan(val pesan: String) : Effect
        data class TampilError(val judul: String, val pesan: String) : Effect
        data object DataTersimpan : Effect
        data object DataDihapus : Effect
    }

    sealed interface DialogForm {
        data class FormPart(val data: MasterPartDto? = null) : DialogForm
        data class FormMaterial(val data: MasterMaterialDto? = null) : DialogForm
        data class FormDefect(val data: MasterDefectDto? = null) : DialogForm
    }
}
