package com.primaraya.inspectra.fitur.checksheet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * ViewModel MVI untuk E-Checksheet.
 */
class ChecksheetMviViewModel(
    application: Application,
    private val masterRepository: MasterDataRepository = SupabaseMasterDataRepository(),
    private val checksheetRepository: ChecksheetRepository = SupabaseChecksheetRepository()
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ChecksheetContract.State())
    val state: StateFlow<ChecksheetContract.State> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ChecksheetContract.Effect>()
    val effect: SharedFlow<ChecksheetContract.Effect> = _effect.asSharedFlow()

    private var loadJob: Job? = null
    
    init {
        // Search Debouncing: Hanya filter picker saat pencarian stabil (300ms)
        _state.map { it.pencarian }
            .distinctUntilChanged()
            .debounce(300)
            .onEach { query ->
                // Jika butuh trigger reload server bisa di sini, 
                // tapi saat ini filter dilakukan di UI State (pickerFiltered).
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: ChecksheetContract.Intent) {
        when (intent) {
            is ChecksheetContract.Intent.Muat -> muat(intent.tipeProses)
            is ChecksheetContract.Intent.CariPart -> _state.update { it.copy(pencarian = intent.query) }
            is ChecksheetContract.Intent.PilihPart -> pilihPart(intent.uniqNo, intent.pilih)
            ChecksheetContract.Intent.LanjutKeForm -> muatForm()
            ChecksheetContract.Intent.KembaliKePicker -> _state.update { it.copy(step = ChecksheetContract.State.Step.PILIH_PART) }
            
            is ChecksheetContract.Intent.TogglePart -> togglePart(intent.uniqNo)
            is ChecksheetContract.Intent.UbahJumlahDiperiksa -> ubahJumlahDiperiksa(intent.uniqNo, intent.jumlah)
            is ChecksheetContract.Intent.UbahJumlahDefect -> ubahJumlahDefect(intent.uniqNo, intent.idDefect, intent.jumlah)
            is ChecksheetContract.Intent.UbahJumlahSlotDefect -> ubahJumlahSlotDefect(intent)
            is ChecksheetContract.Intent.TambahDefect -> tambahKurangi(intent.uniqNo, intent.idDefect, 1)
            is ChecksheetContract.Intent.KurangiDefect -> tambahKurangi(intent.uniqNo, intent.idDefect, -1)
            
            is ChecksheetContract.Intent.SembunyikanDefect -> sembunyikanDefect(intent.uniqNo, intent.idDefect)
            is ChecksheetContract.Intent.TampilkanDefect -> tampilkanDefect(intent.uniqNo, intent.idDefect)
            is ChecksheetContract.Intent.BukaTambahDefectLain -> bukaTambahDefectLain(intent.uniqNo)
            is ChecksheetContract.Intent.TambahDefectLain -> tambahDefectLain(intent.uniqNo, intent.defect)
            ChecksheetContract.Intent.TutupDialogTambahDefect -> _state.update { it.copy(dialogTambahDefect = null) }

            ChecksheetContract.Intent.Tinjau -> buatPreview()
            ChecksheetContract.Intent.TutupPreview -> _state.update { it.copy(preview = null) }
            ChecksheetContract.Intent.Kirim -> kirim()
            ChecksheetContract.Intent.TutupBerhasil -> _state.update { it.copy(berhasil = false, step = ChecksheetContract.State.Step.PILIH_PART) }
            ChecksheetContract.Intent.Retry -> muat(_state.value.tipeProses)
        }
    }

    private fun muat(tipeProses: TipeProses) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    tipeProses = tipeProses,
                    dataPicker = AsyncData.Loading,
                    dataChecksheet = AsyncData.Idle,
                    partTerpilih = emptySet(),
                    step = ChecksheetContract.State.Step.PILIH_PART,
                    preview = null
                )
            }

            when (val result = masterRepository.getPartPickerItems(tipeProses.name)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(dataPicker = AsyncData.Success(result.data.toImmutableList())) }
                }
                is NetworkResult.Error -> {
                    val msg = UserMessageMapper.fromThrowableMessage(result.message, KonteksOperasi.CHECKSHEET)
                    _state.update { it.copy(dataPicker = AsyncData.Error(msg.title, msg.body)) }
                }
                else -> Unit
            }
        }
    }

    private fun pilihPart(uniqNo: String, pilih: Boolean) {
        _state.update {
            val set = it.partTerpilih.toMutableSet()
            if (pilih) set.add(uniqNo) else set.remove(uniqNo)
            it.copy(partTerpilih = set)
        }
    }

    private fun muatForm() {
        val tipeProses = _state.value.tipeProses
        val terpilih = _state.value.partTerpilih
        if (terpilih.isEmpty()) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(dataChecksheet = AsyncData.Loading) }

            val slotsRes = masterRepository.getSlotWaktu(tipeProses.name)
            val slots = (slotsRes as? NetworkResult.Success)?.data ?: emptyList()

            when (val result = masterRepository.getChecksheetData(tipeProses.name)) {
                is NetworkResult.Success -> {
                    val daftar = result.data
                        .filter { dto -> terpilih.contains(dto.uniq_no) }
                        .map { dto ->
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
                                        detailSlot = slots.map { SlotNg(it.id, it.label_waktu) }.toImmutableList()
                                    )
                                }.toImmutableList(),
                                lokasiGambar = dto.lokasi_gambar,
                                terbuka = terpilih.size == 1 // Auto-open if only one part selected
                            )
                        }.toImmutableList()

                    _state.update {
                        it.copy(
                            dataChecksheet = AsyncData.Success(daftar),
                            step = ChecksheetContract.State.Step.ISI_FORM
                        )
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
        val jamSekarang = LocalTime.now()
        
        updateDaftarPart(uniqNo) { part ->
            part.copy(
                daftarDefect = part.daftarDefect.map { defect ->
                    if (defect.idDefect == idDefect) {
                        val inputBaru = defect.copy(jumlahNg = jumlah.coerceAtLeast(0))
                        
                        // Smart Slot Selection: Jika NG > 0 dan detail slot masih kosong (semua 0), 
                        // otomatis isi slot yang sesuai dengan jam sekarang.
                        if (jumlah > 0 && inputBaru.detailSlot.all { it.jumlah == 0 }) {
                            val slotIdTarget = cariSlotIdSesuaiJam(jamSekarang, inputBaru.detailSlot)
                            if (slotIdTarget != null) {
                                inputBaru.copy(
                                    detailSlot = inputBaru.detailSlot.map { slot ->
                                        if (slot.slotId == slotIdTarget) slot.copy(jumlah = jumlah) else slot
                                    }.toImmutableList()
                                )
                            } else inputBaru
                        } else {
                            inputBaru
                        }
                    } else defect
                }.toImmutableList()
            )
        }
    }

    private fun cariSlotIdSesuaiJam(sekarang: LocalTime, slots: List<SlotNg>): String? {
        val jamStr = sekarang.format(DateTimeFormatter.ofPattern("HH"))
        // Cari slot yang labelnya mengandung jam sekarang, misal "08:00 - 09:00"
        return slots.find { it.labelWaktu.startsWith(jamStr) }?.slotId ?: slots.firstOrNull()?.slotId
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
                            }.toImmutableList()
                        )
                    } else defect
                }.toImmutableList()
            )
        }
    }

    private fun tambahKurangi(uniqNo: String, idDefect: String, delta: Int) {
        val part = _state.value.daftarPart.firstOrNull { it.uniqNo == uniqNo } ?: return
        val defect = part.daftarDefect.firstOrNull { it.idDefect == idDefect } ?: return
        ubahJumlahDefect(uniqNo, idDefect, defect.jumlahNg + delta)
    }

    private fun sembunyikanDefect(uniqNo: String, idDefect: String) {
        updateDaftarPart(uniqNo) { part ->
            part.copy(defectTersembunyi = part.defectTersembunyi + idDefect)
        }
    }

    private fun tampilkanDefect(uniqNo: String, idDefect: String) {
        updateDaftarPart(uniqNo) { part ->
            part.copy(defectTersembunyi = part.defectTersembunyi - idDefect)
        }
    }

    private fun bukaTambahDefectLain(uniqNo: String) {
        viewModelScope.launch {
            _state.update { it.copy(dialogTambahDefect = uniqNo) }
            val current = _state.value.daftarDefectMaster
            if (current !is AsyncData.Success) {
                _state.update { it.copy(daftarDefectMaster = AsyncData.Loading) }
                when (val result = masterRepository.getDefectsPage(com.primaraya.inspectra.core.data.PageRequest(limit = 100, cursorColumn = "id_defect"))) {
                    is NetworkResult.Success -> _state.update { it.copy(daftarDefectMaster = AsyncData.Success(result.data.toImmutableList())) }
                    is NetworkResult.Error -> _effect.emit(ChecksheetContract.Effect.PesanError("Gagal muat master defect", result.message))
                    else -> Unit
                }
            }
        }
    }

    private fun tambahDefectLain(uniqNo: String, defect: com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto) {
        viewModelScope.launch {
            val slotsRes = masterRepository.getSlotWaktu(_state.value.tipeProses.name)
            val slots = (slotsRes as? NetworkResult.Success)?.data ?: emptyList()

            updateDaftarPart(uniqNo) { part ->
                if (part.daftarDefect.any { it.idDefect == defect.id_defect }) {
                    part.copy(defectTersembunyi = part.defectTersembunyi - defect.id_defect)
                } else {
                    val input = InputDefect(
                        idDefect = defect.id_defect,
                        namaDefect = defect.nama_defect,
                        kategori = runCatching { KategoriDefect.valueOf(defect.kategori.uppercase()) }.getOrDefault(KategoriDefect.PROSES),
                        jumlahNg = 0,
                        detailSlot = slots.map { SlotNg(it.id, it.label_waktu) }.toImmutableList()
                    )
                    part.copy(daftarDefect = (part.daftarDefect + input).toImmutableList())
                }
            }
            _state.update { it.copy(dialogTambahDefect = null) }
        }
    }

    private fun updateDaftarPart(targetUniqNo: String? = null, transform: (RingkasanPartChecksheet) -> RingkasanPartChecksheet) {
        val current = _state.value.dataChecksheet
        if (current is AsyncData.Success) {
            val updated = current.data.map { 
                if (targetUniqNo == null || it.uniqNo == targetUniqNo) transform(it) else it
            }.toImmutableList()
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
                    daftarDefectNg = part.daftarDefect.filter { it.jumlahNg > 0 }.toImmutableList()
                )
            }.toImmutableList()
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
                            }.toImmutableList()
                        )
                    }.toImmutableList()
                    
                    _state.update {
                        it.copy(
                            mengirim = false,
                            berhasil = true, // Show success screen
                            preview = null,
                            dataChecksheet = AsyncData.Success(clearedList)
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
