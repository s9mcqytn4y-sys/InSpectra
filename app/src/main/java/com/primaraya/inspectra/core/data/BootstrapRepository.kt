package com.primaraya.inspectra.core.data

import com.primaraya.inspectra.core.network.InspectraHttpClient
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.network.runNetworkCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Serializable
data class AppBootstrapDto(
    val app_code: String,
    val schema_revision: String,
    val server_time: String,
    val data_revision: JsonObject,
    val total_press_part: Int,
    val total_sewing_part: Int,
    val total_material: Int,
    val total_defect: Int,
    val total_part_belum_siap: Int
)

interface BootstrapRepository {
    suspend fun getBootstrapData(): NetworkResult<AppBootstrapDto>
}

class SupabaseBootstrapRepository(
    private val driver: DatabaseDriver = SupabasePgRestDriver()
) : BootstrapRepository {
    private val json = InspectraHttpClient.json

    override suspend fun getBootstrapData(): NetworkResult<AppBootstrapDto> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            val list = driver.getList(
                table = RemoteTable.ViewAppBootstrap,
                query = "limit=1",
                decode = { json.decodeFromString<List<AppBootstrapDto>>(it) }
            )
            list.first()
        }
    }
}
