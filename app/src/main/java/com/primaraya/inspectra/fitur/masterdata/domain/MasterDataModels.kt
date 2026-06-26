package com.primaraya.inspectra.fitur.masterdata.domain

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
    val id: String? = null,
    val kode_supplier: String? = null,
    val nama_supplier: String,
    val kategori: String? = null,
    val aktif: Boolean = true
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class MasterPartDto(
    val id: String? = null,
    val part_no: String? = null,
    val uniq_no: String,
    val nama_part: String,
    val model: String? = null,
    val customer: String? = null,
    val komoditas: String,
    val total_item_per_kanban: Int? = null,
    val sample_item_per_kanban: Int? = null,
    val sample_cycle_note: String? = null,
    val lokasi_gambar: String? = null,
    val aktif: Boolean = true,
    val status_kelengkapan: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val butuh_review: Boolean = false,
    val catatan_review: String? = null,
    val jumlah_material: Int? = null,
    val jumlah_defect_proses: Int? = null,
    val jumlah_defect_material: Int? = null,
    val jumlah_defect: Int? = null,
    val status_input: String? = null
)

@Serializable
data class MasterPartDefectDto(
    val id: String? = null,
    val uniq_no: String,
    val id_defect: String,
    val urutan: Int = 1,
    val wajib_check: Boolean = true,
    val aktif: Boolean = true
)

@Serializable
data class MasterPartMaterialDto(
    val id: String? = null,
    val uniq_no: String,
    val material_id: String,
    val material_spec_id: String? = null,
    val urutan: Int = 1,
    val label_material: String,
    val aktif: Boolean = true
)

@Serializable
data class MasterMaterialDefectDto(
    val id: String? = null,
    val material_id: String,
    val id_defect: String,
    val urutan: Int = 1,
    val wajib_check: Boolean = true,
    val aktif: Boolean = true
)

@Serializable
data class MasterMaterialDto(
    val id: String? = null,
    val supplier_id: String? = null,
    val supplier: String? = null,
    val nama_material: String,
    val spec: String? = null,
    val satuan: String? = null,
    val lebar_roll_cm: Double? = null,
    val panjang_roll_cm: Double? = null,
    val tebal_mm: Double? = null,
    val berat_gsm: Double? = null,
    val gramasi_gsm: Double? = null,
    val warna: String? = null,
    val catatan_spesifikasi: String? = null,
    val aktif: Boolean = true
)

@Serializable
data class MasterMaterialSpecDto(
    val id: String? = null,
    val material_id: String,
    val spec_asli: String? = null,
    val satuan: String? = null,
    val lebar_value: Double? = null,
    val panjang_value: Double? = null,
    val tebal_value: Double? = null,
    val berat_value: Double? = null,
    val qty_value: Double? = null,
    val warna: String? = null,
    val grade: String? = null,
    val aktif: Boolean = true
)

@Serializable
data class MasterDefectDto(
    val id_defect: String,
    val nama_defect: String,
    val kategori: String,
    val aktif: Boolean = true
)

@Serializable
data class MasterSlotWaktuDto(
    val id: String,
    val kode_slot: String,
    val tipe_proses: String,
    val nama_shift: String,
    val label_waktu: String,
    val urutan: Int
)

@Serializable
data class MasterPartEffectiveDefectDto(
    val relation_id: String? = null,
    val uniq_no: String,
    val id_defect: String,
    val nama_defect: String? = null,
    val kategori: String? = null,
    val sumber_defect: String, // PROSES_PART atau MATERIAL
    val material_id: String? = null,
    val nama_material: String? = null,
    val urutan: Int = 1,
    val wajib_check: Boolean = true,
    val aktif: Boolean = true
)
