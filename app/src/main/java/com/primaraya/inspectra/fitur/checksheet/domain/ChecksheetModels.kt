package com.primaraya.inspectra.fitur.checksheet.domain

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.SerialName
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
    val no: Int = 0,
    @SerialName("part_no") val nomorPart: String?,
    @SerialName("uniq_no") val uniqNo: String,
    @SerialName("nama_part") val namaPart: String,
    val model: String?,
    val customer: String?,
    val komoditas: TipeProses,
    @SerialName("image_url") val lokasiGambar: String? = null
)

@Serializable
data class SlotNg(
    val slotId: String,
    val labelWaktu: String,
    val jumlah: Int = 0
)

@Serializable
data class InputDefect(
    @SerialName("id_defect") val idDefect: String,
    @SerialName("nama_defect") val namaDefect: String,
    @SerialName("kategori") val kategori: KategoriDefect,
    @SerialName("jumlah_ng") val jumlahNg: Int = 0,
    @SerialName("detail_slot") val detailSlot: List<SlotNg> = emptyList()
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
    val potensiDefectMaterial: ImmutableList<String> = persistentListOf(),
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
    val daftarMaterial: ImmutableList<MaterialPartAcuan> = persistentListOf(),
    val daftarDefect: ImmutableList<InputDefect>,
    val defectTersembunyi: Set<String> = emptySet(),
    val lokasiGambar: String? = null,
    val jumlahDiperiksa: Int = 0,
    val terbuka: Boolean = false
) {
    val jumlahNg: Int
        get() = daftarDefect.filter { it.idDefect !in defectTersembunyi }.sumOf { it.jumlahNg }

    val daftarDefectAktif: List<InputDefect>
        get() = daftarDefect.filter { it.idDefect !in defectTersembunyi }

    val jumlahOk: Int
        get() = (jumlahDiperiksa - jumlahNg).coerceAtLeast(0)

    val kuantitasTidakValid: Boolean
        get() = jumlahDiperiksa < 0 || jumlahNg > jumlahDiperiksa

    val rasioNg: Float
        get() = if (jumlahDiperiksa > 0) {
            (jumlahNg.toFloat() / jumlahDiperiksa.toFloat()) * 100f
        } else {
            0f
        }

    val rasioNgSatuDesimal: Float
        get() = (rasioNg * 10f).roundToInt() / 10f
}

@Serializable
data class PartPickerItem(
    val uniq_no: String,
    val part_no: String? = null,
    val nama_part: String,
    val model: String? = null,
    val customer: String? = null,
    val komoditas: String,
    val image_url: String? = null,
    val menggunakan_default: Boolean = true,
    val jumlah_material: Int = 0,
    val jumlah_defect: Int = 0,
    val status_input: String = "BELUM_SIAP"
)

@Serializable
data class PayloadChecksheet(
    @SerialName("versi_payload") val versiPayload: String = "fase-mvi-supabase",
    @SerialName("tipe_proses") val tipeProses: String,
    @SerialName("dibuat_pada_millis") val dibuatPadaMillis: Long,
    @SerialName("total_diperiksa") val totalDiperiksa: Int,
    @SerialName("total_ok") val totalOk: Int,
    @SerialName("total_ng") val totalNg: Int,
    @SerialName("rasio_ng_global") val rasioNgGlobal: Float,
    @SerialName("daftar_part") val daftarPart: List<PayloadPartDiperiksa>
)

@Serializable
data class PayloadPartDiperiksa(
    @SerialName("uniq_no") val uniqNo: String,
    @SerialName("nomor_part") val nomorPart: String?,
    @SerialName("nama_part") val namaPart: String,
    @SerialName("komoditas") val komoditas: String,
    @SerialName("jumlah_diperiksa") val jumlahDiperiksa: Int,
    @SerialName("jumlah_ok") val jumlahOk: Int,
    @SerialName("jumlah_ng") val jumlahNg: Int,
    @SerialName("rasio_ng") val rasioNg: Float,
    @SerialName("daftar_material") val daftarMaterial: List<String> = emptyList(),
    @SerialName("daftar_defect_ng") val daftarDefectNg: List<InputDefect>
)
