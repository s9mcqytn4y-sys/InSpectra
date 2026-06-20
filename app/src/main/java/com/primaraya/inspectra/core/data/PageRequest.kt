package com.primaraya.inspectra.core.data

/**
 * Parameter pagination berbasis limit + cursor.
 *
 * Cursor lebih stabil daripada offset untuk data yang besar dan sering berubah.
 */
data class PageRequest(
    val limit: Int = 50,
    val cursorColumn: String,
    val cursorValue: String? = null,
    val ascending: Boolean = true,
    val searchColumn: String? = null,
    val searchKeyword: String? = null
) {
    init {
        require(limit in 10..100) { "Limit harus 10 sampai 100." }
    }

    fun toPostgrestQuery(select: String = "*"): String {
        val order = if (ascending) "asc" else "desc"
        val parts = mutableListOf(
            "select=$select",
            "order=$cursorColumn.$order",
            "limit=$limit"
        )

        if (!cursorValue.isNullOrBlank()) {
            parts += "$cursorColumn=gt.$cursorValue"
        }

        if (!searchColumn.isNullOrBlank() && !searchKeyword.isNullOrBlank()) {
            parts += "$searchColumn=ilike.*${searchKeyword.trim()}*"
        }

        return parts.joinToString("&")
    }
}
