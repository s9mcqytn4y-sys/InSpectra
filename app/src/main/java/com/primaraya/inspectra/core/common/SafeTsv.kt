package com.primaraya.inspectra.core.common

object SafeTsv {

    fun parseRows(
        raw: String,
        expectedColumns: Int,
        skipHeader: Boolean = true,
        sourceName: String = "TSV"
    ): List<List<String>> {
        return raw
            .trim()
            .lineSequence()
            .drop(if (skipHeader) 1 else 0)
            .mapIndexedNotNull { index, line ->
                val cleanLine = line.trimEnd('\r', '\n')

                if (cleanLine.isBlank()) return@mapIndexedNotNull null

                val columns = cleanLine
                    .split('\t')
                    .map { it.trim() }

                when {
                    columns.size == expectedColumns -> columns

                    columns.size < expectedColumns -> {
                        columns + List(expectedColumns - columns.size) { "" }
                    }

                    else -> {
                        columns.take(expectedColumns - 1) +
                            columns.drop(expectedColumns - 1).joinToString(" ")
                    }
                }
            }
            .toList()
    }

    fun get(row: List<String>, index: Int): String {
        return row.getOrNull(index).orEmpty().trim()
    }

    fun String?.blankToNull(): String? {
        return this?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun String.toNullableDouble(): Double? {
        return trim()
            .replace(",", ".")
            .takeIf { it.isNotBlank() }
            ?.toDoubleOrNull()
    }
}
