package com.primaraya.inspectra.fitur.masterdata.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.ui.UserMessageMapper
import com.primaraya.inspectra.core.ui.KonteksOperasi
import com.primaraya.inspectra.fitur.masterdata.data.MasterDataRepository
import com.primaraya.inspectra.fitur.masterdata.data.SupabaseMasterDataRepository
import com.primaraya.inspectra.fitur.masterdata.domain.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MasterDataViewModel(
    private val repository: MasterDataRepository = SupabaseMasterDataRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(MasterDataContract.State())
    val state: StateFlow<MasterDataContract.State> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<MasterDataContract.Effect>()
    val effect: SharedFlow<MasterDataContract.Effect> = _effect.asSharedFlow()

    private var loadJob: Job? = null

    init {
        onIntent(MasterDataContract.Intent.MuatAwal)
    }

    fun onIntent(intent: MasterDataContract.Intent) {
        when (intent) {
            MasterDataContract.Intent.MuatAwal -> muatDataTabAktif()
            is MasterDataContract.Intent.PilihTab -> {
                _state.update { it.copy(tabAktif = intent.tab, kataKunci = "") }
                muatDataTabAktif()
            }
            is MasterDataContract.Intent.Cari -> _state.update { it.copy(kataKunci = intent.keyword) }

            // Part
            MasterDataContract.Intent.TambahPart -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormPart()) }
            is MasterDataContract.Intent.EditPart -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormPart(intent.data)) }
            is MasterDataContract.Intent.SimpanPart -> simpanPart(intent.data)
            is MasterDataContract.Intent.HapusPart -> confirmHapus("Hapus Part", "Hapus part ${intent.data.uniq_no}?") { hapusPart(intent.data) }
            is MasterDataContract.Intent.TogglePartDetail -> togglePartDetail(intent.uniqNo)

            // Material
            MasterDataContract.Intent.TambahMaterial -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormMaterial()) }
            is MasterDataContract.Intent.EditMaterial -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormMaterial(intent.data)) }
            is MasterDataContract.Intent.SimpanMaterial -> simpanMaterial(intent.data)
            is MasterDataContract.Intent.HapusMaterial -> confirmHapus("Hapus Material", "Hapus material ${intent.data.nama_material}?") { hapusMaterial(intent.data) }

            // Supplier
            MasterDataContract.Intent.TambahSupplier -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormSupplier()) }
            is MasterDataContract.Intent.EditSupplier -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormSupplier(intent.data)) }
            is MasterDataContract.Intent.SimpanSupplier -> simpanSupplier(intent.data)
            is MasterDataContract.Intent.HapusSupplier -> confirmHapus("Hapus Supplier", "Hapus supplier ${intent.data.nama_supplier}?") { hapusSupplier(intent.data) }

            // Defect
            MasterDataContract.Intent.TambahDefect -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormDefect()) }
            is MasterDataContract.Intent.EditDefect -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormDefect(intent.data)) }
            is MasterDataContract.Intent.SimpanDefect -> simpanDefect(intent.data)
            is MasterDataContract.Intent.HapusDefect -> confirmHapus("Hapus Defect", "Hapus defect ${intent.data.nama_defect}?") { hapusDefect(intent.data) }

            // Relations - Defect
            is MasterDataContract.Intent.BukaPilihDefect -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.PilihDefectUntukPart(intent.uniqNo)) }
            is MasterDataContract.Intent.TambahDefectKePart -> tambahDefectKePart(intent.uniqNo, intent.idDefect)
            is MasterDataContract.Intent.HapusDefectDariPart -> hapusDefectDariPart(intent.uniqNo, intent.relationId)

            // Relations - Material
            is MasterDataContract.Intent.BukaPilihMaterial -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.PilihMaterialUntukPart(intent.uniqNo)) }
            is MasterDataContract.Intent.TambahMaterialKePart -> tambahMaterialKePart(intent.uniqNo, intent.materialId, intent.label)
            is MasterDataContract.Intent.HapusMaterialDariPart -> hapusMaterialDariPart(intent.uniqNo, intent.relationId)

            MasterDataContract.Intent.TutupDialog -> _state.update { it.copy(dialogForm = null) }
            MasterDataContract.Intent.ClearUserMessage -> _state.update { it.copy(userMessage = null) }
            MasterDataContract.Intent.Retry -> muatDataTabAktif()
        }
    }

    private fun muatDataTabAktif() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val tab = _state.value.tabAktif
            updateTabState(tab, AsyncData.Loading)
            when (tab) {
                MasterDataContract.TabMasterData.PART -> handleLoadResult(repository.getParts()) { d -> _state.update { it.copy(parts = AsyncData.Success(d)) } }
                MasterDataContract.TabMasterData.MATERIAL -> handleLoadResult(repository.getMaterials()) { d -> _state.update { it.copy(materials = AsyncData.Success(d)) } }
                MasterDataContract.TabMasterData.SUPPLIER -> handleLoadResult(repository.getSuppliers()) { d -> _state.update { it.copy(suppliers = AsyncData.Success(d)) } }
                MasterDataContract.TabMasterData.DEFECT -> handleLoadResult(repository.getDefects()) { d -> _state.update { it.copy(defects = AsyncData.Success(d)) } }
            }
        }
    }

    private fun togglePartDetail(uniqNo: String) {
        val current = _state.value.partDetails[uniqNo] ?: MasterDataContract.PartRelationState()
        val next = !current.expanded
        _state.update { it.copy(partDetails = it.partDetails + (uniqNo to current.copy(expanded = next))) }
        if (next) muatDetailPart(uniqNo)
    }

    private fun muatDetailPart(uniqNo: String) {
        viewModelScope.launch {
            updatePartRelation(uniqNo) { it.copy(defects = AsyncData.Loading, materials = AsyncData.Loading) }
            
            val defectRes = repository.getPartDefects(uniqNo)
            val materialRes = repository.getPartMaterials(uniqNo)
            
            updatePartRelation(uniqNo) { s ->
                s.copy(
                    defects = when (defectRes) {
                        is NetworkResult.Success -> AsyncData.Success(defectRes.data)
                        is NetworkResult.Error -> AsyncData.Error("Error", defectRes.message)
                        else -> s.defects
                    },
                    materials = when (materialRes) {
                        is NetworkResult.Success -> AsyncData.Success(materialRes.data)
                        is NetworkResult.Error -> AsyncData.Error("Error", materialRes.message)
                        else -> s.materials
                    }
                )
            }
        }
    }

    private fun updatePartRelation(uniqNo: String, transform: (MasterDataContract.PartRelationState) -> MasterDataContract.PartRelationState) {
        _state.update { 
            val s = it.partDetails[uniqNo] ?: MasterDataContract.PartRelationState()
            it.copy(partDetails = it.partDetails + (uniqNo to transform(s))) 
        }
    }

    private fun tambahDefectKePart(uniqNo: String, idDefect: String) {
        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }
            val dto = MasterPartDefectDto(uniq_no = uniqNo, id_defect = idDefect)
            if (repository.upsertPartDefect(dto) is NetworkResult.Success) {
                _state.update { it.copy(menyimpan = false, dialogForm = null) }
                _effect.emit(MasterDataContract.Effect.DataTersimpan)
                muatDetailPart(uniqNo)
            } else tampilkanError("Gagal menautkan defect")
        }
    }

    private fun hapusDefectDariPart(uniqNo: String, relationId: String) {
        viewModelScope.launch {
            if (repository.deletePartDefect(relationId) is NetworkResult.Success) {
                _effect.emit(MasterDataContract.Effect.DataDihapus)
                muatDetailPart(uniqNo)
            } else tampilkanError("Gagal menghapus tautan")
        }
    }

    private fun tambahMaterialKePart(uniqNo: String, materialId: String, label: String) {
        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }
            val dto = MasterPartMaterialDto(uniq_no = uniqNo, material_id = materialId, label_material = label)
            if (repository.upsertPartMaterial(dto) is NetworkResult.Success) {
                _state.update { it.copy(menyimpan = false, dialogForm = null) }
                _effect.emit(MasterDataContract.Effect.DataTersimpan)
                muatDetailPart(uniqNo)
            } else tampilkanError("Gagal menautkan material")
        }
    }

    private fun hapusMaterialDariPart(uniqNo: String, relationId: String) {
        viewModelScope.launch {
            if (repository.deletePartMaterial(relationId) is NetworkResult.Success) {
                _effect.emit(MasterDataContract.Effect.DataDihapus)
                muatDetailPart(uniqNo)
            } else tampilkanError("Gagal menghapus tautan material")
        }
    }

    private fun confirmHapus(judul: String, pesan: String, action: () -> Unit) {
        _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.KonfirmasiHapus(judul, pesan, action)) }
    }

    private fun <T> handleLoadResult(result: NetworkResult<List<T>>, onSuccess: (List<T>) -> Unit) {
        when (result) {
            is NetworkResult.Success -> {
                if (result.data.isEmpty()) updateTabState(_state.value.tabAktif, AsyncData.Empty("Data Kosong", "Belum ada data."))
                else onSuccess(result.data)
            }
            is NetworkResult.Error -> {
                val msg = UserMessageMapper.fromThrowableMessage(result.message, KonteksOperasi.MASTER_DATA)
                updateTabState(_state.value.tabAktif, AsyncData.Error(msg.title, msg.body))
            }
            else -> Unit
        }
    }

    private fun updateTabState(tab: MasterDataContract.TabMasterData, async: AsyncData<*>) {
        _state.update {
            when (tab) {
                MasterDataContract.TabMasterData.PART -> it.copy(parts = async as AsyncData<List<MasterPartDto>>)
                MasterDataContract.TabMasterData.MATERIAL -> it.copy(materials = async as AsyncData<List<MasterMaterialDto>>)
                MasterDataContract.TabMasterData.SUPPLIER -> it.copy(suppliers = async as AsyncData<List<MasterSupplierDto>>)
                MasterDataContract.TabMasterData.DEFECT -> it.copy(defects = async as AsyncData<List<MasterDefectDto>>)
            }
        }
    }

    private fun simpanPart(data: MasterPartDto) {
        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }
            if (repository.upsertPart(data) is NetworkResult.Success) { _state.update { it.copy(menyimpan = false, dialogForm = null) }; _effect.emit(MasterDataContract.Effect.DataTersimpan); muatDataTabAktif() }
            else tampilkanError("Gagal menyimpan part")
        }
    }

    private fun simpanMaterial(data: MasterMaterialDto) {
        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }
            if (repository.upsertMaterial(data) is NetworkResult.Success) { _state.update { it.copy(menyimpan = false, dialogForm = null) }; _effect.emit(MasterDataContract.Effect.DataTersimpan); muatDataTabAktif() }
            else tampilkanError("Gagal menyimpan material")
        }
    }

    private fun simpanSupplier(data: MasterSupplierDto) {
        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }
            if (repository.upsertSupplier(data) is NetworkResult.Success) { _state.update { it.copy(menyimpan = false, dialogForm = null) }; _effect.emit(MasterDataContract.Effect.DataTersimpan); muatDataTabAktif() }
            else tampilkanError("Gagal menyimpan supplier")
        }
    }

    private fun simpanDefect(data: MasterDefectDto) {
        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }
            if (repository.upsertDefect(data) is NetworkResult.Success) { _state.update { it.copy(menyimpan = false, dialogForm = null) }; _effect.emit(MasterDataContract.Effect.DataTersimpan); muatDataTabAktif() }
            else tampilkanError("Gagal menyimpan defect")
        }
    }

    private fun hapusPart(data: MasterPartDto) {
        val id = data.id ?: return
        viewModelScope.launch {
            if (repository.deletePartSoft(id) is NetworkResult.Success) { _effect.emit(MasterDataContract.Effect.DataDihapus); _state.update { it.copy(dialogForm = null) }; muatDataTabAktif() }
        }
    }

    private fun hapusMaterial(data: MasterMaterialDto) {
        val id = data.id ?: return
        viewModelScope.launch {
            if (repository.deleteMaterialSoft(id) is NetworkResult.Success) { _effect.emit(MasterDataContract.Effect.DataDihapus); _state.update { it.copy(dialogForm = null) }; muatDataTabAktif() }
        }
    }

    private fun hapusSupplier(data: MasterSupplierDto) {
        val id = data.id ?: return
        viewModelScope.launch {
            if (repository.deleteSupplierSoft(id) is NetworkResult.Success) { _effect.emit(MasterDataContract.Effect.DataDihapus); _state.update { it.copy(dialogForm = null) }; muatDataTabAktif() }
        }
    }

    private fun hapusDefect(data: MasterDefectDto) {
        viewModelScope.launch {
            if (repository.deleteDefectSoft(data.id_defect) is NetworkResult.Success) { _effect.emit(MasterDataContract.Effect.DataDihapus); _state.update { it.copy(dialogForm = null) }; muatDataTabAktif() }
        }
    }

    private fun tampilkanError(raw: String?) {
        val message = UserMessageMapper.fromThrowableMessage(raw, KonteksOperasi.MASTER_DATA)
        _state.update { it.copy(menyimpan = false, userMessage = message) }
    }
}
