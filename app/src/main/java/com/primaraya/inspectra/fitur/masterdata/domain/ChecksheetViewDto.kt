package com.primaraya.inspectra.fitur.masterdata.domain

import com.primaraya.inspectra.fitur.checksheet.domain.KategoriDefect
import kotlinx.serialization.Serializable

@Serializable
data class ChecksheetPartDefectViewDto(
    val uniq_no: String,
    val part_no: String? = null,
    val nama_part: String,
    val model: String? = null,
    val customer: String? = null,
    val komoditas: String,
    val lokasi_gambar: String? = null,
    val daftar_defect: List<DefectItemDto> = emptyList()
)

@Serializable
data class DefectItemDto(
    val id_defect: String,
    val nama_defect: String,
    val kategori: String,
    val urutan: Int = 1
)
