package com.primaraya.inspectra.fitur.laporan.data

import com.primaraya.inspectra.core.common.CoroutineDispatchersProvider
import com.primaraya.inspectra.core.common.DefaultDispatchersProvider
import com.primaraya.inspectra.core.data.DatabaseDriver
import com.primaraya.inspectra.core.data.SupabasePgRestDriver
import com.primaraya.inspectra.core.data.RemoteTable
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.network.runNetworkCatching
import com.primaraya.inspectra.fitur.checksheet.domain.PartAcuan
import com.primaraya.inspectra.fitur.laporan.domain.LaporanRepository
import com.primaraya.inspectra.fitur.laporan.domain.LaporanSubmitDto
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class SupabaseLaporanRepository(
    private val driver: DatabaseDriver = SupabasePgRestDriver(),
    private val dispatchers: CoroutineDispatchersProvider = DefaultDispatchersProvider
) : LaporanRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    override suspend fun getPartsForProcess(tipeProses: String): List<PartAcuan> = withContext(dispatchers.io) {
        val result = runNetworkCatching {
            driver.getList(
                table = RemoteTable.ViewChecksheetPartPicker,
                query = "komoditas=eq.$tipeProses",
                decode = { json.decodeFromString(ListSerializer(PartAcuan.serializer()), it) }
            )
        }
        
        when (result) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error -> throw Exception(result.message)
            is NetworkResult.Loading -> emptyList()
        }
    }

    override suspend fun submitLaporan(dto: LaporanSubmitDto): Unit = withContext(dispatchers.io) {
        val result = runNetworkCatching {
            driver.rpc(
                functionName = "submit_laporan_harian",
                body = dto,
                encode = { json.encodeToString(LaporanSubmitDto.serializer(), it) },
                decode = { it }
            )
        }
        
        when (result) {
            is NetworkResult.Success -> Unit
            is NetworkResult.Error -> throw Exception(result.message)
            is NetworkResult.Loading -> Unit
        }
    }
}
