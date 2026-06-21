package com.primaraya.inspectra.fitur.cutting.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.data.PageRequest
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.ui.KonteksOperasi
import com.primaraya.inspectra.core.ui.UserMessageMapper
import com.primaraya.inspectra.fitur.checksheet.domain.SlotNg
import com.primaraya.inspectra.fitur.cutting.data.CuttingRepository
import com.primaraya.inspectra.fitur.cutting.data.SupabaseCuttingRepository
import com.primaraya.inspectra.fitur.cutting.domain.InputBatchCutting
import com.primaraya.inspectra.fitur.cutting.domain.InputDefectCutting
import com.primaraya.inspectra.fitur.cutting.domain.ValidatorBatchCutting
import com.primaraya.inspectra.fitur.masterdata.data.MasterDataRepository
import com.primaraya.inspectra.fitur.masterdata.data.SupabaseMasterDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class CuttingViewModel(
    application: Application,
    private val repository: CuttingRepository = SupabaseCuttingRepository(),
    private val masterRepository: MasterDataRepository = SupabaseMasterDataRepository()
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(CuttingContract.State(input = inputAwal()))
    val state: StateFlow<CuttingContract.State> = _state.asStateFlow()

    fun onIntent(intent: CuttingContract.Intent) {
        when (intent) {
            CuttingContract.Intent.Muat -> muat()
            is CuttingContract.Intent.UbahInput -> ubahInput(intent.input)
            is CuttingContract.Intent.PilihMaterial -> ubahInput(
                _state.value.input.copy(
                    materialId = intent.material.material_id,
                    namaMaterial = intent.material.nama_material,
                    spesifikasiMaterial = intent.material.spec_ringkas,
                    ukuranCuttingCm = intent.material.daftar_ukuran_cutting.firstOrNull()?.ukuran_cutting_cm?.toString().orEmpty()
                )
            )
            is CuttingContract.Intent.TambahDefect -> tambahDefect(intent)
            is CuttingContract.Intent.UbahJumlahDefect -> ubahDefect(intent.idDefect) {
                it.copy(jumlahLayerTerdampak = intent.jumlah.toIntOrNull() ?: 0)
            }
            is CuttingContract.Intent.UbahPanjangDefect -> ubahDefect(intent.idDefect) {
                it.copy(panjangDefectCm = intent.panjang.toDoubleOrNull())
            }
            is CuttingContract.Intent.UbahSlotDefect -> ubahDefect(intent.idDefect) {
                it.copy(idSlotWaktu = intent.idSlot)
            }
            is CuttingContract.Intent.HapusDefect -> ubahInput(
                _state.value.input.copy(daftarDefect = _state.value.input.daftarDefect.filterNot { it.idDefect == intent.idDefect })
            )
            CuttingContract.Intent.BukaPreview -> bukaPreview()
            CuttingContract.Intent.TutupPreview -> _state.update { it.copy(menampilkanPreview = false) }
            CuttingContract.Intent.Simpan -> simpan()
            CuttingContract.Intent.HapusPesan -> _state.update { it.copy(pesan = null) }
        }
    }

    private fun muat() {
        viewModelScope.launch {
            _state.update { it.copy(material = AsyncData.Loading, defect = AsyncData.Loading) }
            val material = repository.bacaOpsiMaterial()
            val defect = masterRepository.getDefectsPage(PageRequest(cursorColumn = "nama_defect", limit = 100))
            val slot = masterRepository.getSlotWaktu("CUTTING")

            _state.update {
                it.copy(
                    material = material.toAsyncData("Material Cutting belum tersedia."),
                    defect = defect.toAsyncData("Defect belum tersedia."),
                    slotWaktu = (slot as? NetworkResult.Success)?.data?.map { data ->
                        SlotNg(data.id, data.label_waktu)
                    }.orEmpty()
                )
            }
            bacaRingkasan()
        }
    }

    private fun bacaRingkasan() {
        viewModelScope.launch {
            when (val hasil = repository.bacaRingkasanHarian(_state.value.input.tanggalPemeriksaan)) {
                is NetworkResult.Success -> _state.update { it.copy(ringkasan = AsyncData.Success(hasil.data)) }
                is NetworkResult.Error -> _state.update { it.copy(ringkasan = AsyncData.Error("Ringkasan belum tersedia", hasil.message)) }
                else -> Unit
            }
        }
    }

    private fun tambahDefect(intent: CuttingContract.Intent.TambahDefect) {
        if (_state.value.input.daftarDefect.any { it.idDefect == intent.defect.id_defect }) return
        ubahInput(
            _state.value.input.copy(
                daftarDefect = _state.value.input.daftarDefect + InputDefectCutting(
                    idDefect = intent.defect.id_defect,
                    namaDefect = intent.defect.nama_defect,
                    jumlahLayerTerdampak = 1
                )
            )
        )
    }

    private fun ubahDefect(idDefect: String, transformasi: (InputDefectCutting) -> InputDefectCutting) {
        ubahInput(
            _state.value.input.copy(
                daftarDefect = _state.value.input.daftarDefect.map {
                    if (it.idDefect == idDefect) transformasi(it) else it
                }
            )
        )
    }

    private fun ubahInput(input: InputBatchCutting) {
        _state.update { it.copy(input = input, daftarPesanValidasi = emptyList()) }
    }

    private fun bukaPreview() {
        val input = _state.value.input
        val pesanValidasi = ValidatorBatchCutting.validasi(input)
        if (pesanValidasi.isNotEmpty()) {
            _state.update { it.copy(daftarPesanValidasi = pesanValidasi) }
            return
        }
        _state.update { it.copy(menampilkanPreview = true, daftarPesanValidasi = emptyList()) }
    }

    private fun simpan() {
        val input = _state.value.input

        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }
            when (val hasil = repository.simpanBatch(input)) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(
                            input = inputAwal(),
                            menampilkanPreview = false,
                            menyimpan = false,
                            daftarPesanValidasi = emptyList(),
                            pesan = "Batch Cutting berhasil disimpan."
                        )
                    }
                    bacaRingkasan()
                }
                is NetworkResult.Error -> {
                    val pesan = UserMessageMapper.fromThrowableMessage(hasil.message, KonteksOperasi.CHECKSHEET)
                    _state.update { it.copy(menyimpan = false, pesan = pesan.body) }
                }
                else -> Unit
            }
        }
    }

    private fun inputAwal(): InputBatchCutting = InputBatchCutting(tanggalPemeriksaan = LocalDate.now().toString())

    private fun <T> NetworkResult<List<T>>.toAsyncData(pesanKosong: String): AsyncData<List<T>> {
        return when (this) {
            is NetworkResult.Success -> if (data.isEmpty()) AsyncData.Empty("Belum ada data", pesanKosong) else AsyncData.Success(data)
            is NetworkResult.Error -> AsyncData.Error("Gagal memuat", message)
            else -> AsyncData.Loading
        }
    }
}
