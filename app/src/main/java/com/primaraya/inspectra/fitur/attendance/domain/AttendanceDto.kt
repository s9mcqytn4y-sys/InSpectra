package com.primaraya.inspectra.fitur.attendance.domain

import com.primaraya.inspectra.core.common.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmployeeDto(
    @SerialName("id") val id: String? = null,
    @SerialName("nama_lengkap") val namaLengkap: String,
    @SerialName("tipe_pekerja") val tipePekerja: TipePekerja,
    @SerialName("no_reg") val noReg: String? = null,
    @SerialName("line_process") val lineProcess: String, // String representation of LineProcess or TipeProses
    @SerialName("aktif") val aktif: Boolean = true
)

@Serializable
data class AttendanceSubmitDto(
    @SerialName("tanggal") val tanggal: String,
    @SerialName("line_process") val lineProcess: String,
    @SerialName("attendance_list") val attendanceList: List<AttendanceRowDto>
)

@Serializable
data class AttendanceRowDto(
    @SerialName("karyawan_id") val karyawanId: String,
    @SerialName("keterangan") val keterangan: KeteranganHadir,
    @SerialName("jam_lembur_aktual") val jamLemburAktual: Float,
    @SerialName("lembur_non_main_job") val lemburNonMainJob: LemburNonMainJob
)
