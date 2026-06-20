package com.primaraya.inspectra.fitur.masterdata.ui

import kotlinx.serialization.Serializable

@Serializable
data class PartFormState(
    val id: String? = null,
    val uniqNo: String = "",
    val partNo: String = "",
    val namaPart: String = "",
    val model: String = "",
    val customer: String = "",
    val komoditas: String = "PRESS",
    val totalItemPerKanban: String = "",
    val sampleItemPerKanban: String = "",
    val sampleCycleNote: String = "",
    val submitted: Boolean = false
) {
    val uniqNoError: String?
        get() = when {
            !submitted -> null
            uniqNo.isBlank() -> "UNIQ wajib diisi."
            uniqNo.length > 30 -> "UNIQ maksimal 30 karakter."
            else -> null
        }

    val namaPartError: String?
        get() = when {
            !submitted -> null
            namaPart.isBlank() -> "Nama part wajib diisi."
            namaPart.length < 3 -> "Nama part terlalu pendek."
            else -> null
        }

    val totalKanbanError: String?
        get() = when {
            !submitted || totalItemPerKanban.isBlank() -> null
            totalItemPerKanban.toIntOrNull() == null -> "Isi angka yang valid."
            totalItemPerKanban.toInt() < 0 -> "Tidak boleh minus."
            else -> null
        }

    val valid: Boolean
        get() = uniqNoError == null &&
            namaPartError == null &&
            totalKanbanError == null &&
            uniqNo.isNotBlank() &&
            namaPart.isNotBlank()
}
