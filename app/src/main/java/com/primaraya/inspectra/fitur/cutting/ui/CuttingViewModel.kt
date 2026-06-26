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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate

class CuttingViewModel(
    application: Application,
    private val repository: CuttingRepository = SupabaseCuttingRepository(),
    private val masterRepository: MasterDataRepository = SupabaseMasterDataRepository()
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(CuttingContract.State(input = inputAwal()))
    val state: StateFlow<CuttingContract.State> = _state.asStateFlow()

    companion object {
        private const val DEFAULT_NAMA_OPERATOR = "Agung"
    }

    fun onIntent(intent: CuttingContract.Intent) {
        when (intent) {
            CuttingContract.Intent.Muat -> muat()
            is CuttingContract.Intent.UbahInput -> ubahInput(intent.input)
            is CuttingContract.Intent.PilihMaterial -> {
                val ukuran = intent.material.daftarUkuranValid.firstOrNull { it.is_default }
                    ?: intent.material.daftarUkuranValid.firstOrNull()
                ubahInput(
                    _state.value.input.copy(
                        materialId = intent.material.material_id,
                        namaMaterial = intent.material.nama_material,
                        spesifikasiMaterial = intent.material.spec_ringkas,
                        idReferensiUkuranMaterial = ukuran?.id,
                        ukuranCuttingCm = ukuran?.ukuranEfektif?.toString().orEmpty()
                    )
                )
            }
            is CuttingContract.Intent.PilihPartAcuanUkuran -> {
                val ukuran = intent.part.daftarUkuranValid.firstOrNull()
                ubahInput(
                    _state.value.input.copy(
                        // ID referensi part bukan foreign key size_reference_id pada RPC.
                        // Nilai dipakai sebagai acuan manual sampai kontrak transaksi menyimpan referensi part tersendiri.
                        idReferensiUkuranMaterial = null,
                        ukuranCuttingCm = ukuran?.ukuranEfektif?.toString().orEmpty()
                    )
                )
            }
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
            _state.update { it.copy(material = AsyncData.Loading, partUkuran = AsyncData.Loading, defect = AsyncData.Loading) }
            
            when (val bootstrap = repository.bacaBootstrap()) {
                is NetworkResult.Success -> {
                    val data = bootstrap.data
                    _state.update {
                        it.copy(
                            material = if (data.material_option.isEmpty()) AsyncData.Empty("Belum ada data", "Material Cutting belum tersedia.") else AsyncData.Success(data.material_option.toImmutableList()),
                            partUkuran = if (data.part_size_option.isEmpty()) AsyncData.Empty("Belum ada data", "Referensi ukuran part belum tersedia.") else AsyncData.Success(data.part_size_option.toImmutableList()),
                            defect = if (data.defect_option.isEmpty()) AsyncData.Empty("Belum ada data", "Defect belum tersedia.") else AsyncData.Success(data.defect_option.toImmutableList()),
                            slotWaktu = data.slot_waktu.map { sw -> SlotNg(sw.id, sw.label_waktu) }.toImmutableList()
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _state.update { 
                        it.copy(
                            material = AsyncData.Error("Gagal memuat", bootstrap.message),
                            partUkuran = AsyncData.Error("Gagal memuat", bootstrap.message),
                            defect = AsyncData.Error("Gagal memuat", bootstrap.message)
                        ) 
                    }
                }
                else -> Unit
            }
            bacaRingkasan()
        }
    }

    private fun bacaRingkasan() {
        viewModelScope.launch {
            when (val hasil = repository.bacaRingkasanHarian(_state.value.input.tanggalPemeriksaan)) {
                is NetworkResult.Success -> {
                    val list = hasil.data.toImmutableList()
                    _state.update { it.copy(ringkasan = AsyncData.Success(list)) }
                }
                is NetworkResult.Error -> _state.update { it.copy(ringkasan = AsyncData.Error("Ringkasan belum tersedia", hasil.message)) }
                else -> Unit
            }
        }
    }

    private fun tambahDefect(intent: CuttingContract.Intent.TambahDefect) {
        if (_state.value.input.daftarDefect.any { it.idDefect == intent.defect.id_defect }) return
        val currentDefects = _state.value.input.daftarDefect
        val newList = currentDefects + InputDefectCutting(
            idDefect = intent.defect.id_defect,
            namaDefect = intent.defect.nama_defect,
            jumlahLayerTerdampak = 1
        )
        ubahInput(_state.value.input.copy(daftarDefect = newList))
    }

    private fun ubahDefect(idDefect: String, transformasi: (InputDefectCutting) -> InputDefectCutting) {
        val currentDefects = _state.value.input.daftarDefect
        val updatedList = currentDefects.map {
            if (it.idDefect == idDefect) transformasi(it) else it
        }
        ubahInput(_state.value.input.copy(daftarDefect = updatedList))
    }

    private fun ubahInput(input: InputBatchCutting) {
        _state.update { it.copy(input = input, daftarPesanValidasi = persistentListOf()) }
    }

    private fun bukaPreview() {
        val input = _state.value.input
        val pesanValidasi = ValidatorBatchCutting.validasi(input)
        if (pesanValidasi.isNotEmpty()) {
            _state.update { it.copy(daftarPesanValidasi = pesanValidasi) }
            return
        }
        _state.update { it.copy(menampilkanPreview = true, daftarPesanValidasi = persistentListOf()) }
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
                            daftarPesanValidasi = persistentListOf(),
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

    private fun inputAwal(): InputBatchCutting = InputBatchCutting(
        tanggalPemeriksaan = java.time.LocalDate.now().toString(),
        namaOperator = DEFAULT_NAMA_OPERATOR
    )

    private fun <T : Any> NetworkResult<List<T>>.toAsyncData(pesanKosong: String): AsyncData<ImmutableList<T>> {
        return when (this) {
            is NetworkResult.Success -> {
                val list = this.data.toImmutableList()
                if (list.isEmpty()) AsyncData.Empty("Belum ada data", pesanKosong) else AsyncData.Success(list)
            }
            is NetworkResult.Error -> AsyncData.Error("Gagal memuat", this.message)
            else -> AsyncData.Loading
        }
    }
}
