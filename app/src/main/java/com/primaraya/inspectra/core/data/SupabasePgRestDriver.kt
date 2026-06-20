package com.primaraya.inspectra.core.data

import com.primaraya.inspectra.BuildConfig
import com.primaraya.inspectra.core.network.InspectraHttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * Implementasi DatabaseDriver berbasis Supabase PostgREST.
 *
 * Semua response diperiksa HTTP status-nya sebelum decode agar error dari server
 * tidak membuat UI crash.
 */
class SupabasePgRestDriver : DatabaseDriver {

    private val baseUrl = BuildConfig.SUPABASE_URL
        .trim()
        .removeSurrounding("\"")
        .trimEnd('/')

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    override suspend fun <T> getList(
        table: RemoteTable,
        query: String,
        decode: (String) -> T
    ): T {
        val response = InspectraHttpClient.client.get("$baseUrl/rest/v1/${table.value}?$query")
        val body = response.bodyAsText()

        if (!response.status.isSuccess()) {
            throw DatabaseDriverException.fromSupabase(body)
        }

        return decode(body)
    }

    override suspend fun <TBody, TResponse> insertReturning(
        table: RemoteTable,
        body: TBody,
        encode: (TBody) -> String,
        decode: (String) -> TResponse
    ): TResponse {
        val response = InspectraHttpClient.client.post("$baseUrl/rest/v1/${table.value}?select=*") {
            headers.append("Prefer", "return=representation")
            setBody(encode(body))
        }

        val responseText = response.bodyAsText()

        if (!response.status.isSuccess()) {
            throw DatabaseDriverException.fromSupabase(responseText)
        }

        return decode(responseText)
    }

    override suspend fun <TBody> upsert(
        table: RemoteTable,
        body: TBody,
        encode: (TBody) -> String,
        onConflict: String?
    ) {
        val conflict = onConflict?.let { "?on_conflict=$it" }.orEmpty()

        val response = InspectraHttpClient.client.post("$baseUrl/rest/v1/${table.value}$conflict") {
            headers.append("Prefer", "resolution=merge-duplicates")
            setBody(encode(body))
        }

        val responseText = response.bodyAsText()

        if (!response.status.isSuccess()) {
            throw DatabaseDriverException.fromSupabase(responseText)
        }
    }

    override suspend fun softDelete(
        table: RemoteTable,
        idColumn: String,
        id: String
    ) {
        val response = InspectraHttpClient.client.patch("$baseUrl/rest/v1/${table.value}?$idColumn=eq.$id") {
            setBody(mapOf("aktif" to false))
        }

        val responseText = response.bodyAsText()

        if (!response.status.isSuccess()) {
            throw DatabaseDriverException.fromSupabase(responseText)
        }
    }
}

/**
 * Exception internal database driver.
 */
class DatabaseDriverException(
    message: String,
    val code: String? = null
) : RuntimeException(message) {

    companion object {
        fun fromSupabase(body: String): DatabaseDriverException {
            val code = Regex("\"code\"\\s*:\\s*\"([^\"]+)\"")
                .find(body)
                ?.groupValues
                ?.getOrNull(1)

            val message = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
                .find(body)
                ?.groupValues
                ?.getOrNull(1)
                ?: "Permintaan database belum berhasil."

            return DatabaseDriverException(message, code)
        }
    }
}
