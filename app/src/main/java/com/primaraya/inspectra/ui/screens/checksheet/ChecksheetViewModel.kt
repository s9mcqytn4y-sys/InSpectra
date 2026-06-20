package com.primaraya.inspectra.ui.screens.checksheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primaraya.inspectra.checksheet.data.ChecksheetRepository
import com.primaraya.inspectra.checksheet.data.SupabaseChecksheetRepository
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.domain.model.DataAcuanChecksheet
import com.primaraya.inspectra.domain.model.PayloadChecksheet
import com.primaraya.inspectra.domain.model.PayloadPartDiperiksa
import com.primaraya.inspectra.domain.model.RingkasanPartChecksheet
import com.primaraya.inspectra.domain.model.TipeProses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import com.primaraya.inspectra.core.ui.UserMessage
import com.primaraya.inspectra.core.ui.UserMessageMapper
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChecksheetUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val daftarPart: List<RingkasanPartChecksheet> = emptyList(),
    val previewPayload: PayloadChecksheet? = null,
    val userMessage: UserMessage? = null
) {
    val totalDiperiksa: Int get() = daftarPart.sumOf { it.jumlahDiperiksa }
    val totalNg: Int get() = daftarPart.sumOf { it.jumlahNg }
    val totalOk: Int get() = totalDiperiksa - totalNg

    val rasioNg: Float
        get() = if (totalDiperiksa > 0) (totalNg.toFloat() / totalDiperiksa) * 100f else 0f

    val adaInput: Boolean
        get() = daftarPart.any { it.jumlahDiperiksa > 0 || it.jumlahNg > 0 }

    val adaQtyTidakValid: Boolean
        get() = daftarPart.any { it.kuantitasTidakValid }
}

sealed interface ChecksheetEvent {
    data class ShowSuccess(val message: String) : ChecksheetEvent
    data class ShowError(val message: String) : ChecksheetEvent
    data class SubmitSuccess(val sesiId: String) : ChecksheetEvent
}

class ChecksheetViewModel : ViewModel() {
    private val repository: ChecksheetRepository = SupabaseChecksheetRepository()

    private val _uiState = MutableStateFlow(ChecksheetUiState())
    val uiState: StateFlow<ChecksheetUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChecksheetEvent>()
    val events: SharedFlow<ChecksheetEvent> = _events.asSharedFlow()

    fun muatChecksheet(tipeProses: TipeProses) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userMessage = null, previewPayload = null) }
            
            val daftar = withContext(Dispatchers.Default) {
                DataAcuanChecksheet.daftarPartChecksheet(tipeProses).map {
                    it.copy(
                        jumlahDiperiksa = 0,
                        terbuka = false,
                        daftarDefect = it.daftarDefect.map { defect -> defect.copy(jumlahNg = 0) }
                    )
                }
            }
            
            _uiState.update { it.copy(isLoading = false, daftarPart = daftar) }
        }
    }

    fun ubahBukaTutup(uniqNo: String) {
        _uiState.update { state ->
            val newList = state.daftarPart.map { part ->
                if (part.uniqNo == uniqNo) part.copy(terbuka = !part.terbuka) else part
            }
            state.copy(daftarPart = newList)
        }
    }

    fun ubahJumlahDiperiksa(uniqNo: String, jumlah: Int) {
        _uiState.update { state ->
            val newList = state.daftarPart.map { part ->
                if (part.uniqNo == uniqNo) part.copy(jumlahDiperiksa = jumlah.coerceAtLeast(0)) else part
            }
            state.copy(daftarPart = newList, userMessage = null, previewPayload = null)
        }
    }

    fun tambahKurangiDefect(uniqNo: String, idDefect: String, tambah: Boolean) {
        _uiState.update { state ->
            val newList = state.daftarPart.map { part ->
                if (part.uniqNo != uniqNo) return@map part
                part.copy(
                    daftarDefect = part.daftarDefect.map { defect ->
                        if (defect.idDefect == idDefect) {
                            val next = if (tambah) defect.jumlahNg + 1 else defect.jumlahNg - 1
                            defect.copy(jumlahNg = next.coerceAtLeast(0))
                        } else defect
                    }
                )
            }
            state.copy(daftarPart = newList, userMessage = null, previewPayload = null)
        }
    }

    fun isiManualDefect(uniqNo: String, idDefect: String, jumlah: Int) {
        _uiState.update { state ->
            val newList = state.daftarPart.map { part ->
                if (part.uniqNo != uniqNo) return@map part
                part.copy(
                    daftarDefect = part.daftarDefect.map { defect ->
                        if (defect.idDefect == idDefect) defect.copy(jumlahNg = jumlah.coerceAtLeast(0)) else defect
                    }
                )
            }
            state.copy(daftarPart = newList, userMessage = null, previewPayload = null)
        }
    }

    fun hapusPayload() {
        _uiState.update { it.copy(previewPayload = null, userMessage = null) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun buatPreviewPayload(tipeProses: TipeProses) {
        val aktif = _uiState.value.daftarPart.filter { it.jumlahDiperiksa > 0 || it.jumlahNg > 0 }

        if (aktif.isEmpty()) {
            _uiState.update { 
                it.copy(userMessage = UserMessage("Input Kosong", "Isi minimal satu part terlebih dahulu.")) 
            }
            return
        }

        val tidakValid = aktif.filter { it.kuantitasTidakValid }
        if (tidakValid.isNotEmpty()) {
            _uiState.update { 
                it.copy(userMessage = UserMessage("Input Tidak Valid", "Jumlah NG tidak boleh melebihi jumlah diperiksa.")) 
            }
            return
        }

        val state = _uiState.value
        val payload = PayloadChecksheet(
            tipeProses = tipeProses.name,
            dibuatPadaMillis = System.currentTimeMillis(),
            totalDiperiksa = state.totalDiperiksa,
            totalOk = state.totalOk,
            totalNg = state.totalNg,
            rasioNgGlobal = state.rasioNg,
            daftarPart = aktif.map { part ->
                PayloadPartDiperiksa(
                    uniqNo = part.uniqNo,
                    nomorPart = part.nomorPart,
                    namaPart = part.namaPart,
                    komoditas = part.komoditas.name,
                    jumlahDiperiksa = part.jumlahDiperiksa,
                    jumlahOk = part.jumlahOk,
                    jumlahNg = part.jumlahNg,
                    rasioNg = part.rasioNgSatuDesimal,
                    daftarMaterial = part.daftarMaterial,
                    daftarDefectNg = part.daftarDefect.filter { it.jumlahNg > 0 }
                )
            }
        )

        _uiState.update { it.copy(userMessage = null, previewPayload = payload) }
    }

    fun submitKeSupabase() {
        val payload = _uiState.value.previewPayload ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, userMessage = null) }
            val result = repository.submitChecksheet(payload)
            
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            previewPayload = null,
                            daftarPart = it.daftarPart.map { part ->
                                part.copy(
                                    jumlahDiperiksa = 0,
                                    terbuka = false,
                                    daftarDefect = part.daftarDefect.map { defect ->
                                        defect.copy(jumlahNg = 0)
                                    }
                                )
                            }
                        )
                    }
                    _events.emit(ChecksheetEvent.SubmitSuccess(result.data))
                    _events.emit(ChecksheetEvent.ShowSuccess("Checksheet berhasil dikirim."))
                }
                is NetworkResult.Error -> {
                    val friendly = UserMessageMapper.fromThrowableMessage(result.message)
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            userMessage = friendly
                        )
                    }
                    _events.emit(ChecksheetEvent.ShowError(friendly.body))
                }
                is NetworkResult.Loading -> { }
            }
        }
    }
}
