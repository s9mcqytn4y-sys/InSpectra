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
            is MasterDataContract.Intent.HapusPart -> hapusPart(intent.data)
            is MasterDataContract.Intent.TogglePartDetail -> togglePartDetail(intent.uniqNo)

            // Material
            MasterDataContract.Intent.TambahMaterial -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormMaterial()) }
            is MasterDataContract.Intent.EditMaterial -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormMaterial(intent.data)) }
            is MasterDataContract.Intent.SimpanMaterial -> simpanMaterial(intent.data)
            is MasterDataContract.Intent.HapusMaterial -> hapusMaterial(intent.data)

            // Supplier
            MasterDataContract.Intent.TambahSupplier -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormSupplier()) }
            is MasterDataContract.Intent.EditSupplier -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormSupplier(intent.data)) }
            is MasterDataContract.Intent.SimpanSupplier -> simpanSupplier(intent.data)
            is MasterDataContract.Intent.HapusSupplier -> hapusSupplier(intent.data)

            // Defect
            MasterDataContract.Intent.TambahDefect -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormDefect()) }
            is MasterDataContract.Intent.EditDefect -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormDefect(intent.data)) }
            is MasterDataContract.Intent.SimpanDefect -> simpanDefect(intent.data)
            is MasterDataContract.Intent.HapusDefect -> hapusDefect(intent.data)

            // Relations
            is MasterDataContract.Intent.TambahDefectKePart -> tambahDefectKePart(intent.uniqNo, intent.idDefect)
            is MasterDataContract.Intent.HapusDefectDariPart -> hapusDefectDariPart(intent.uniqNo, intent.relationId)

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
                MasterDataContract.TabMasterData.PART -> {
                    handleLoadResult(repository.getParts()) { d -> _state.update { it.copy(parts = AsyncData.Success(d)) } }
                }
                MasterDataContract.TabMasterData.MATERIAL -> {
                    handleLoadResult(repository.getMaterials()) { d -> _state.update { it.copy(materials = AsyncData.Success(d)) } }
                }
                MasterDataContract.TabMasterData.SUPPLIER -> {
                    handleLoadResult(repository.getSuppliers()) { d -> _state.update { it.copy(suppliers = AsyncData.Success(d)) } }
                }
                MasterDataContract.TabMasterData.DEFECT -> {
                    handleLoadResult(repository.getDefects()) { d -> _state.update { it.copy(defects = AsyncData.Success(d)) } }
                }
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
            _state.update { 
                val s = it.partDetails[uniqNo] ?: MasterDataContract.PartRelationState()
                it.copy(partDetails = it.partDetails + (uniqNo to s.copy(defects = AsyncData.Loading))) 
            }
            
            when (val result = repository.getPartDefects(uniqNo)) {
                is NetworkResult.Success -> {
                    _state.update { 
                        val s = it.partDetails[uniqNo] ?: MasterDataContract.PartRelationState()
                        it.copy(partDetails = it.partDetails + (uniqNo to s.copy(defects = AsyncData.Success(result.data))))
                    }
                }
                is NetworkResult.Error -> {
                    _state.update { 
                        val s = it.partDetails[uniqNo] ?: MasterDataContract.PartRelationState()
                        it.copy(partDetails = it.partDetails + (uniqNo to s.copy(defects = AsyncData.Error("Error", result.message))))
                    }
                }
                else -> Unit
            }
        }
    }

    private fun tambahDefectKePart(uniqNo: String, idDefect: String) {
        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }
            val dto = MasterPartDefectDto(uniq_no = uniqNo, id_defect = idDefect)
            when (repository.upsertPartDefect(dto)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(menyimpan = false, dialogForm = null) }
                    _effect.emit(MasterDataContract.Effect.DataTersimpan)
                    muatDetailPart(uniqNo)
                }
                is NetworkResult.Error -> tampilkanError("Gagal menautkan defect")
                else -> Unit
            }
        }
    }

    private fun hapusDefectDariPart(uniqNo: String, relationId: String) {
        viewModelScope.launch {
            when (repository.deletePartDefect(relationId)) {
                is NetworkResult.Success -> {
                    _effect.emit(MasterDataContract.Effect.DataDihapus)
                    muatDetailPart(uniqNo)
                }
                is NetworkResult.Error -> tampilkanError("Gagal menghapus tautan")
                else -> Unit
            }
        }
    }

    // Standard CRUD handlers (simpanPart, hapusPart, etc. from previous turn)
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
            when (val result = repository.upsertPart(data)) {
                is NetworkResult.Success -> { _state.update { it.copy(menyimpan = false, dialogForm = null) }; _effect.emit(MasterDataContract.Effect.DataTersimpan); muatDataTabAktif() }
                is NetworkResult.Error -> tampilkanError(result.message)
                else -> Unit
            }
        }
    }

    private fun simpanMaterial(data: MasterMaterialDto) {
        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }
            when (val result = repository.upsertMaterial(data)) {
                is NetworkResult.Success -> { _state.update { it.copy(menyimpan = false, dialogForm = null) }; _effect.emit(MasterDataContract.Effect.DataTersimpan); muatDataTabAktif() }
                is NetworkResult.Error -> tampilkanError(result.message)
                else -> Unit
            }
        }
    }

    private fun simpanSupplier(data: MasterSupplierDto) {
        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }
            when (val result = repository.upsertSupplier(data)) {
                is NetworkResult.Success -> { _state.update { it.copy(menyimpan = false, dialogForm = null) }; _effect.emit(MasterDataContract.Effect.DataTersimpan); muatDataTabAktif() }
                is NetworkResult.Error -> tampilkanError(result.message)
                else -> Unit
            }
        }
    }

    private fun simpanDefect(data: MasterDefectDto) {
        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }
            when (val result = repository.upsertDefect(data)) {
                is NetworkResult.Success -> { _state.update { it.copy(menyimpan = false, dialogForm = null) }; _effect.emit(MasterDataContract.Effect.DataTersimpan); muatDataTabAktif() }
                is NetworkResult.Error -> tampilkanError(result.message)
                else -> Unit
            }
        }
    }

    private fun hapusPart(data: MasterPartDto) {
        val id = data.id ?: return
        viewModelScope.launch {
            when (val result = repository.deletePartSoft(id)) {
                is NetworkResult.Success -> { _effect.emit(MasterDataContract.Effect.DataDihapus); muatDataTabAktif() }
                is NetworkResult.Error -> tampilkanError(result.message)
                else -> Unit
            }
        }
    }

    private fun hapusMaterial(data: MasterMaterialDto) {
        val id = data.id ?: return
        viewModelScope.launch {
            when (val result = repository.deleteMaterialSoft(id)) {
                is NetworkResult.Success -> { _effect.emit(MasterDataContract.Effect.DataDihapus); muatDataTabAktif() }
                is NetworkResult.Error -> tampilkanError(result.message)
                else -> Unit
            }
        }
    }

    private fun hapusSupplier(data: MasterSupplierDto) {
        val id = data.id ?: return
        viewModelScope.launch {
            when (val result = repository.deleteSupplierSoft(id)) {
                is NetworkResult.Success -> { _effect.emit(MasterDataContract.Effect.DataDihapus); muatDataTabAktif() }
                is NetworkResult.Error -> tampilkanError(result.message)
                else -> Unit
            }
        }
    }

    private fun hapusDefect(data: MasterDefectDto) {
        viewModelScope.launch {
            when (val result = repository.deleteDefectSoft(data.id_defect)) {
                is NetworkResult.Success -> { _effect.emit(MasterDataContract.Effect.DataDihapus); muatDataTabAktif() }
                is NetworkResult.Error -> tampilkanError(result.message)
                else -> Unit
            }
        }
    }

    private fun tampilkanError(raw: String?) {
        val message = UserMessageMapper.fromThrowableMessage(raw, KonteksOperasi.MASTER_DATA)
        _state.update { it.copy(menyimpan = false, userMessage = message) }
    }
}
