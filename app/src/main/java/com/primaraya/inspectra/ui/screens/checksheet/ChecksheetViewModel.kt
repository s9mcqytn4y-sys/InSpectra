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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

data class ChecksheetUiState(
    val isLoading: Boolean = false,
    val daftarPart: List<RingkasanPartChecksheet> = emptyList(),
    val pesanValidasi: String? = null,
    val payloadJson: String? = null
)

sealed interface ChecksheetEvent {
    data class ShowToast(val message: String) : ChecksheetEvent
    object SubmitSuccess : ChecksheetEvent
}

class ChecksheetViewModel : ViewModel() {
    private val repository: ChecksheetRepository = SupabaseChecksheetRepository()

    private val jsonFormat = Json {
        prettyPrint = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val _uiState = MutableStateFlow(ChecksheetUiState())
    val uiState: StateFlow<ChecksheetUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChecksheetEvent>()
    val events: SharedFlow<ChecksheetEvent> = _events.asSharedFlow()

    fun muatChecksheet(tipeProses: TipeProses) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, pesanValidasi = null, payloadJson = null) }
            
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
            state.copy(daftarPart = newList, pesanValidasi = null, payloadJson = null)
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
            state.copy(daftarPart = newList, pesanValidasi = null, payloadJson = null)
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
            state.copy(daftarPart = newList, pesanValidasi = null, payloadJson = null)
        }
    }

    fun hapusPayload() {
        _uiState.update { it.copy(payloadJson = null, pesanValidasi = null) }
    }

    fun buatPayloadValidasi(tipeProses: TipeProses) {
        val aktif = _uiState.value.daftarPart.filter { it.jumlahDiperiksa > 0 || it.jumlahNg > 0 }

        if (aktif.isEmpty()) {
            _uiState.update { it.copy(pesanValidasi = "Isi minimal satu part sebelum membuat payload validasi.") }
            return
        }

        val tidakValid = aktif.filter { it.kuantitasTidakValid }
        if (tidakValid.isNotEmpty()) {
            _uiState.update { it.copy(pesanValidasi = "Jumlah NG melebihi jumlah diperiksa pada: ${tidakValid.joinToString(", ") { it.uniqNo }}.") }
            return
        }

        val totalDiperiksa = aktif.sumOf { it.jumlahDiperiksa }
        val totalNg = aktif.sumOf { it.jumlahNg }
        val totalOk = totalDiperiksa - totalNg
        val rasioGlobal = if (totalDiperiksa > 0) ((totalNg.toFloat() / totalDiperiksa.toFloat()) * 100f * 10f).roundToInt() / 10f else 0f

        val payload = PayloadChecksheet(
            tipeProses = tipeProses.name,
            dibuatPadaMillis = System.currentTimeMillis(),
            totalDiperiksa = totalDiperiksa,
            totalOk = totalOk,
            totalNg = totalNg,
            rasioNgGlobal = rasioGlobal,
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

        _uiState.update { it.copy(pesanValidasi = null, payloadJson = jsonFormat.encodeToString(payload)) }
    }

    fun submitKeSupabase() {
        val json = _uiState.value.payloadJson ?: return
        val payload = try {
            jsonFormat.decodeFromString<PayloadChecksheet>(json)
        } catch (e: Exception) {
            _uiState.update { it.copy(pesanValidasi = "Gagal parsing payload JSON") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, pesanValidasi = null) }
            val result = repository.submitChecksheet(payload)
            
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, payloadJson = null) }
                    _events.emit(ChecksheetEvent.SubmitSuccess)
                    _events.emit(ChecksheetEvent.ShowToast("Berhasil submit ke Supabase! (ID: ${result.data})"))
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, pesanValidasi = "${result.title}: ${result.message}") }
                }
                is NetworkResult.Loading -> { } // Diabaikan karena loading state diatur sebelum pemanggilan
            }
        }
    }
}
