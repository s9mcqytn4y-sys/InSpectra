package com.primaraya.inspectra.fitur.checksheet.domain

import kotlinx.serialization.Serializable

@Serializable
data class SesiChecksheetDto(
    val id: String? = null,
    val tipe_proses: String,
    val total_diperiksa: Int,
    val total_ok: Int,
    val total_ng: Int,
    val rasio_ng_global: Double,
    val created_at: String? = null
)

@Serializable
data class ItemChecksheetDto(
    val id: String? = null,
    val id_sesi: String,
    val uniq_no: String,
    val jumlah_diperiksa: Int,
    val jumlah_ok: Int,
    val jumlah_ng: Int,
    val rasio_ng: Double
)

@Serializable
data class DefectChecksheetDto(
    val id: String? = null,
    val id_item: String,
    val id_defect: String,
    val nama_defect: String,
    val kategori: String,
    val jumlah: Int
)
