package com.primaraya.inspectra.fitur.masterdata.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.ui.UserMessageMapper
import com.primaraya.inspectra.core.ui.KonteksOperasi
import com.primaraya.inspectra.fitur.masterdata.data.MasterDataRepository
import com.primaraya.inspectra.fitur.masterdata.data.SupabaseMasterDataRepository
import com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto
import com.primaraya.inspectra.fitur.masterdata.domain.MasterMaterialDto
import com.primaraya.inspectra.fitur.masterdata.domain.MasterPartDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel MVI untuk Master Data.
 */
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

            is MasterDataContract.Intent.Cari -> {
                _state.update { it.copy(kataKunci = intent.keyword) }
            }

            MasterDataContract.Intent.TambahPart -> {
                _state.update {
                    it.copy(dialogForm = MasterDataContract.DialogForm.FormPart())
                }
            }

            is MasterDataContract.Intent.EditPart -> {
                _state.update {
                    it.copy(dialogForm = MasterDataContract.DialogForm.FormPart(intent.data))
                }
            }

            is MasterDataContract.Intent.SimpanPart -> simpanPart(intent.data)
            is MasterDataContract.Intent.HapusPart -> hapusPart(intent.data)

            MasterDataContract.Intent.TambahMaterial -> {
                _state.update {
                    it.copy(dialogForm = MasterDataContract.DialogForm.FormMaterial())
                }
            }

            is MasterDataContract.Intent.EditMaterial -> {
                _state.update {
                    it.copy(dialogForm = MasterDataContract.DialogForm.FormMaterial(intent.data))
                }
            }

            is MasterDataContract.Intent.SimpanMaterial -> simpanMaterial(intent.data)
            is MasterDataContract.Intent.HapusMaterial -> hapusMaterial(intent.data)

            MasterDataContract.Intent.TambahDefect -> {
                _state.update {
                    it.copy(dialogForm = MasterDataContract.DialogForm.FormDefect())
                }
            }

            is MasterDataContract.Intent.EditDefect -> {
                _state.update {
                    it.copy(dialogForm = MasterDataContract.DialogForm.FormDefect(intent.data))
                }
            }

            is MasterDataContract.Intent.SimpanDefect -> simpanDefect(intent.data)
            is MasterDataContract.Intent.HapusDefect -> hapusDefect(intent.data)

            MasterDataContract.Intent.TutupDialog -> {
                _state.update { it.copy(dialogForm = null) }
            }

            MasterDataContract.Intent.ClearUserMessage -> {
                _state.update { it.copy(userMessage = null) }
            }

            MasterDataContract.Intent.Retry -> muatDataTabAktif()
        }
    }

    private fun muatDataTabAktif() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, userMessage = null) }

            when (_state.value.tabAktif) {
                MasterDataContract.TabMasterData.PART -> {
                    when (val result = repository.getParts()) {
                        is NetworkResult.Success -> {
                            _state.update {
                                it.copy(loading = false, daftarPart = result.data)
                            }
                        }

                        is NetworkResult.Error -> tampilkanError(result.message)
                        NetworkResult.Loading -> Unit
                    }
                }

                MasterDataContract.TabMasterData.MATERIAL -> {
                    when (val result = repository.getMaterials()) {
                        is NetworkResult.Success -> {
                            _state.update {
                                it.copy(loading = false, daftarMaterial = result.data)
                            }
                        }

                        is NetworkResult.Error -> tampilkanError(result.message)
                        NetworkResult.Loading -> Unit
                    }
                }

                MasterDataContract.TabMasterData.DEFECT -> {
                    when (val result = repository.getDefects()) {
                        is NetworkResult.Success -> {
                            _state.update {
                                it.copy(loading = false, daftarDefect = result.data)
                            }
                        }

                        is NetworkResult.Error -> tampilkanError(result.message)
                        NetworkResult.Loading -> Unit
                    }
                }
            }
        }
    }

    private fun simpanPart(data: MasterPartDto) {
        val error = validasiPart(data)
        if (error != null) {
            kirimError("Data part belum lengkap", error)
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }

            when (val result = repository.upsertPart(data)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(menyimpan = false, dialogForm = null) }
                    _effect.emit(MasterDataContract.Effect.DataTersimpan)
                    muatDataTabAktif()
                }

                is NetworkResult.Error -> tampilkanError(result.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    private fun simpanMaterial(data: MasterMaterialDto) {
        if (data.nama_material.isBlank()) {
            kirimError("Data material belum lengkap", "Nama material wajib diisi.")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }

            when (val result = repository.upsertMaterial(data)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(menyimpan = false, dialogForm = null) }
                    _effect.emit(MasterDataContract.Effect.DataTersimpan)
                    muatDataTabAktif()
                }

                is NetworkResult.Error -> tampilkanError(result.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    private fun simpanDefect(data: MasterDefectDto) {
        if (data.id_defect.isBlank() || data.nama_defect.isBlank()) {
            kirimError("Data defect belum lengkap", "Kode dan nama defect wajib diisi.")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }

            when (val result = repository.upsertDefect(data)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(menyimpan = false, dialogForm = null) }
                    _effect.emit(MasterDataContract.Effect.DataTersimpan)
                    muatDataTabAktif()
                }

                is NetworkResult.Error -> tampilkanError(result.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    private fun hapusPart(data: MasterPartDto) {
        val id = data.id ?: return
        viewModelScope.launch {
            when (val result = repository.deletePartSoft(id)) {
                is NetworkResult.Success -> {
                    _effect.emit(MasterDataContract.Effect.DataDihapus)
                    muatDataTabAktif()
                }

                is NetworkResult.Error -> tampilkanError(result.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    private fun hapusMaterial(data: MasterMaterialDto) {
        val id = data.id ?: return
        viewModelScope.launch {
            when (val result = repository.deleteMaterialSoft(id)) {
                is NetworkResult.Success -> {
                    _effect.emit(MasterDataContract.Effect.DataDihapus)
                    muatDataTabAktif()
                }

                is NetworkResult.Error -> tampilkanError(result.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    private fun hapusDefect(data: MasterDefectDto) {
        viewModelScope.launch {
            when (val result = repository.deleteDefectSoft(data.id_defect)) {
                is NetworkResult.Success -> {
                    _effect.emit(MasterDataContract.Effect.DataDihapus)
                    muatDataTabAktif()
                }

                is NetworkResult.Error -> tampilkanError(result.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    private fun validasiPart(data: MasterPartDto): String? {
        return when {
            data.uniq_no.isBlank() -> "UNIQ wajib diisi."
            data.nama_part.isBlank() -> "Nama part wajib diisi."
            data.komoditas.isBlank() -> "Proses atau komoditas wajib dipilih."
            else -> null
        }
    }

    private fun tampilkanError(raw: String?) {
        val message = UserMessageMapper.fromThrowableMessage(raw, KonteksOperasi.MASTER_DATA)
        _state.update { it.copy(loading = false, menyimpan = false, userMessage = message) }
    }

    private fun kirimError(judul: String, pesan: String) {
        viewModelScope.launch {
            _effect.emit(MasterDataContract.Effect.TampilError(judul, pesan))
        }
    }
}
