package com.primaraya.inspectra.fitur.checksheet.data

import com.primaraya.inspectra.core.data.DatabaseDriver
import com.primaraya.inspectra.core.data.RemoteTable
import com.primaraya.inspectra.core.data.SupabasePgRestDriver
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.network.runNetworkCatching
import com.primaraya.inspectra.core.common.CoroutineDispatchersProvider
import com.primaraya.inspectra.core.common.DefaultDispatchersProvider
import com.primaraya.inspectra.fitur.checksheet.domain.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class SupabaseChecksheetRepository(
    private val driver: DatabaseDriver = SupabasePgRestDriver(),
    private val storageDriver: com.primaraya.inspectra.core.data.SupabaseStorageDriver = com.primaraya.inspectra.core.data.SupabaseStorageDriver(),
    private val dispatchers: CoroutineDispatchersProvider = DefaultDispatchersProvider
) : ChecksheetRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    override suspend fun submitChecksheet(
        payload: PayloadChecksheet
    ): NetworkResult<String> = withContext(dispatchers.io) {
        runNetworkCatching {
            require(payload.tipeProses != "CUTTING") {
                "Cutting harus dikirim melalui form Cutting."
            }

            // Convert UI list back to standard list for serialization fix
            val serializablePayload = payload.copy(
                daftarPart = payload.daftarPart.map { p ->
                    p.copy(
                        daftarDefectNg = p.daftarDefectNg.map { d ->
                            d.copy(detailSlot = d.detailSlot.toList())
                        }
                    )
                }
            )

            val resultId = driver.rpc(
                functionName = "rpc_submit_checksheet",
                body = serializablePayload,
                encode = { json.encodeToString(PayloadChecksheet.serializer(), it) },
                decode = { it.trim().removeSurrounding("\"") }
            )

            resultId
        }
    }

    suspend fun getPartPickerItems(tipeProses: String): NetworkResult<List<PartPickerItem>> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.ViewChecksheetPartPicker,
                query = "komoditas=eq.$tipeProses&order=uniq_no.asc",
                decode = { json.decodeFromString(ListSerializer(PartPickerItem.serializer()), it) }
            )
        }
    }
}
