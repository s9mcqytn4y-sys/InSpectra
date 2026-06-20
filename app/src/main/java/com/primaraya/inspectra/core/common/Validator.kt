package com.primaraya.inspectra.core.common

/**
 * Utilitas validasi input terpusat.
 */
object Validator {

    fun validateRequired(value: String?, fieldName: String): String? {
        return if (value.isNullOrBlank()) "$fieldName wajib diisi." else null
    }

    fun validateMinLength(value: String?, min: Int, fieldName: String): String? {
        return if ((value?.length ?: 0) < min) "$fieldName minimal $min karakter." else null
    }

    fun validateMaxLength(value: String?, max: Int, fieldName: String): String? {
        return if ((value?.length ?: 0) > max) "$fieldName maksimal $max karakter." else null
    }

    fun validateNumeric(value: String?, fieldName: String): String? {
        if (value.isNullOrBlank()) return null
        return if (value.toDoubleOrNull() == null) "$fieldName harus berupa angka." else null
    }
}
