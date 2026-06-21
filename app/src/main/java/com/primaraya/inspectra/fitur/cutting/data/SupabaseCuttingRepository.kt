package com.primaraya.inspectra.fitur.cutting.data

import com.primaraya.inspectra.core.data.DatabaseDriver
import com.primaraya.inspectra.core.data.RemoteTable
import com.primaraya.inspectra.core.data.SupabasePgRestDriver
import com.primaraya.inspectra.core.network.InspectraHttpClient
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.network.runNetworkCatching
import com.primaraya.inspectra.fitur.cutting.domain.InputBatchCutting
import com.primaraya.inspectra.fitur.cutting.domain.InputDefectCutting
import com.primaraya.inspectra.fitur.cutting.domain.OpsiMaterialCutting
import com.primaraya.inspectra.fitur.cutting.domain.RingkasanHarianCutting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

class SupabaseCuttingRepository(
    private val driver: DatabaseDriver = SupabasePgRestDriver()
) : CuttingRepository {

    private val json = InspectraHttpClient.json

    override suspend fun bacaOpsiMaterial(): NetworkResult<List<OpsiMaterialCutting>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.ViewCuttingMaterialOption,
                query = "select=*&order=nama_material.asc&limit=100",
                decode = { json.decodeFromString(ListSerializer(OpsiMaterialCutting.serializer()), it) }
            )
        }
    }

    override suspend fun simpanBatch(input: InputBatchCutting): NetworkResult<String> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            val totalLayer = input.totalLayer
            val sesi = SesiCuttingDto(
                tipe_proses = "CUTTING",
                tanggal_pemeriksaan = input.tanggalPemeriksaan,
                nama_shift = input.namaShift,
                nama_operator = input.namaOperator.ifBlank { null },
                nama_line = input.namaLine.ifBlank { null },
                total_diperiksa = totalLayer,
                total_ok = input.qtyLayerOkAngka,
                total_ng = input.qtyLayerNgAngka,
                rasio_ng_global = input.rasioNgLayer
            )
            val sesiTersimpan = driver.insertReturning(
                table = RemoteTable.SesiChecksheet,
                body = sesi,
                encode = { json.encodeToString(SesiCuttingDto.serializer(), it) },
                decode = { json.decodeFromString(ListSerializer(SesiCuttingDto.serializer()), it).first() }
            )
            val idSesi = requireNotNull(sesiTersimpan.id) { "Server tidak mengembalikan ID sesi Cutting." }

            val batch = BatchCuttingDto(
                id_sesi = idSesi,
                material_id = input.materialId,
                nama_material_snapshot = input.namaMaterial,
                spec_material_snapshot = input.spesifikasiMaterial.ifBlank { null },
                no_lot_roll = input.nomorLotRoll.ifBlank { null },
                no_roll = input.nomorRoll.ifBlank { null },
                ukuran_cutting_cm = requireNotNull(input.ukuranCuttingAngka),
                qty_layer_ok = input.qtyLayerOkAngka,
                qty_layer_ng = input.qtyLayerNgAngka,
                waste_panjang_cm = input.wastePanjangAngka,
                nama_operator = input.namaOperator.ifBlank { null },
                catatan = input.catatan.ifBlank { null }
            )
            val batchTersimpan = driver.insertReturning(
                table = RemoteTable.CuttingBatch,
                body = batch,
                encode = { json.encodeToString(BatchCuttingDto.serializer(), it) },
                decode = { json.decodeFromString(ListSerializer(BatchCuttingDto.serializer()), it).first() }
            )
            val idBatch = requireNotNull(batchTersimpan.id) { "Server tidak mengembalikan ID batch Cutting." }

            if (input.daftarDefect.isNotEmpty()) {
                val defect = input.daftarDefect.map { it.toDto(idBatch) }
                driver.upsert(
                    table = RemoteTable.CuttingDefectDetail,
                    body = defect,
                    encode = { json.encodeToString(ListSerializer(DefectDetailCuttingDto.serializer()), it) }
                )
            }

            idSesi
        }
    }

    override suspend fun bacaRingkasanHarian(tanggalPemeriksaan: String): NetworkResult<List<RingkasanHarianCutting>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.ViewCuttingDailySummary,
                query = "select=*&tanggal_pemeriksaan=eq.$tanggalPemeriksaan",
                decode = { json.decodeFromString(ListSerializer(RingkasanHarianCutting.serializer()), it) }
            )
        }
    }

    private fun InputDefectCutting.toDto(idBatch: String): DefectDetailCuttingDto {
        return DefectDetailCuttingDto(
            id_cutting_batch = idBatch,
            id_defect = idDefect,
            nama_defect_snapshot = namaDefect,
            slot_waktu_id = idSlotWaktu,
            jumlah_layer_terdampak = jumlahLayerTerdampak,
            panjang_defect_cm = panjangDefectCm
        )
    }
}

@Serializable
private data class SesiCuttingDto(
    val id: String? = null,
    val tipe_proses: String,
    val tanggal_pemeriksaan: String,
    val nama_shift: String,
    val nama_operator: String? = null,
    val nama_line: String? = null,
    val total_diperiksa: Int,
    val total_ok: Int,
    val total_ng: Int,
    val rasio_ng_global: Double
)

@Serializable
private data class BatchCuttingDto(
    val id: String? = null,
    val id_sesi: String,
    val material_id: String,
    val nama_material_snapshot: String,
    val spec_material_snapshot: String? = null,
    val no_lot_roll: String? = null,
    val no_roll: String? = null,
    val ukuran_cutting_cm: Double,
    val qty_layer_ok: Int,
    val qty_layer_ng: Int,
    val waste_panjang_cm: Double,
    val nama_operator: String? = null,
    val catatan: String? = null
)

@Serializable
private data class DefectDetailCuttingDto(
    val id_cutting_batch: String,
    val id_defect: String,
    val nama_defect_snapshot: String,
    val slot_waktu_id: String? = null,
    val jumlah_layer_terdampak: Int,
    val panjang_defect_cm: Double? = null
)
