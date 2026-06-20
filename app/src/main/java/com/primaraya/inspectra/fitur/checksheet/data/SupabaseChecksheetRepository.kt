package com.primaraya.inspectra.fitur.checksheet.data

import com.primaraya.inspectra.core.database.SupabaseRestClient
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.network.runNetworkCatching
import com.primaraya.inspectra.fitur.checksheet.domain.PayloadChecksheet
import com.primaraya.inspectra.fitur.checksheet.domain.DefectChecksheetDto
import com.primaraya.inspectra.fitur.checksheet.domain.ItemChecksheetDto
import com.primaraya.inspectra.fitur.checksheet.domain.SesiChecksheetDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseChecksheetRepository(
    private val client: SupabaseRestClient = SupabaseRestClient()
) : ChecksheetRepository {

    override suspend fun submitChecksheet(
        payload: PayloadChecksheet
    ): NetworkResult<String> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            val sesiDto = SesiChecksheetDto(
                tipe_proses = payload.tipeProses,
                total_diperiksa = payload.totalDiperiksa,
                total_ok = payload.totalOk,
                total_ng = payload.totalNg,
                rasio_ng_global = payload.rasioNgGlobal.toDouble()
            )

            val insertedSesi = client.insertReturning<SesiChecksheetDto, List<SesiChecksheetDto>>(
                table = "e_sesi_checksheet",
                body = sesiDto
            ).firstOrNull() ?: error("Sesi belum berhasil dibuat.")

            val sesiId = insertedSesi.id
                ?: error("ID Sesi tidak dikembalikan.")

            val itemDtos = payload.daftarPart.map { part ->
                ItemChecksheetDto(
                    id_sesi = sesiId,
                    uniq_no = part.uniqNo,
                    jumlah_diperiksa = part.jumlahDiperiksa,
                    jumlah_ok = part.jumlahOk,
                    jumlah_ng = part.jumlahNg,
                    rasio_ng = part.rasioNg.toDouble()
                )
            }

            val insertedItems = client.insertReturning<List<ItemChecksheetDto>, List<ItemChecksheetDto>>(
                table = "e_item_checksheet",
                body = itemDtos
            )

            val itemIdByUniq = insertedItems
                .mapNotNull { item ->
                    val id = item.id ?: return@mapNotNull null
                    item.uniq_no to id
                }
                .toMap()

            val defectDtos = payload.daftarPart.flatMap { part ->
                val itemId = itemIdByUniq[part.uniqNo] ?: return@flatMap emptyList()

                part.daftarDefectNg.map { defect ->
                    DefectChecksheetDto(
                        id_item = itemId,
                        id_defect = defect.idDefect,
                        nama_defect = defect.namaDefect,
                        kategori = defect.kategori.name,
                        jumlah = defect.jumlahNg
                    )
                }
            }

            if (defectDtos.isNotEmpty()) {
                client.upsert(table = "e_defect_checksheet", body = defectDtos)
            }

            sesiId
        }
    }
}
