package com.primaraya.inspectra.core.data

import java.net.URLEncoder

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
            "select=${select.url()}",
            "order=${"$cursorColumn.$order".url()}",
            "limit=$limit"
        )

        if (!cursorValue.isNullOrBlank()) {
            parts += "$cursorColumn=gt.${cursorValue.url()}"
        }

        if (!searchColumn.isNullOrBlank() && !searchKeyword.isNullOrBlank()) {
            parts += "$searchColumn=ilike.${"*${searchKeyword.trim()}*".url()}"
        }

        return parts.joinToString("&")
    }

    private fun String.url(): String =
        URLEncoder.encode(this, Charsets.UTF_8.name())
}
