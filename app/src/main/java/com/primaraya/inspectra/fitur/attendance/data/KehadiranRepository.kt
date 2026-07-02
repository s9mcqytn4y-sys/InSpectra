package com.primaraya.inspectra.fitur.attendance.data

import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.fitur.attendance.domain.AttendanceSubmitDto
import com.primaraya.inspectra.fitur.attendance.domain.EmployeeDto

interface KehadiranRepository {
    suspend fun getEmployeesByLine(line: String): NetworkResult<List<EmployeeDto>>
    suspend fun submitAttendanceBatch(dto: AttendanceSubmitDto): NetworkResult<Unit>
    suspend fun upsertEmployee(employee: EmployeeDto): NetworkResult<Unit>
    suspend fun deleteEmployee(id: String): NetworkResult<Unit>
}
