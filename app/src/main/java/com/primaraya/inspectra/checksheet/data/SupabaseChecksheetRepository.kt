package com.primaraya.inspectra.checksheet.data

import com.primaraya.inspectra.checksheet.domain.DefectChecksheetDto
import com.primaraya.inspectra.checksheet.domain.ItemChecksheetDto
import com.primaraya.inspectra.checksheet.domain.SesiChecksheetDto
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.network.SupabaseProvider
import com.primaraya.inspectra.core.network.runNetworkCatching
import com.primaraya.inspectra.domain.model.PayloadChecksheet
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseChecksheetRepository : ChecksheetRepository {
    private val client = SupabaseProvider.client

    override suspend fun submitChecksheet(payload: PayloadChecksheet): NetworkResult<String> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            // 1. Insert Sesi
            val sesiDto = SesiChecksheetDto(
                tipe_proses = payload.tipeProses,
                total_diperiksa = payload.totalDiperiksa,
                total_ok = payload.totalOk,
                total_ng = payload.totalNg,
                rasio_ng_global = payload.rasioNgGlobal.toDouble()
            )
            
            val insertedSesi = client.postgrest["e_sesi_checksheet"]
                .insert(sesiDto) {
                    select() // Minta data dikembalikan setelah insert
                }
                .decodeSingle<SesiChecksheetDto>()
                
            val sesiId = insertedSesi.id ?: throw IllegalStateException("ID Sesi tidak dikembalikan oleh Supabase")

            // 2. Insert Items and Defects
            payload.daftarPart.forEach { part ->
                val itemDto = ItemChecksheetDto(
                    id_sesi = sesiId,
                    uniq_no = part.uniqNo,
                    jumlah_diperiksa = part.jumlahDiperiksa,
                    jumlah_ok = part.jumlahOk,
                    jumlah_ng = part.jumlahNg,
                    rasio_ng = part.rasioNg.toDouble()
                )
                
                val insertedItem = client.postgrest["e_item_checksheet"]
                    .insert(itemDto) { select() }
                    .decodeSingle<ItemChecksheetDto>()
                    
                val itemId = insertedItem.id ?: throw IllegalStateException("ID Item tidak dikembalikan oleh Supabase")
                
                // 3. Insert Defects if any
                val defectsToInsert = part.daftarDefectNg.filter { it.jumlahNg > 0 }.map { defect ->
                    DefectChecksheetDto(
                        id_item = itemId,
                        id_defect = defect.idDefect,
                        nama_defect = defect.namaDefect,
                        kategori = defect.kategori.name,
                        jumlah = defect.jumlahNg
                    )
                }
                
                if (defectsToInsert.isNotEmpty()) {
                    client.postgrest["e_defect_checksheet"].insert(defectsToInsert)
                }
            }
            
            sesiId
        }
    }
}
