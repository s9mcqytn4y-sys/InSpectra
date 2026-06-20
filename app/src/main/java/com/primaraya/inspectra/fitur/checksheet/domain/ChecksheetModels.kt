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
data class InputDefect(
    val idDefect: String,
    val namaDefect: String,
    val kategori: KategoriDefect,
    val jumlahNg: Int = 0
)

@Serializable
data class DetailCutting(
    val noLot: String? = null,
    val noRoll: String? = null,
    val sizeCuttingCm: String? = null,
    val waste: Double? = null,
    val pic: String? = null
)

@Serializable
data class RingkasanPartChecksheet(
    val uniqNo: String,
    val nomorPart: String?,
    val namaPart: String,
    val komoditas: TipeProses,
    val daftarMaterial: List<String> = emptyList(), // Not used for now
    val daftarDefect: List<InputDefect>,
    val lokasiGambar: String? = null,
    val jumlahDiperiksa: Int = 0,
    val terbuka: Boolean = false,
    val detailCutting: DetailCutting? = null
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
    val versiPayload: String = "fase-mvi-supabase-cutting",
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
    val daftarDefectNg: List<InputDefect>,
    val detailCutting: DetailCutting? = null
)
