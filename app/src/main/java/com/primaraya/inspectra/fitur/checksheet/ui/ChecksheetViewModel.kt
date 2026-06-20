package com.primaraya.inspectra.fitur.checksheet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.fitur.checksheet.data.ChecksheetRepository
import com.primaraya.inspectra.fitur.checksheet.data.SupabaseChecksheetRepository
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.ui.UserMessageMapper
import com.primaraya.inspectra.core.ui.KonteksOperasi
import com.primaraya.inspectra.fitur.checksheet.domain.*
import com.primaraya.inspectra.fitur.masterdata.data.MasterDataRepository
import com.primaraya.inspectra.fitur.masterdata.data.SupabaseMasterDataRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel MVI untuk E-Checksheet.
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

    fun onIntent(intent: ChecksheetContract.Intent) {
        when (intent) {
            is ChecksheetContract.Intent.Muat -> muat(intent.tipeProses)
            is ChecksheetContract.Intent.TogglePart -> togglePart(intent.uniqNo)
            is ChecksheetContract.Intent.UbahJumlahDiperiksa -> ubahJumlahDiperiksa(intent.uniqNo, intent.jumlah)
            is ChecksheetContract.Intent.UbahJumlahDefect -> ubahJumlahDefect(intent.uniqNo, intent.idDefect, intent.jumlah)
            is ChecksheetContract.Intent.TambahDefect -> tambahKurangi(intent.uniqNo, intent.idDefect, 1)
            is ChecksheetContract.Intent.KurangiDefect -> tambahKurangi(intent.uniqNo, intent.idDefect, -1)
            is ChecksheetContract.Intent.UbahDetailCutting -> ubahDetailCutting(intent)
            ChecksheetContract.Intent.Tinjau -> buatPreview()
            ChecksheetContract.Intent.TutupPreview -> _state.update { it.copy(preview = null) }
            ChecksheetContract.Intent.Kirim -> kirim()
            ChecksheetContract.Intent.Retry -> muat(_state.value.tipeProses)
        }
    }

    private fun muat(tipeProses: TipeProses) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    tipeProses = tipeProses,
                    dataChecksheet = AsyncData.Loading,
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
                            detailCutting = if (tipeProses == TipeProses.CUTTING) DetailCutting() else null
                        )
                    }

                    _state.update {
                        it.copy(dataChecksheet = if (daftar.isEmpty()) AsyncData.Empty("Tidak ada data", "Silakan periksa Master Data.") else AsyncData.Success(daftar))
                    }
                }

                is NetworkResult.Error -> {
                    val msg = UserMessageMapper.fromThrowableMessage(result.message, KonteksOperasi.CHECKSHEET)
                    _state.update { it.copy(dataChecksheet = AsyncData.Error(msg.title, msg.body)) }
                }
                else -> Unit
            }
        }
    }

    private fun togglePart(uniqNo: String) {
        updateDaftarPart(uniqNo) { it.copy(terbuka = !it.terbuka) }
    }

    private fun ubahJumlahDiperiksa(uniqNo: String, jumlah: Int) {
        _state.update { it.copy(preview = null) }
        updateDaftarPart(uniqNo) { it.copy(jumlahDiperiksa = jumlah.coerceAtLeast(0)) }
    }

    private fun ubahJumlahDefect(uniqNo: String, idDefect: String, jumlah: Int) {
        _state.update { it.copy(preview = null) }
        updateDaftarPart(uniqNo) { part ->
            part.copy(
                daftarDefect = part.daftarDefect.map { defect ->
                    if (defect.idDefect == idDefect) defect.copy(jumlahNg = jumlah.coerceAtLeast(0)) else defect
                }
            )
        }
    }

    private fun tambahKurangi(uniqNo: String, idDefect: String, delta: Int) {
        val part = _state.value.daftarPart.firstOrNull { it.uniqNo == uniqNo } ?: return
        val defect = part.daftarDefect.firstOrNull { it.idDefect == idDefect } ?: return
        ubahJumlahDefect(uniqNo, idDefect, defect.jumlahNg + delta)
    }

    private fun ubahDetailCutting(intent: ChecksheetContract.Intent.UbahDetailCutting) {
        _state.update { it.copy(preview = null) }
        updateDaftarPart(intent.uniqNo) { part ->
            val current = part.detailCutting ?: DetailCutting()
            part.copy(
                detailCutting = current.copy(
                    noLot = intent.lot ?: current.noLot,
                    noRoll = intent.roll ?: current.noRoll,
                    sizeCuttingCm = intent.size ?: current.sizeCuttingCm,
                    waste = intent.waste ?: current.waste,
                    pic = intent.pic ?: current.pic
                )
            )
        }
    }

    private fun updateDaftarPart(targetUniqNo: String? = null, transform: (RingkasanPartChecksheet) -> RingkasanPartChecksheet) {
        val current = _state.value.dataChecksheet
        if (current is AsyncData.Success) {
            val updated = current.data.map { 
                if (targetUniqNo == null || it.uniqNo == targetUniqNo) transform(it) else it
            }
            _state.update { it.copy(dataChecksheet = AsyncData.Success(updated)) }
        }
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
                    daftarDefectNg = part.daftarDefect.filter { it.jumlahNg > 0 },
                    detailCutting = part.detailCutting
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
                            dataChecksheet = AsyncData.Success(it.daftarPart.map { part ->
                                part.copy(
                                    jumlahDiperiksa = 0,
                                    terbuka = false,
                                    daftarDefect = part.daftarDefect.map { defect ->
                                        defect.copy(jumlahNg = 0)
                                    },
                                    detailCutting = if (part.komoditas == TipeProses.CUTTING) DetailCutting() else null
                                )
                            })
                        )
                    }

                    _effect.emit(ChecksheetContract.Effect.KirimBerhasil(result.data))
                    _effect.emit(ChecksheetContract.Effect.PesanSukses("Checksheet berhasil dikirim."))
                }

                is NetworkResult.Error -> {
                    val message = UserMessageMapper.fromThrowableMessage(result.message, KonteksOperasi.CHECKSHEET)
                    _state.update { it.copy(mengirim = false) }
                    _effect.emit(ChecksheetContract.Effect.PesanError(message.title, message.body))
                }
                else -> Unit
            }
        }
    }

    private fun kirimError(judul: String, pesan: String) {
        viewModelScope.launch {
            _effect.emit(ChecksheetContract.Effect.PesanError(judul, pesan))
        }
    }
}
