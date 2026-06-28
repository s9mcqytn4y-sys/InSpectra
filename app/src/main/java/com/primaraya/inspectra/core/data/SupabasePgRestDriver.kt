package com.primaraya.inspectra.core.data

import com.primaraya.inspectra.BuildConfig
import com.primaraya.inspectra.core.network.InspectraHttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

/**
 * Implementasi DatabaseDriver berbasis Supabase PostgREST.
 *
 * Semua response diperiksa HTTP status-nya sebelum decode agar error dari server
 * tidak membuat UI crash. Termasuk quota limit 429 dan 503 dari Free Plan.
 */
class SupabasePgRestDriver : DatabaseDriver {

    private val baseUrl = BuildConfig.SUPABASE_URL
        .trim()
        .removeSurrounding("\"")
        .trimEnd('/')

    private val json = InspectraHttpClient.json

    private fun checkQuotaLimits(status: HttpStatusCode) {
        if (status == HttpStatusCode.TooManyRequests || status == HttpStatusCode.ServiceUnavailable) {
            throw DatabaseDriverException(
                "Sistem sedang sibuk karena batas penggunaan gratis. Mohon coba beberapa saat lagi.",
                status.value.toString()
            )
        }
    }

    override suspend fun <T> getList(
        table: RemoteTable,
        query: String,
        decode: (String) -> T
    ): T {
        val response = InspectraHttpClient.client.get("$baseUrl/rest/v1/${table.value}?$query")
        val body = response.bodyAsText()

        if (!response.status.isSuccess()) {
            checkQuotaLimits(response.status)
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
            checkQuotaLimits(response.status)
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
            checkQuotaLimits(response.status)
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
            checkQuotaLimits(response.status)
            throw DatabaseDriverException.fromSupabase(responseText)
        }
    }

    override suspend fun <TBody, TResponse> rpc(
        functionName: String,
        body: TBody,
        encode: (TBody) -> String,
        decode: (String) -> TResponse
    ): TResponse {
        val response = InspectraHttpClient.client.post("$baseUrl/rest/v1/rpc/$functionName") {
            setBody(encode(body))
        }

        val responseText = response.bodyAsText()

        if (!response.status.isSuccess()) {
            checkQuotaLimits(response.status)
            throw DatabaseDriverException.fromSupabase(responseText)
        }

        return decode(responseText)
    }
}

/**
 * Exception internal database driver.
 */
class DatabaseDriverException(
    message: String,
    val code: String? = null
) : RuntimeException(message) {

    @Serializable
    private data class SupabaseErrorResponse(
        val message: String? = null,
        val code: String? = null,
        val details: String? = null,
        val hint: String? = null
    )

    companion object {
        fun fromSupabase(body: String): DatabaseDriverException {
            if (body.isBlank()) {
                return DatabaseDriverException("Permintaan database gagal tanpa pesan error dari server.")
            }

            return try {
                val error = InspectraHttpClient.json.decodeFromString<SupabaseErrorResponse>(body)
                DatabaseDriverException(
                    message = error.message ?: "Permintaan database belum berhasil.",
                    code = error.code
                )
            } catch (e: SerializationException) {
                // Fallback if the body is not standard JSON (e.g., 502 Bad Gateway HTML)
                DatabaseDriverException("Terjadi kesalahan pada server (Respon tidak dikenali).")
            } catch (e: Exception) {
                DatabaseDriverException("Terjadi kesalahan sistem: ${e.message}")
            }
        }
    }
}
