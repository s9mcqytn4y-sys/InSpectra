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
    val lokasiGambarRemote: String? = null,
    val lokasiGambarLokal: String? = null,
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

@Serializable
data class MaterialFormState(
    val id: String? = null,
    val supplierId: String? = null,
    val supplierNama: String = "",
    val namaMaterial: String = "",
    val spec: String = "",
    val satuan: String = "PCS",
    val lebarCm: String = "",
    val panjangRollCm: String = "",
    val tebalMm: String = "",
    val beratGsm: String = "",
    val gramasiGsm: String = "",
    val warna: String = "",
    val catatanSpesifikasi: String = "",
    val submitted: Boolean = false
) {
    val namaMaterialError: String?
        get() = if (submitted && namaMaterial.isBlank()) "Nama material wajib diisi." else null

    val valid: Boolean get() = namaMaterial.isNotBlank()
}

@Serializable
data class SupplierFormState(
    val id: String? = null,
    val kodeSupplier: String = "",
    val namaSupplier: String = "",
    val kategori: String = "",
    val submitted: Boolean = false
) {
    val namaSupplierError: String?
        get() = if (submitted && namaSupplier.isBlank()) "Nama supplier wajib diisi." else null

    val valid: Boolean get() = namaSupplier.isNotBlank()
}

@Serializable
data class DefectFormState(
    val idDefect: String = "",
    val namaDefect: String = "",
    val kategori: String = "PROSES",
    val submitted: Boolean = false
) {
    val idDefectError: String?
        get() = when {
            !submitted -> null
            idDefect.isBlank() -> "ID Defect wajib diisi."
            else -> null
        }

    val namaDefectError: String?
        get() = if (submitted && namaDefect.isBlank()) "Nama defect wajib diisi." else null

    val valid: Boolean get() = idDefect.isNotBlank() && namaDefect.isNotBlank()
}
