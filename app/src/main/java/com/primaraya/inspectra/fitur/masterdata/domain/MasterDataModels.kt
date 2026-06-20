package com.primaraya.inspectra.fitur.masterdata.domain

import kotlinx.serialization.Serializable

@Serializable
data class MasterSupplierDto(
    val id: String? = null,
    val kode_supplier: String? = null,
    val nama_supplier: String,
    val kategori: String? = null,
    val aktif: Boolean = true
)

@Serializable
data class MasterPartDto(
    val id: String? = null,
    val part_no: String? = null,
    val uniq_no: String,
    val nama_part: String,
    val model: String? = null,
    val customer: String? = null,
    val komoditas: String,
    val lokasi_gambar: String? = null,
    val aktif: Boolean = true
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
data class MasterMaterialDto(
    val id: String? = null,
    val supplier: String? = null,
    val nama_material: String,
    val spec: String? = null,
    val satuan: String? = null,
    val aktif: Boolean = true
)

@Serializable
data class MasterDefectDto(
    val id_defect: String,
    val nama_defect: String,
    val kategori: String,
    val aktif: Boolean = true
)
