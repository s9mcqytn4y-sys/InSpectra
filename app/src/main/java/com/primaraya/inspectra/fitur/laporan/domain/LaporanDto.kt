package com.primaraya.inspectra.fitur.laporan.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LaporanSubmitDto(
    val tanggal: String,
    @SerialName("tipe_proses") val tipeProses: String,
    @SerialName("mp_direct") val mpDirect: Int,
    @SerialName("mp_indirect") val mpIndirect: Int,
    @SerialName("jkn_hour") val jknHour: Int,
    @SerialName("jkn_menit") val jknMenit: Int,
    @SerialName("ot_prod") val otProd: Double,
    @SerialName("ot_non") val otNon: Double,
    @SerialName("bantuan_keluar") val bantuanKeluar: Int,
    @SerialName("bantuan_masuk") val bantuanMasuk: Int,
    val details: List<DetailLaporanDto>
)

@Serializable
data class DetailLaporanDto(
    @SerialName("id_part") val idPart: String,
    val planning: Int,
    val actual: Int,
    val ng: Int
)
