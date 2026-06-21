package com.primaraya.inspectra.fitur.cutting.domain

import kotlinx.serialization.Serializable
import kotlin.math.round

@Serializable
data class UkuranCuttingAcuan(
    val id: String,
    val ukuran_cutting_cm: Double,
    val urutan: Int
)

@Serializable
data class OpsiMaterialCutting(
    val material_id: String,
    val nama_material: String,
    val spec_ringkas: String = "",
    val satuan: String,
    val daftar_ukuran_cutting: List<UkuranCuttingAcuan> = emptyList()
) {
    val labelPilihan: String
        get() = listOf(nama_material, spec_ringkas.takeIf(String::isNotBlank), satuan.takeIf(String::isNotBlank))
            .filterNotNull()
            .joinToString(" - ")
}

@Serializable
data class OpsiPartUkuranCutting(
    val uniq_no: String,
    val part_no: String? = null,
    val nama_part: String,
    val model: String? = null,
    val komoditas: String,
    val daftar_ukuran_cutting: List<UkuranCuttingAcuan> = emptyList()
) {
    val labelPilihan: String
        get() = listOf(uniq_no, part_no, nama_part).filterNotNull().filter(String::isNotBlank).joinToString(" - ")
}

@Serializable
data class InputDefectCutting(
    val idDefect: String,
    val namaDefect: String,
    val idSlotWaktu: String? = null,
    val jumlahLayerTerdampak: Int,
    val panjangDefectCm: Double? = null
)

@Serializable
data class InputBatchCutting(
    val tanggalPemeriksaan: String,
    val namaShift: String = "SHIFT_1",
    val materialId: String = "",
    val namaMaterial: String = "",
    val spesifikasiMaterial: String = "",
    val uniqNoPart: String = "",
    val namaPart: String = "",
    val idReferensiUkuranPart: String? = null,
    val nomorLotRoll: String = "",
    val nomorRoll: String = "",
    val ukuranCuttingCm: String = "",
    val qtyLayerOk: String = "",
    val qtyLayerNg: String = "",
    val wastePanjangCm: String = "",
    val catatan: String = "",
    val daftarDefect: List<InputDefectCutting> = emptyList()
) {
    val ukuranCuttingAngka: Double? get() = ukuranCuttingCm.toDoubleOrNull()
    val qtyLayerOkAngka: Int get() = qtyLayerOk.toIntOrNull() ?: 0
    val qtyLayerNgAngka: Int get() = qtyLayerNg.toIntOrNull() ?: 0
    val wastePanjangAngka: Double get() = wastePanjangCm.toDoubleOrNull() ?: 0.0
    val totalLayer: Int get() = qtyLayerOkAngka + qtyLayerNgAngka
    val totalLayerDefect: Int get() = daftarDefect.sumOf { it.jumlahLayerTerdampak }
    val rasioNgLayer: Double get() = if (totalLayer == 0) 0.0 else pembulatan(qtyLayerNgAngka.toDouble() / totalLayer * 100)
    val estimasiPanjangOkCm: Double get() = pembulatan((ukuranCuttingAngka ?: 0.0) * qtyLayerOkAngka)
    val estimasiPanjangNgCm: Double get() = pembulatan((ukuranCuttingAngka ?: 0.0) * qtyLayerNgAngka)
    val rasioWastePanjang: Double get() {
        val dasar = estimasiPanjangOkCm + estimasiPanjangNgCm + wastePanjangAngka
        return if (dasar == 0.0) 0.0 else pembulatan(wastePanjangAngka / dasar * 100)
    }

    private fun pembulatan(nilai: Double): Double = round(nilai * 1000) / 1000
}

@Serializable
data class RingkasanHarianCutting(
    val id_sesi: String,
    val tanggal_pemeriksaan: String,
    val nama_shift: String,
    val nama_line: String? = null,
    val total_batch: Int,
    val total_layer_ok: Int,
    val total_layer_ng: Int,
    val total_panjang_ok_cm: Double,
    val total_panjang_ng_cm: Double,
    val total_waste_cm: Double,
    val rasio_ng_layer: Double,
    val rasio_waste_panjang: Double
)

object ValidatorBatchCutting {
    fun validasi(input: InputBatchCutting): List<String> = buildList {
        if (input.materialId.isBlank()) add("Material wajib dipilih.")
        if ((input.ukuranCuttingAngka ?: 0.0) <= 0) add("Ukuran cutting harus lebih besar dari nol.")
        if (input.qtyLayerOk.toIntOrNull() == null || input.qtyLayerOkAngka < 0) add("Jumlah layer OK tidak boleh negatif.")
        if (input.qtyLayerNg.toIntOrNull() == null || input.qtyLayerNgAngka < 0) add("Jumlah layer NG tidak boleh negatif.")
        if (input.totalLayer <= 0) add("Isi minimal satu layer OK atau NG.")
        if (input.wastePanjangCm.toDoubleOrNull() == null || input.wastePanjangAngka < 0) add("Waste panjang tidak boleh negatif.")
        if (input.qtyLayerNgAngka > 0 && input.daftarDefect.isEmpty()) add("Tambahkan minimal satu defect untuk layer NG.")
        if (input.totalLayerDefect > input.qtyLayerNgAngka) add("Total layer defect tidak boleh melebihi layer NG.")
        input.daftarDefect.forEach { defect ->
            if (defect.jumlahLayerTerdampak <= 0) add("Jumlah layer defect harus lebih besar dari nol.")
            if (defect.panjangDefectCm != null && defect.panjangDefectCm <= 0) add("Panjang defect harus lebih besar dari nol.")
        }
    }
}
