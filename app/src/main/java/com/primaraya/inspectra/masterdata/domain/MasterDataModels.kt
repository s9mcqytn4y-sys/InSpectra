package com.primaraya.inspectra.masterdata.domain

import kotlinx.serialization.Serializable

@Serializable
data class MasterPartDto(
    val id: String? = null,
    val part_no: String? = null,
    val uniq_no: String,
    val nama_part: String,
    val model: String? = null,
    val customer: String? = null,
    val komoditas: String? = null,
    val lokasi_gambar: String? = null
)

@Serializable
data class MasterMaterialDto(
    val id: String? = null,
    val supplier: String? = null,
    val nama_material: String,
    val spec: String? = null,
    val satuan: String? = null
)
