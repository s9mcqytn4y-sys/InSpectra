package com.primaraya.inspectra.fitur.masterdata.ui

import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.ui.UserMessage
import com.primaraya.inspectra.fitur.masterdata.domain.*

/**
 * Kontrak MVI untuk layar Master Data.
 */
object MasterDataContract {

    enum class TabMasterData {
        PART,
        MATERIAL,
        SUPPLIER,
        DEFECT
    }

    enum class FilterMasterData {
        SEMUA,
        TANPA_MATERIAL,
        TANPA_DEFECT,
        NONAKTIF
    }

    data class State(
        val tabAktif: TabMasterData = TabMasterData.PART,
        val filterAktif: FilterMasterData = FilterMasterData.SEMUA,
        val parts: AsyncData<List<MasterPartDto>> = AsyncData.Idle,
        val materials: AsyncData<List<MasterMaterialDto>> = AsyncData.Idle,
        val suppliers: AsyncData<List<MasterSupplierDto>> = AsyncData.Idle,
        val defects: AsyncData<List<MasterDefectDto>> = AsyncData.Idle,
        val partDetails: Map<String, PartRelationState> = emptyMap(),
        val materialDetails: Map<String, MaterialRelationState> = emptyMap(),
        val menyimpan: Boolean = false,
        val kataKunci: String = "",
        val dialogForm: DialogForm? = null,
        val userMessage: UserMessage? = null,
        val partFormDraft: PartFormState = PartFormState(),
        val materialFormDraft: MaterialFormState = MaterialFormState(),
        val supplierFormDraft: SupplierFormState = SupplierFormState(),
        val defectFormDraft: DefectFormState = DefectFormState(),
        val canLoadMore: Boolean = true,
        val loadingMore: Boolean = false
    )

    data class PartRelationState(
        val defects: AsyncData<List<MasterPartDefectDto>> = AsyncData.Idle,
        val materials: AsyncData<List<MasterPartMaterialDto>> = AsyncData.Idle,
        val expanded: Boolean = false
    )

    data class MaterialRelationState(
        val defects: AsyncData<List<MasterMaterialDefectDto>> = AsyncData.Idle,
        val expanded: Boolean = false
    )

    sealed interface Intent {
        data object MuatAwal : Intent
        data class PilihTab(val tab: TabMasterData) : Intent
        data class PilihFilter(val filter: FilterMasterData) : Intent
        data class Cari(val keyword: String) : Intent

        data object TambahPart : Intent
        data class EditPart(val data: MasterPartDto) : Intent
        data class UbahFormPart(val data: PartFormState) : Intent
        data class SimpanPart(val data: PartFormState) : Intent
        data class HapusPart(val data: MasterPartDto) : Intent
        data class TogglePartDetail(val uniqNo: String) : Intent

        data object TambahMaterial : Intent
        data class EditMaterial(val data: MasterMaterialDto) : Intent
        data class UbahFormMaterial(val data: MaterialFormState) : Intent
        data class SimpanMaterial(val data: MaterialFormState) : Intent
        data class HapusMaterial(val data: MasterMaterialDto) : Intent
        data class ToggleMaterialDetail(val materialId: String) : Intent

        data object TambahSupplier : Intent
        data class EditSupplier(val data: MasterSupplierDto) : Intent
        data class UbahFormSupplier(val data: SupplierFormState) : Intent
        data class SimpanSupplier(val data: SupplierFormState) : Intent
        data class HapusSupplier(val data: MasterSupplierDto) : Intent

        data object TambahDefect : Intent
        data class EditDefect(val data: MasterDefectDto) : Intent
        data class UbahFormDefect(val data: DefectFormState) : Intent
        data class SimpanDefect(val data: DefectFormState) : Intent
        data class HapusDefect(val data: MasterDefectDto) : Intent

        // Relations - Defect
        data class BukaPilihDefect(val uniqNo: String) : Intent
        data class TambahDefectKePart(val uniqNo: String, val idDefect: String) : Intent
        data class HapusDefectDariPart(val uniqNo: String, val relationId: String) : Intent

        // Relations - Material
        data class BukaPilihMaterial(val uniqNo: String) : Intent
        data class TambahMaterialKePart(val uniqNo: String, val materialId: String, val label: String) : Intent
        data class HapusMaterialDariPart(val uniqNo: String, val relationId: String) : Intent

        data class BukaPilihDefectUntukMaterial(val materialId: String) : Intent
        data class TambahDefectKeMaterial(val materialId: String, val idDefect: String) : Intent
        data class HapusDefectDariMaterial(val materialId: String, val relationId: String) : Intent

        data object MuatLebihBanyak : Intent
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
        data class FormSupplier(val data: MasterSupplierDto? = null) : DialogForm
        data class FormDefect(val data: MasterDefectDto? = null) : DialogForm
        data class PilihDefectUntukPart(val uniqNo: String) : DialogForm
        data class PilihMaterialUntukPart(val uniqNo: String) : DialogForm
        data class PilihDefectUntukMaterial(val materialId: String) : DialogForm
        data class KonfirmasiHapus(val judul: String, val pesan: String, val onConfirm: () -> Unit) : DialogForm
    }
}
