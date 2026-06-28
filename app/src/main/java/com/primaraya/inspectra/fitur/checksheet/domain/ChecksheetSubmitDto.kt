package com.primaraya.inspectra.fitur.checksheet.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SesiChecksheetDto(
    @SerialName("id") val id: String? = null,
    @SerialName("tipe_proses") val tipe_proses: String,
    @SerialName("total_diperiksa") val total_diperiksa: Int,
    @SerialName("total_ok") val total_ok: Int,
    @SerialName("total_ng") val total_ng: Int,
    @SerialName("rasio_ng_global") val rasio_ng_global: Double,
    @SerialName("created_at") val created_at: String? = null
)

@Serializable
data class ItemChecksheetDto(
    @SerialName("id") val id: String? = null,
    @SerialName("id_sesi") val id_sesi: String,
    @SerialName("uniq_no") val uniq_no: String,
    @SerialName("jumlah_diperiksa") val jumlah_diperiksa: Int,
    @SerialName("jumlah_ok") val jumlah_ok: Int,
    @SerialName("jumlah_ng") val jumlah_ng: Int,
    @SerialName("rasio_ng") val rasio_ng: Double
)

@Serializable
data class DefectChecksheetDto(
    @SerialName("id") val id: String? = null,
    @SerialName("id_item") val id_item: String,
    @SerialName("id_defect") val id_defect: String,
    @SerialName("nama_defect") val nama_defect: String,
    @SerialName("kategori") val kategori: String,
    @SerialName("jumlah") val jumlah: Int
)

@Serializable
data class DefectSlotChecksheetDto(
    @SerialName("id") val id: String? = null,
    @SerialName("id_defect_checksheet") val id_defect_checksheet: String,
    @SerialName("slot_waktu_id") val slot_waktu_id: String,
    @SerialName("jumlah") val jumlah: Int
)
