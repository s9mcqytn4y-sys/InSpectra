package com.primaraya.inspectra.fitur.attendance.ui

import androidx.compose.runtime.Immutable
import com.primaraya.inspectra.core.common.KeteranganHadir
import com.primaraya.inspectra.core.common.LemburNonMainJob
import com.primaraya.inspectra.core.common.LineProcess
import com.primaraya.inspectra.core.common.TipePekerja
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

object AttendanceContract {

    @Immutable
    data class State(
        val isLoading: Boolean = false,
        val tanggal: String = "",
        val selectedLine: LineProcess = LineProcess.PRESS_PRESS,
        val attendanceRows: ImmutableList<AttendanceRowState> = persistentListOf(),
        val errorMessage: String? = null,
        val successMessage: String? = null
    ) {
        // Real-time MP (Man Power) Calculation
        val totalMp: Int get() = attendanceRows.count { 
            it.keterangan == KeteranganHadir.HADIR || it.keterangan == KeteranganHadir.TELAT 
        }
    }

    @Immutable
    data class AttendanceRowState(
        val employeeId: String,
        val namaLengkap: String,
        val tipePekerja: TipePekerja,
        val noReg: String?,
        val keterangan: KeteranganHadir = KeteranganHadir.HADIR,
        val jamLembur: String = "0",
        val lemburNonMainJob: LemburNonMainJob = LemburNonMainJob.TIDAK_ADA
    )

    sealed interface Intent {
        data class Muat(val line: LineProcess, val tanggal: String) : Intent
        data class PilihLine(val line: LineProcess) : Intent
        data class UbahTanggal(val tanggal: String) : Intent
        
        data class UpdateKeterangan(val employeeId: String, val keterangan: KeteranganHadir) : Intent
        data class UpdateJamLembur(val employeeId: String, val value: String) : Intent
        data class UpdateLemburNonMainJob(val employeeId: String, val value: LemburNonMainJob) : Intent
        
        data object Simpan : Intent
        data object ClearMessage : Intent
    }
}
