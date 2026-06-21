package com.primaraya.inspectra.fitur.checksheet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.draft.DraftStore
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
import kotlinx.serialization.builtins.ListSerializer

/**
 * ViewModel MVI untuk E-Checksheet.
 */
class ChecksheetMviViewModel(
    application: Application,
    private val masterRepository: MasterDataRepository = SupabaseMasterDataRepository(),
    private val checksheetRepository: ChecksheetRepository = SupabaseChecksheetRepository(),
    private val draftStore: DraftStore = DraftStore(application)
) : AndroidViewModel(application) {

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
            is ChecksheetContract.Intent.UbahJumlahSlotDefect -> ubahJumlahSlotDefect(intent)
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

            // 1. Cek Draft Dulu
            val draftKey = "checksheet_draft_${tipeProses.name}"
            val draft = draftStore.readDraft(draftKey, ListSerializer(RingkasanPartChecksheet.serializer())).first()
            
            if (draft != null) {
                _state.update { it.copy(dataChecksheet = AsyncData.Success(draft)) }
                return@launch
            }

            // 2. Jika tidak ada draft, muat dari remote
            val slotsRes = masterRepository.getSlotWaktu(tipeProses.name)
            val slots = (slotsRes as? NetworkResult.Success)?.data ?: emptyList()

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
                                    jumlahNg = 0,
                                    detailSlot = slots.map { SlotNg(it.id, it.label_waktu) }
                                )
                            },
                            lokasiGambar = dto.lokasi_gambar,
                            detailCutting = if (tipeProses == TipeProses.CUTTING) DetailCutting() else null
                        )
                    }

                    _state.update {
                        it.copy(dataChecksheet = if (daftar.isEmpty()) AsyncData.Empty("Belum ada data", "Silakan periksa Data Induk.") else AsyncData.Success(daftar))
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

    private fun ubahJumlahSlotDefect(intent: ChecksheetContract.Intent.UbahJumlahSlotDefect) {
        _state.update { it.copy(preview = null) }
        updateDaftarPart(intent.uniqNo) { part ->
            part.copy(
                daftarDefect = part.daftarDefect.map { defect ->
                    if (defect.idDefect == intent.idDefect) {
                        defect.copy(
                            detailSlot = defect.detailSlot.map { slot ->
                                if (slot.slotId == intent.slotId) slot.copy(jumlah = intent.jumlah.coerceAtLeast(0)) else slot
                            }
                        )
                    } else defect
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
            
            // Simpan ke draft
            viewModelScope.launch {
                val draftKey = "checksheet_draft_${_state.value.tipeProses.name}"
                draftStore.saveDraft(draftKey, ListSerializer(RingkasanPartChecksheet.serializer()), updated)
            }
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

        if (state.adaSlotTidakMatch) {
            kirimError("Slot belum sesuai", "Total detail slot harus sama dengan jumlah NG.")
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
                    val clearedList = _state.value.daftarPart.map { part ->
                        part.copy(
                            jumlahDiperiksa = 0,
                            terbuka = false,
                            daftarDefect = part.daftarDefect.map { defect ->
                                defect.copy(jumlahNg = 0)
                            },
                            detailCutting = if (part.komoditas == TipeProses.CUTTING) DetailCutting() else null
                        )
                    }
                    
                    _state.update {
                        it.copy(
                            mengirim = false,
                            preview = null,
                            dataChecksheet = AsyncData.Success(clearedList)
                        )
                    }

                    // Hapus draft setelah berhasil kirim
                    val draftKey = "checksheet_draft_${_state.value.tipeProses.name}"
                    draftStore.clearDraft(draftKey)

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
