package com.primaraya.inspectra.fitur.checksheet.data

import com.primaraya.inspectra.core.data.DatabaseDriver
import com.primaraya.inspectra.core.data.RemoteTable
import com.primaraya.inspectra.core.data.SupabasePgRestDriver
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.network.runNetworkCatching
import com.primaraya.inspectra.fitur.checksheet.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class SupabaseChecksheetRepository(
    private val driver: DatabaseDriver = SupabasePgRestDriver()
) : ChecksheetRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    override suspend fun submitChecksheet(
        payload: PayloadChecksheet
    ): NetworkResult<String> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            // 1. Simpan Sesi
            val sesiDto = SesiChecksheetDto(
                tipe_proses = payload.tipeProses,
                total_diperiksa = payload.totalDiperiksa,
                total_ok = payload.totalOk,
                total_ng = payload.totalNg,
                rasio_ng_global = payload.rasioNgGlobal.toDouble()
            )

            val insertedSesiList = driver.insertReturning(
                table = RemoteTable.SesiChecksheet,
                body = sesiDto,
                encode = { json.encodeToString(SesiChecksheetDto.serializer(), it) },
                decode = { json.decodeFromString(ListSerializer(SesiChecksheetDto.serializer()), it) }
            )
            
            val sesiId = insertedSesiList.firstOrNull()?.id ?: error("Sesi gagal dibuat.")

            // 2. Simpan Item (Parts)
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

            val insertedItems = driver.insertReturning(
                table = RemoteTable.ItemChecksheet,
                body = itemDtos,
                encode = { json.encodeToString(ListSerializer(ItemChecksheetDto.serializer()), it) },
                decode = { json.decodeFromString(ListSerializer(ItemChecksheetDto.serializer()), it) }
            )

            val itemIdByUniq = insertedItems.mapNotNull { it.id?.let { id -> it.uniq_no to id } }.toMap()

            // 3. Simpan Defect & Detail Slot
            val allDefectSlots = mutableListOf<DefectSlotChecksheetDto>()

            payload.daftarPart.forEach { part ->
                val itemId = itemIdByUniq[part.uniqNo] ?: return@forEach
                
                // Simpan Defect (Returning ID)
                val defectDtos = part.daftarDefectNg.map { defect ->
                    DefectChecksheetDto(
                        id_item = itemId,
                        id_defect = defect.idDefect,
                        nama_defect = defect.namaDefect,
                        kategori = defect.kategori.name,
                        jumlah = defect.jumlahNg
                    )
                }

                if (defectDtos.isNotEmpty()) {
                    val insertedDefects = driver.insertReturning(
                        table = RemoteTable.DefectChecksheet,
                        body = defectDtos,
                        encode = { json.encodeToString(ListSerializer(DefectChecksheetDto.serializer()), it) },
                        decode = { json.decodeFromString(ListSerializer(DefectChecksheetDto.serializer()), it) }
                    )

                    // Map ID ke Slot
                    insertedDefects.forEach { defRow ->
                        val originalDefect = part.daftarDefectNg.firstOrNull { it.idDefect == defRow.id_defect }
                        originalDefect?.detailSlot?.filter { it.jumlah > 0 }?.forEach { slot ->
                            allDefectSlots.add(
                                DefectSlotChecksheetDto(
                                    id_defect_checksheet = defRow.id!!,
                                    slot_waktu_id = slot.slotId,
                                    jumlah = slot.jumlah
                                )
                            )
                        }
                    }
                }
            }

            if (allDefectSlots.isNotEmpty()) {
                driver.upsert(
                    table = RemoteTable.DefectSlotChecksheet,
                    body = allDefectSlots,
                    encode = { json.encodeToString(ListSerializer(DefectSlotChecksheetDto.serializer()), it) }
                )
            }

            // 4. Handle Detail Cutting
            val cuttingDtos = payload.daftarPart.mapNotNull { part ->
                val detail = part.detailCutting ?: return@mapNotNull null
                if (detail.noLot.isNullOrBlank() && detail.noRoll.isNullOrBlank()) return@mapNotNull null
                
                val itemId = itemIdByUniq[part.uniqNo] ?: return@mapNotNull null
                DetailCuttingDto(id_item = itemId, no_lot = detail.noLot, no_roll = detail.noRoll, size_cutting_cm = detail.sizeCuttingCm, waste = detail.waste, pic = detail.pic)
            }

            if (cuttingDtos.isNotEmpty()) {
                driver.upsert(RemoteTable.DetailCutting, cuttingDtos, { json.encodeToString(ListSerializer(DetailCuttingDto.serializer()), it) })
            }

            sesiId
        }
    }
}
