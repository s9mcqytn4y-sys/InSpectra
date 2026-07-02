package com.primaraya.inspectra.fitur.attendance.data

import com.primaraya.inspectra.core.common.CoroutineDispatchersProvider
import com.primaraya.inspectra.core.common.DefaultDispatchersProvider
import com.primaraya.inspectra.core.data.DatabaseDriver
import com.primaraya.inspectra.core.data.RemoteTable
import com.primaraya.inspectra.core.data.SupabasePgRestDriver
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.network.runNetworkCatching
import com.primaraya.inspectra.fitur.attendance.domain.AttendanceSubmitDto
import com.primaraya.inspectra.fitur.attendance.domain.EmployeeDto
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class SupabaseKehadiranRepository(
    private val driver: DatabaseDriver = SupabasePgRestDriver(),
    private val dispatchers: CoroutineDispatchersProvider = DefaultDispatchersProvider
) : KehadiranRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    override suspend fun getEmployeesByLine(line: String): NetworkResult<List<EmployeeDto>> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.Employee,
                query = "line_process=eq.$line&aktif=eq.true&order=nama_lengkap.asc",
                decode = { json.decodeFromString(ListSerializer(EmployeeDto.serializer()), it) }
            )
        }
    }

    override suspend fun submitAttendanceBatch(dto: AttendanceSubmitDto): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.rpc(
                functionName = "submit_attendance_batch",
                body = dto,
                encode = { json.encodeToString(AttendanceSubmitDto.serializer(), it) },
                decode = { }
            )
        }
    }

    override suspend fun upsertEmployee(employee: EmployeeDto): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.upsert(
                table = RemoteTable.Employee,
                body = employee,
                encode = { json.encodeToString(EmployeeDto.serializer(), it) },
                onConflict = "id"
            )
        }
    }

    override suspend fun deleteEmployee(id: String): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.softDelete(
                table = RemoteTable.Employee,
                idColumn = "id",
                id = id
            )
        }
    }
}
