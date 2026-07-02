package com.primaraya.inspectra.fitur.attendance.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.primaraya.inspectra.core.common.LineProcess
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.fitur.attendance.data.KehadiranRepository
import com.primaraya.inspectra.fitur.attendance.data.SupabaseKehadiranRepository
import com.primaraya.inspectra.fitur.attendance.domain.AttendanceRowDto
import com.primaraya.inspectra.fitur.attendance.domain.AttendanceSubmitDto
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class AttendanceViewModel(
    application: Application,
    private val repository: KehadiranRepository = SupabaseKehadiranRepository()
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AttendanceContract.State())
    val state: StateFlow<AttendanceContract.State> = _state.asStateFlow()

    init {
        val today = LocalDate.now().toString()
        onIntent(AttendanceContract.Intent.Muat(LineProcess.PRESS_PRESS, today))
    }

    fun onIntent(intent: AttendanceContract.Intent) {
        when (intent) {
            is AttendanceContract.Intent.Muat -> muat(intent.line, intent.tanggal)
            is AttendanceContract.Intent.PilihLine -> {
                _state.update { it.copy(selectedLine = intent.line) }
                muat(intent.line, _state.value.tanggal)
            }
            is AttendanceContract.Intent.UbahTanggal -> {
                _state.update { it.copy(tanggal = intent.tanggal) }
                muat(_state.value.selectedLine, intent.tanggal)
            }
            is AttendanceContract.Intent.UpdateKeterangan -> updateRow(intent.employeeId) { it.copy(keterangan = intent.keterangan) }
            is AttendanceContract.Intent.UpdateJamLembur -> updateRow(intent.employeeId) { it.copy(jamLembur = intent.value) }
            is AttendanceContract.Intent.UpdateLemburNonMainJob -> updateRow(intent.employeeId) { it.copy(lemburNonMainJob = intent.value) }
            AttendanceContract.Intent.Simpan -> simpan()
            AttendanceContract.Intent.ClearMessage -> _state.update { it.copy(errorMessage = null, successMessage = null) }
        }
    }

    private fun muat(line: LineProcess, tanggal: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, tanggal = tanggal) }
            when (val result = repository.getEmployeesByLine(line.name)) {
                is NetworkResult.Success -> {
                    val rows = result.data.map { emp ->
                        AttendanceContract.AttendanceRowState(
                            employeeId = emp.id ?: "",
                            namaLengkap = emp.namaLengkap,
                            tipePekerja = emp.tipePekerja,
                            noReg = emp.noReg
                        )
                    }.toImmutableList()
                    _state.update { it.copy(isLoading = false, attendanceRows = rows) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                else -> {}
            }
        }
    }

    private fun updateRow(id: String, transform: (AttendanceContract.AttendanceRowState) -> AttendanceContract.AttendanceRowState) {
        _state.update { s ->
            val updated = s.attendanceRows.map { if (it.employeeId == id) transform(it) else it }.toImmutableList()
            s.copy(attendanceRows = updated)
        }
    }

    private fun simpan() {
        val s = _state.value
        if (s.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val dto = AttendanceSubmitDto(
                tanggal = s.tanggal,
                lineProcess = s.selectedLine.name,
                attendanceList = s.attendanceRows.map { row ->
                    AttendanceRowDto(
                        karyawanId = row.employeeId,
                        keterangan = row.keterangan,
                        jamLemburAktual = row.jamLembur.toFloatOrNull() ?: 0f,
                        lemburNonMainJob = row.lemburNonMainJob
                    )
                }
            )

            when (val result = repository.submitAttendanceBatch(dto)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, successMessage = "Data kehadiran berhasil disimpan") }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                else -> {}
            }
        }
    }
}
