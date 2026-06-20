package com.primaraya.inspectra.core.database

import com.primaraya.inspectra.BuildConfig
import com.primaraya.inspectra.core.network.InspectraHttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * Client REST Supabase yang aman terhadap error PostgREST.
 *
 * Seluruh response diperiksa statusnya sebelum decode agar error JSON
 * dari server tidak memicu JsonConvertException pada UI.
 */
class SupabaseRestClient {

    val baseUrl: String = BuildConfig.SUPABASE_URL
        .trim()
        .removeSurrounding("\"")
        .trimEnd('/')

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    /**
     * Mengambil list dari tabel/view Supabase.
     */
    suspend inline fun <reified T> getList(
        table: String,
        query: String = "select=*"
    ): T {
        val response = InspectraHttpClient.client.get("$baseUrl/rest/v1/$table?$query")

        if (!response.status.isSuccess()) {
            throw SupabaseRestException.fromBody(response.bodyAsText())
        }

        return json.decodeFromString(response.bodyAsText())
    }

    /**
     * Insert dengan return representation.
     */
    suspend inline fun <reified TBody, reified TResponse> insertReturning(
        table: String,
        body: TBody
    ): TResponse {
        val response = InspectraHttpClient.client.post("$baseUrl/rest/v1/$table?select=*") {
            headers.append("Prefer", "return=representation")
            setBody(body)
        }

        if (!response.status.isSuccess()) {
            throw SupabaseRestException.fromBody(response.bodyAsText())
        }

        return json.decodeFromString(response.bodyAsText())
    }

    /**
     * Upsert data.
     */
    suspend inline fun <reified TBody> upsert(
        table: String,
        body: TBody
    ) {
        val response = InspectraHttpClient.client.post("$baseUrl/rest/v1/$table") {
            headers.append("Prefer", "resolution=merge-duplicates")
            setBody(body)
        }

        if (!response.status.isSuccess()) {
            throw SupabaseRestException.fromBody(response.bodyAsText())
        }
    }

    /**
     * Soft delete dengan update aktif=false.
     */
    suspend fun softDelete(
        table: String,
        idColumn: String,
        id: String
    ) {
        val response = InspectraHttpClient.client.patch("$baseUrl/rest/v1/$table?$idColumn=eq.$id") {
            setBody(mapOf("aktif" to false))
        }

        if (!response.status.isSuccess()) {
            throw SupabaseRestException.fromBody(response.bodyAsText())
        }
    }
}

/**
 * Exception internal untuk error PostgREST.
 */
class SupabaseRestException(
    message: String,
    val code: String? = null
) : RuntimeException(message) {

    companion object {
        fun fromBody(body: String): SupabaseRestException {
            val code = Regex("\"code\"\\s*:\\s*\"([^\"]+)\"")
                .find(body)
                ?.groupValues
                ?.getOrNull(1)

            val message = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
                .find(body)
                ?.groupValues
                ?.getOrNull(1)
                ?: "Permintaan ke server belum berhasil."

            return SupabaseRestException(message, code)
        }
    }
}
