package com.primaraya.inspectra.fitur.masterdata.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi

enum class FilterDataInduk {
    SEMUA,
    SIAP_INPUT,
    PERLU_VERIFIKASI,
    TANPA_MATERIAL,
    TANPA_DEFECT,
    NONAKTIF
}

@Serializable
data class MasterSupplierDto(
    @SerialName("id") val id: String? = null,
    @SerialName("kode_supplier") val kode_supplier: String? = null,
    @SerialName("nama_supplier") val nama_supplier: String,
    @SerialName("kategori") val kategori: String? = null,
    @SerialName("aktif") val aktif: Boolean = true
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class MasterPartDto(
    @SerialName("id") val id: String? = null,
    @SerialName("part_no") val part_no: String? = null,
    @SerialName("uniq_no") val uniq_no: String,
    @SerialName("nama_part") val nama_part: String,
    @SerialName("model") val model: String? = null,
    @SerialName("customer") val customer: String? = null,
    @SerialName("komoditas") val komoditas: String,
    @SerialName("total_item_per_kanban") val total_item_per_kanban: Int? = null,
    @SerialName("sample_item_per_kanban") val sample_item_per_kanban: Int? = null,
    @SerialName("sample_cycle_note") val sample_cycle_note: String? = null,
    @SerialName("lokasi_gambar") val lokasi_gambar: String? = null,
    @SerialName("aktif") val aktif: Boolean = true,
    @SerialName("status_kelengkapan") val status_kelengkapan: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("butuh_review") val butuh_review: Boolean = false,
    @SerialName("catatan_review") val catatan_review: String? = null,
    @SerialName("jumlah_material") val jumlah_material: Int? = null,
    @SerialName("jumlah_defect_proses") val jumlah_defect_proses: Int? = null,
    @SerialName("jumlah_defect_material") val jumlah_defect_material: Int? = null,
    @SerialName("jumlah_defect") val jumlah_defect: Int? = null,
    @SerialName("status_input") val status_input: String? = null,
    @SerialName("daftar_material") val daftar_material: List<MasterPartMaterialDto> = emptyList()
)

@Serializable
data class MasterPartDefectDto(
    @SerialName("id") val id: String? = null,
    @SerialName("uniq_no") val uniq_no: String,
    @SerialName("id_defect") val id_defect: String,
    @SerialName("urutan") val urutan: Int = 1,
    @SerialName("wajib_check") val wajib_check: Boolean = true,
    @SerialName("aktif") val aktif: Boolean = true
)

@Serializable
data class MasterPartMaterialDto(
    @SerialName("id") val id: String? = null,
    @SerialName("uniq_no") val uniq_no: String,
    @SerialName("material_id") val material_id: String,
    @SerialName("material_spec_id") val material_spec_id: String? = null,
    @SerialName("urutan") val urutan: Int = 1,
    @SerialName("label_material") val label_material: String,
    @SerialName("aktif") val aktif: Boolean = true
)

@Serializable
data class MasterMaterialDefectDto(
    @SerialName("id") val id: String? = null,
    @SerialName("material_id") val material_id: String,
    @SerialName("id_defect") val id_defect: String,
    @SerialName("urutan") val urutan: Int = 1,
    @SerialName("wajib_check") val wajib_check: Boolean = true,
    @SerialName("aktif") val aktif: Boolean = true
)

@Serializable
data class MasterMaterialDto(
    @SerialName("id") val id: String? = null,
    @SerialName("supplier_id") val supplier_id: String? = null,
    @SerialName("supplier") val supplier: String? = null,
    @SerialName("nama_material") val nama_material: String,
    @SerialName("spec") val spec: String? = null,
    @SerialName("satuan") val satuan: String? = null,
    @SerialName("lebar_roll_cm") val lebar_roll_cm: Double? = null,
    @SerialName("panjang_roll_cm") val panjang_roll_cm: Double? = null,
    @SerialName("tebal_mm") val tebal_mm: Double? = null,
    @SerialName("berat_gsm") val berat_gsm: Double? = null,
    @SerialName("gramasi_gsm") val gramasi_gsm: Double? = null,
    @SerialName("warna") val warna: String? = null,
    @SerialName("catatan_spesifikasi") val catatan_spesifikasi: String? = null,
    @SerialName("aktif") val aktif: Boolean = true
)

@Serializable
data class MasterMaterialSpecDto(
    @SerialName("id") val id: String? = null,
    @SerialName("material_id") val material_id: String,
    @SerialName("spec_asli") val spec_asli: String? = null,
    @SerialName("satuan") val satuan: String? = null,
    @SerialName("lebar_value") val lebar_value: Double? = null,
    @SerialName("panjang_value") val panjang_value: Double? = null,
    @SerialName("tebal_value") val tebal_value: Double? = null,
    @SerialName("berat_value") val berat_value: Double? = null,
    @SerialName("qty_value") val qty_value: Double? = null,
    @SerialName("warna") val warna: String? = null,
    @SerialName("grade") val grade: String? = null,
    @SerialName("aktif") val aktif: Boolean = true
)

@Serializable
data class MasterDefectDto(
    @SerialName("id_defect") val id_defect: String,
    @SerialName("nama_defect") val nama_defect: String,
    @SerialName("kategori") val kategori: String,
    @SerialName("aktif") val aktif: Boolean = true
)

@Serializable
data class MasterSlotWaktuDto(
    @SerialName("id") val id: String,
    @SerialName("kode_slot") val kode_slot: String,
    @SerialName("tipe_proses") val tipe_proses: String,
    @SerialName("nama_shift") val nama_shift: String,
    @SerialName("label_waktu") val label_waktu: String,
    @SerialName("urutan") val urutan: Int
)

@Serializable
data class MasterPartEffectiveDefectDto(
    @SerialName("relation_id") val relation_id: String? = null,
    @SerialName("uniq_no") val uniq_no: String,
    @SerialName("id_defect") val id_defect: String,
    @SerialName("nama_defect") val nama_defect: String? = null,
    @SerialName("kategori") val kategori: String? = null,
    @SerialName("sumber_defect") val sumber_defect: String, // PROSES_PART atau MATERIAL
    @SerialName("material_id") val material_id: String? = null,
    @SerialName("nama_material") val nama_material: String? = null,
    @SerialName("urutan") val urutan: Int = 1,
    @SerialName("wajib_check") val wajib_check: Boolean = true,
    @SerialName("aktif") val aktif: Boolean = true
)
