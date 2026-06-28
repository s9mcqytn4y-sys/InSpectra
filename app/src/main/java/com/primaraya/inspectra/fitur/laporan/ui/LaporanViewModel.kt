package com.primaraya.inspectra.fitur.laporan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.primaraya.inspectra.fitur.laporan.data.SupabaseLaporanRepository
import com.primaraya.inspectra.fitur.laporan.domain.DetailLaporanDto
import com.primaraya.inspectra.fitur.laporan.domain.LaporanRepository
import com.primaraya.inspectra.fitur.laporan.domain.LaporanSubmitDto
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.primaraya.inspectra.core.common.CoroutineDispatchersProvider
import com.primaraya.inspectra.core.common.DefaultDispatchersProvider
import kotlinx.coroutines.flow.stateIn

class LaporanViewModel(
    application: Application,
    private val repository: LaporanRepository = SupabaseLaporanRepository(),
    private val dispatchers: CoroutineDispatchersProvider = DefaultDispatchersProvider
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(LaporanContract.State())
    val state: StateFlow<LaporanContract.State> = _state.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = LaporanContract.State()
    )
    private val _effect = MutableSharedFlow<LaporanContract.Effect>()
    val effect: SharedFlow<LaporanContract.Effect> = _effect.asSharedFlow()

    fun onIntent(intent: LaporanContract.Intent) {
        when (intent) {
            is LaporanContract.Intent.Muat -> muat(intent.tipeProses)
            is LaporanContract.Intent.UpdateTanggal -> _state.update { it.copy(tanggal = intent.tanggal) }
            is LaporanContract.Intent.UpdateMpDirect -> _state.update { it.copy(mpDirect = intent.value) }
            is LaporanContract.Intent.UpdateMpIndirect -> _state.update { it.copy(mpIndirect = intent.value) }
            is LaporanContract.Intent.UpdateJknHour -> _state.update { it.copy(jknHour = intent.value) }
            is LaporanContract.Intent.UpdateJknMenit -> _state.update { it.copy(jknMenit = intent.value) }
            is LaporanContract.Intent.UpdateOtProd -> _state.update { it.copy(otProd = intent.value) }
            is LaporanContract.Intent.UpdateOtNon -> _state.update { it.copy(otNon = intent.value) }
            is LaporanContract.Intent.UpdateBantuanKeluar -> _state.update { it.copy(bantuanKeluar = intent.value) }
            is LaporanContract.Intent.UpdateBantuanMasuk -> _state.update { it.copy(bantuanMasuk = intent.value) }
            
            is LaporanContract.Intent.UpdateDetailPlanning -> updateDetail(intent.index) { it.copy(planning = intent.value) }
            is LaporanContract.Intent.UpdateDetailActual -> updateDetail(intent.index) { it.copy(actual = intent.value) }
            is LaporanContract.Intent.UpdateDetailNg -> updateDetail(intent.index) { it.copy(ng = intent.value) }
            
            is LaporanContract.Intent.TogglePilihPart -> togglePilihPart(intent.idPart)
            LaporanContract.Intent.LanjutKeForm -> lanjutKeForm()
            LaporanContract.Intent.KembaliKePilihPart -> _state.update { it.copy(step = LaporanContract.Step.PILIH_PART) }
            
            LaporanContract.Intent.Submit -> submitLaporan()
            LaporanContract.Intent.DismissError -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun togglePilihPart(idPart: String) {
        _state.update { state ->
            val selected = state.selectedPartIds.toMutableSet()
            if (selected.contains(idPart)) {
                selected.remove(idPart)
            } else {
                selected.add(idPart)
            }
            state.copy(selectedPartIds = selected.toPersistentSet())
        }
    }

    private fun lanjutKeForm() {
        _state.update { state ->
            if (state.selectedPartIds.isEmpty()) {
                viewModelScope.launch { _effect.emit(LaporanContract.Effect.ShowSnackbar("Pilih minimal satu part")) }
                return@update state
            }
            
            // Generate details based on selected parts
            val newDetails = state.masterParts
                .filter { state.selectedPartIds.contains(it.uniqNo) }
                .map { part ->
                    // Preserve existing input if it exists
                    val existing = state.details.find { it.idPart == part.uniqNo }
                    existing ?: LaporanContract.DetailLaporanState(
                        idPart = part.uniqNo,
                        namaPart = part.namaPart
                    )
                }
                .toPersistentList()
                
            state.copy(step = LaporanContract.Step.ISI_FORM, details = newDetails)
        }
    }

    private fun updateDetail(index: Int, transform: (LaporanContract.DetailLaporanState) -> LaporanContract.DetailLaporanState) {
        _state.update { state ->
            val newDetails = state.details.set(index, transform(state.details[index]))
            state.copy(details = newDetails)
        }
    }

    private fun muat(tipeProses: String) {
        _state.update { it.copy(isLoading = true, tipeProses = tipeProses, tanggal = LocalDate.now().format(DateTimeFormatter.ISO_DATE)) }
        viewModelScope.launch {
            try {
                val parts = repository.getPartsForProcess(tipeProses)
                _state.update { it.copy(isLoading = false, masterParts = parts.toPersistentList()) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Terjadi kesalahan") }
                _effect.emit(LaporanContract.Effect.ShowSnackbar("Gagal memuat parts: ${e.message}"))
            }
        }
    }

    private fun submitLaporan() {
        val currentState = _state.value
        if (!currentState.isValid) {
            viewModelScope.launch {
                _effect.emit(LaporanContract.Effect.ShowSnackbar("Data belum lengkap atau tidak valid"))
            }
            return
        }

        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val details = currentState.details.map {
                    DetailLaporanDto(
                        idPart = it.idPart,
                        planning = it.planning.toIntOrNull() ?: 0,
                        actual = it.actual.toIntOrNull() ?: 0,
                        ng = it.ng.toIntOrNull() ?: 0
                    )
                }
                
                val dto = LaporanSubmitDto(
                    tanggal = currentState.tanggal,
                    tipeProses = currentState.tipeProses,
                    mpDirect = currentState.mpDirect.toIntOrNull() ?: 0,
                    mpIndirect = currentState.mpIndirect.toIntOrNull() ?: 0,
                    jknHour = currentState.jknHour.toIntOrNull() ?: 0,
                    jknMenit = currentState.jknMenit.toIntOrNull() ?: 0,
                    otProd = currentState.otProd.toDoubleOrNull() ?: 0.0,
                    otNon = currentState.otNon.toDoubleOrNull() ?: 0.0,
                    bantuanKeluar = currentState.bantuanKeluar.toIntOrNull() ?: 0,
                    bantuanMasuk = currentState.bantuanMasuk.toIntOrNull() ?: 0,
                    details = details
                )

                repository.submitLaporan(dto)
                _state.update { it.copy(isLoading = false) }
                _effect.emit(LaporanContract.Effect.ShowSnackbar("Laporan berhasil disimpan"))
                _effect.emit(LaporanContract.Effect.NavigateBack)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Gagal submit laporan") }
                _effect.emit(LaporanContract.Effect.ShowSnackbar("Gagal: ${e.message}"))
            }
        }
    }
}
