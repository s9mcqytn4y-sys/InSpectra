package com.primaraya.inspectra.fitur.checksheet.domain

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
enum class TipeProses {
    PRESS,
    SEWING,
    CUTTING,
    MATERIAL,
    PASS_THROUGH,
    CONSUMABLE
}

@Serializable
enum class KategoriDefect {
    MATERIAL,
    PROSES
}

@Serializable
data class PartAcuan(
    val no: Int,
    val nomorPart: String?,
    val uniqNo: String,
    val namaPart: String,
    val model: String?,
    val customer: String?,
    val komoditas: TipeProses,
    val lokasiGambar: String? = null
)

@Serializable
data class SlotNg(
    val slotId: String,
    val labelWaktu: String,
    val jumlah: Int = 0
)

@Serializable
data class InputDefect(
    val idDefect: String,
    val namaDefect: String,
    val kategori: KategoriDefect,
    val jumlahNg: Int = 0,
    val detailSlot: List<SlotNg> = emptyList()
) {
    val totalNgDariSlot: Int get() = detailSlot.sumOf { it.jumlah }
    val slotMatch: Boolean get() = detailSlot.isEmpty() || totalNgDariSlot == jumlahNg
}



@Serializable
data class MaterialAcuan(
    val no: Int,
    val namaSupplier: String,
    val namaMaterial: String,
    val spec: String?,
    val satuan: String?
)

@Serializable
data class MaterialPartAcuan(
    val barisExcel: Int,
    val uniqNo: String,
    val nomorPart: String?,
    val namaPart: String,
    val komoditas: TipeProses,
    val materialDigunakan: String,
    val namaSupplier: String?,
    val potensiDefectMaterial: List<String>,
    val lebar: Double? = null,
    val panjang: Double? = null,
    val tebalMm: Double? = null,
    val beratGsmGr: Double? = null,
    val qty: Double? = null,
    val satuan: String? = null,
    val specAsli: String? = null
)

@Serializable
data class RingkasanPartChecksheet(
    val uniqNo: String,
    val nomorPart: String?,
    val namaPart: String,
    val komoditas: TipeProses,
    val daftarMaterial: List<MaterialPartAcuan> = emptyList(),
    val daftarDefect: List<InputDefect>,
    val lokasiGambar: String? = null,
    val jumlahDiperiksa: Int = 0,
    val terbuka: Boolean = false
) {
    val jumlahNg: Int
        get() = daftarDefect.sumOf { it.jumlahNg }

    val jumlahOk: Int
        get() = (jumlahDiperiksa - jumlahNg).coerceAtLeast(0)

    val kuantitasTidakValid: Boolean
        get() = jumlahDiperiksa < 0 || jumlahNg > jumlahDiperiksa

    val rasioNg: Float
        get() = if (totalDiperiksa > 0) {
            (jumlahNg.toFloat() / jumlahDiperiksa.toFloat()) * 100f
        } else {
            0f
        }

    private val totalDiperiksa: Int get() = jumlahDiperiksa

    val rasioNgSatuDesimal: Float
        get() = (rasioNg * 10f).roundToInt() / 10f
}

@Serializable
data class PayloadChecksheet(
    val versiPayload: String = "fase-mvi-supabase",
    val tipeProses: String,
    val dibuatPadaMillis: Long,
    val totalDiperiksa: Int,
    val totalOk: Int,
    val totalNg: Int,
    val rasioNgGlobal: Float,
    val daftarPart: List<PayloadPartDiperiksa>
)

@Serializable
data class PayloadPartDiperiksa(
    val uniqNo: String,
    val nomorPart: String?,
    val namaPart: String,
    val komoditas: String,
    val jumlahDiperiksa: Int,
    val jumlahOk: Int,
    val jumlahNg: Int,
    val rasioNg: Float,
    val daftarMaterial: List<String> = emptyList(),
    val daftarDefectNg: List<InputDefect>
)
