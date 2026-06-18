package com.primaraya.inspectra.ui.screens.checksheet

import androidx.lifecycle.ViewModel
import com.primaraya.inspectra.domain.model.DataAcuanChecksheet
import com.primaraya.inspectra.domain.model.InputDefect
import com.primaraya.inspectra.domain.model.PayloadChecksheet
import com.primaraya.inspectra.domain.model.PayloadPartDiperiksa
import com.primaraya.inspectra.domain.model.RingkasanPartChecksheet
import com.primaraya.inspectra.domain.model.TipeProses
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

class ChecksheetViewModel : ViewModel() {

    private val jsonFormat = Json {
        prettyPrint = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val _uiState = MutableStateFlow<List<RingkasanPartChecksheet>>(emptyList())
    val uiState: StateFlow<List<RingkasanPartChecksheet>> = _uiState.asStateFlow()

    private val _pesanValidasi = MutableStateFlow<String?>(null)
    val pesanValidasi: StateFlow<String?> = _pesanValidasi.asStateFlow()

    private val _payloadJson = MutableStateFlow<String?>(null)
    val payloadJson: StateFlow<String?> = _payloadJson.asStateFlow()

    fun muatChecksheet(tipeProses: TipeProses) {
        _pesanValidasi.value = null
        _payloadJson.value = null
        _uiState.value = DataAcuanChecksheet
            .daftarPartChecksheet(tipeProses)
            .map {
                it.copy(
                    jumlahDiperiksa = 0,
                    terbuka = false,
                    daftarDefect = it.daftarDefect.map { defect -> defect.copy(jumlahNg = 0) }
                )
            }
    }

    fun ubahBukaTutup(uniqNo: String) {
        _uiState.update { list ->
            list.map { part ->
                if (part.uniqNo == uniqNo) {
                    part.copy(terbuka = !part.terbuka)
                } else {
                    part
                }
            }
        }
    }

    fun ubahJumlahDiperiksa(uniqNo: String, jumlah: Int) {
        _pesanValidasi.value = null
        _payloadJson.value = null

        _uiState.update { list ->
            list.map { part ->
                if (part.uniqNo == uniqNo) {
                    part.copy(jumlahDiperiksa = jumlah.coerceAtLeast(0))
                } else {
                    part
                }
            }
        }
    }

    fun tambahKurangiDefect(
        uniqNo: String,
        idDefect: String,
        tambah: Boolean
    ) {
        _pesanValidasi.value = null
        _payloadJson.value = null

        _uiState.update { list ->
            list.map { part ->
                if (part.uniqNo != uniqNo) return@map part

                part.copy(
                    daftarDefect = part.daftarDefect.map { defect ->
                        if (defect.idDefect == idDefect) {
                            val next = if (tambah) defect.jumlahNg + 1 else defect.jumlahNg - 1
                            defect.copy(jumlahNg = next.coerceAtLeast(0))
                        } else {
                            defect
                        }
                    }
                )
            }
        }
    }

    fun isiManualDefect(
        uniqNo: String,
        idDefect: String,
        jumlah: Int
    ) {
        _pesanValidasi.value = null
        _payloadJson.value = null

        _uiState.update { list ->
            list.map { part ->
                if (part.uniqNo != uniqNo) return@map part

                part.copy(
                    daftarDefect = part.daftarDefect.map { defect ->
                        if (defect.idDefect == idDefect) {
                            defect.copy(jumlahNg = jumlah.coerceAtLeast(0))
                        } else {
                            defect
                        }
                    }
                )
            }
        }
    }

    fun hapusPayload() {
        _payloadJson.value = null
        _pesanValidasi.value = null
    }

    fun buatPayloadValidasi(tipeProses: TipeProses) {
        val aktif = _uiState.value.filter { it.jumlahDiperiksa > 0 || it.jumlahNg > 0 }

        if (aktif.isEmpty()) {
            _payloadJson.value = null
            _pesanValidasi.value = "Isi minimal satu part sebelum membuat payload validasi."
            return
        }

        val tidakValid = aktif.filter { it.kuantitasTidakValid }
        if (tidakValid.isNotEmpty()) {
            _payloadJson.value = null
            _pesanValidasi.value = "Jumlah NG melebihi jumlah diperiksa pada: ${
                tidakValid.joinToString(", ") { it.uniqNo }
            }."
            return
        }

        val totalDiperiksa = aktif.sumOf { it.jumlahDiperiksa }
        val totalNg = aktif.sumOf { it.jumlahNg }
        val totalOk = totalDiperiksa - totalNg
        val rasioGlobal = if (totalDiperiksa > 0) {
            ((totalNg.toFloat() / totalDiperiksa.toFloat()) * 100f * 10f).roundToInt() / 10f
        } else {
            0f
        }

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

        _pesanValidasi.value = null
        _payloadJson.value = jsonFormat.encodeToString(payload)
    }
}
