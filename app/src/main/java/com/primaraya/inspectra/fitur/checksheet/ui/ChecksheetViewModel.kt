package com.primaraya.inspectra.fitur.checksheet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primaraya.inspectra.fitur.checksheet.data.ChecksheetRepository
import com.primaraya.inspectra.fitur.checksheet.data.SupabaseChecksheetRepository
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.ui.UserMessageMapper
import com.primaraya.inspectra.core.ui.KonteksOperasi
import com.primaraya.inspectra.fitur.checksheet.domain.InputDefect
import com.primaraya.inspectra.fitur.checksheet.domain.KategoriDefect
import com.primaraya.inspectra.fitur.checksheet.domain.PayloadChecksheet
import com.primaraya.inspectra.fitur.checksheet.domain.PayloadPartDiperiksa
import com.primaraya.inspectra.fitur.checksheet.domain.RingkasanPartChecksheet
import com.primaraya.inspectra.fitur.checksheet.domain.TipeProses
import com.primaraya.inspectra.fitur.masterdata.data.MasterDataRepository
import com.primaraya.inspectra.fitur.masterdata.data.SupabaseMasterDataRepository
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
 * ViewModel MVI untuk E-Checksheet.
 *
 * Data part dan defect selalu bersumber dari Master Data agar modul Master Data
 * dan E-Checksheet tetap sinkron.
 */
class ChecksheetMviViewModel(
    private val masterRepository: MasterDataRepository = SupabaseMasterDataRepository(),
    private val checksheetRepository: ChecksheetRepository = SupabaseChecksheetRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ChecksheetContract.State())
    val state: StateFlow<ChecksheetContract.State> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ChecksheetContract.Effect>()
    val effect: SharedFlow<ChecksheetContract.Effect> = _effect.asSharedFlow()

    private var loadJob: Job? = null

    /**
     * Entry point semua aksi dari UI.
     */
    fun onIntent(intent: ChecksheetContract.Intent) {
        when (intent) {
            is ChecksheetContract.Intent.Muat -> muat(intent.tipeProses)
            is ChecksheetContract.Intent.TogglePart -> togglePart(intent.uniqNo)
            is ChecksheetContract.Intent.UbahJumlahDiperiksa -> ubahJumlahDiperiksa(intent.uniqNo, intent.jumlah)
            is ChecksheetContract.Intent.UbahJumlahDefect -> ubahJumlahDefect(intent.uniqNo, intent.idDefect, intent.jumlah)
            is ChecksheetContract.Intent.TambahDefect -> tambahKurangi(intent.uniqNo, intent.idDefect, 1)
            is ChecksheetContract.Intent.KurangiDefect -> tambahKurangi(intent.uniqNo, intent.idDefect, -1)
            ChecksheetContract.Intent.Tinjau -> buatPreview()
            ChecksheetContract.Intent.TutupPreview -> _state.update { it.copy(preview = null) }
            ChecksheetContract.Intent.Kirim -> kirim()
            ChecksheetContract.Intent.Retry -> muat(_state.value.tipeProses)
        }
    }

    /**
     * Memuat data checksheet dari view master data.
     */
    private fun muat(tipeProses: TipeProses) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    tipeProses = tipeProses,
                    preview = null
                )
            }

            when (val result = masterRepository.getChecksheetData(tipeProses.name)) {
                is NetworkResult.Success -> {
                    val daftar = result.data.map { dto ->
                        RingkasanPartChecksheet(
                            uniqNo = dto.uniq_no,
                            nomorPart = dto.part_no,
                            namaPart = dto.nama_part,
                            komoditas = tipeProses,
                            daftarMaterial = emptyList(),
                            daftarDefect = dto.daftar_defect.map { defect ->
                                InputDefect(
                                    idDefect = defect.id_defect,
                                    namaDefect = defect.nama_defect,
                                    kategori = runCatching {
                                        KategoriDefect.valueOf(defect.kategori.uppercase())
                                    }.getOrDefault(KategoriDefect.PROSES),
                                    jumlahNg = 0
                                )
                            },
                            lokasiGambar = dto.lokasi_gambar,
                            jumlahDiperiksa = 0,
                            terbuka = false
                        )
                    }

                    _state.update {
                        it.copy(loading = false, daftarPart = daftar)
                    }
                }

                is NetworkResult.Error -> {
                    tampilkanError(result.message)
                }

                NetworkResult.Loading -> Unit
            }
        }
    }

    private fun togglePart(uniqNo: String) {
        _state.update { state ->
            state.copy(
                daftarPart = state.daftarPart.map {
                    if (it.uniqNo == uniqNo) it.copy(terbuka = !it.terbuka) else it
                }
            )
        }
    }

    private fun ubahJumlahDiperiksa(uniqNo: String, jumlah: Int) {
        _state.update { state ->
            state.copy(
                preview = null,
                daftarPart = state.daftarPart.map {
                    if (it.uniqNo == uniqNo) it.copy(jumlahDiperiksa = jumlah.coerceAtLeast(0)) else it
                }
            )
        }
    }

    private fun ubahJumlahDefect(uniqNo: String, idDefect: String, jumlah: Int) {
        _state.update { state ->
            state.copy(
                preview = null,
                daftarPart = state.daftarPart.map { part ->
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
            )
        }
    }

    private fun tambahKurangi(uniqNo: String, idDefect: String, delta: Int) {
        val part = _state.value.daftarPart.firstOrNull { it.uniqNo == uniqNo } ?: return
        val defect = part.daftarDefect.firstOrNull { it.idDefect == idDefect } ?: return
        ubahJumlahDefect(uniqNo, idDefect, defect.jumlahNg + delta)
    }

    private fun buatPreview() {
        val state = _state.value

        if (!state.adaInput) {
            kirimError("Input kosong", "Isi minimal satu part terlebih dahulu.")
            return
        }

        if (state.adaQtyTidakValid) {
            kirimError("Jumlah belum sesuai", "Jumlah NG tidak boleh melebihi jumlah diperiksa.")
            return
        }

        val aktif = state.daftarPart.filter { it.jumlahDiperiksa > 0 || it.jumlahNg > 0 }

        val payload = PayloadChecksheet(
            versiPayload = "fase-mvi-supabase",
            tipeProses = state.tipeProses.name,
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
                    daftarMaterial = emptyList(),
                    daftarDefectNg = part.daftarDefect.filter { it.jumlahNg > 0 }
                )
            }
        )

        _state.update { it.copy(preview = payload) }
    }

    private fun kirim() {
        val payload = _state.value.preview ?: return

        viewModelScope.launch {
            _state.update { it.copy(mengirim = true) }

            when (val result = checksheetRepository.submitChecksheet(payload)) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(
                            mengirim = false,
                            preview = null,
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

                    _effect.emit(ChecksheetContract.Effect.KirimBerhasil(result.data))
                    _effect.emit(ChecksheetContract.Effect.PesanSukses("Checksheet berhasil dikirim."))
                }

                is NetworkResult.Error -> tampilkanError(result.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    private fun tampilkanError(raw: String?) {
        val message = UserMessageMapper.fromThrowableMessage(raw, KonteksOperasi.CHECKSHEET)
        _state.update { it.copy(loading = false, mengirim = false) }

        viewModelScope.launch {
            _effect.emit(
                ChecksheetContract.Effect.PesanError(
                    judul = message.title,
                    pesan = message.body
                )
            )
        }
    }

    private fun kirimError(judul: String, pesan: String) {
        viewModelScope.launch {
            _effect.emit(ChecksheetContract.Effect.PesanError(judul, pesan))
        }
    }
}
