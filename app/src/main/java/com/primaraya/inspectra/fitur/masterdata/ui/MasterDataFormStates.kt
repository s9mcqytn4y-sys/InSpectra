package com.primaraya.inspectra.fitur.masterdata.ui

import kotlinx.serialization.Serializable

@Serializable
data class MaterialFormState(
    val id: String? = null,
    val supplier: String = "",
    val namaMaterial: String = "",
    val spec: String = "",
    val satuan: String = "PCS",
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
