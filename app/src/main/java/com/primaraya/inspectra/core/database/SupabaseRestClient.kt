package com.primaraya.inspectra.core.database

import com.primaraya.inspectra.BuildConfig
import com.primaraya.inspectra.core.network.InspectraHttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess

/**
 * Client ringan untuk Supabase PostgREST.
 *
 * Semua akses tabel dan view Supabase lewat client ini agar URL, header,
 * error mapping, dan parsing response konsisten.
 */
class SupabaseRestClient {

    val baseUrl: String = BuildConfig.SUPABASE_URL
        .trim()
        .removeSurrounding("\"")
        .trimEnd('/')

    /**
     * Mengambil daftar data dari tabel atau view Supabase.
     *
     * @param table nama tabel atau view di schema public.
     * @param query query string PostgREST, contoh `select=*&aktif=eq.true`.
     */
    suspend inline fun <reified T> getList(
        table: String,
        query: String = "select=*"
    ): T {
        return InspectraHttpClient.client
            .get("$baseUrl/rest/v1/$table?$query")
            .body()
    }

    /**
     * Insert data dan mengembalikan representasi baris yang tersimpan.
     *
     * @param table nama tabel public.
     * @param body payload DTO.
     */
    suspend inline fun <reified TBody, reified TResponse> insertReturning(
        table: String,
        body: TBody
    ): TResponse {
        val response = InspectraHttpClient.client.post("$baseUrl/rest/v1/$table") {
            url {
                parameters.append("select", "*")
            }
            headers.append("Prefer", "return=representation")
            setBody(body)
        }

        if (!response.status.isSuccess()) {
            error("Permintaan penyimpanan belum berhasil.")
        }

        return response.body()
    }

    /**
     * Upsert data master.
     *
     * @param table nama tabel public.
     * @param body payload DTO.
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
            error("Data belum berhasil disimpan.")
        }
    }

    /**
     * Soft delete data dengan mengubah kolom aktif menjadi false.
     *
     * @param table nama tabel public.
     * @param idColumn nama kolom id.
     * @param id nilai id.
     */
    suspend fun softDelete(
        table: String,
        idColumn: String,
        id: String
    ) {
        val response = InspectraHttpClient.client.patch("$baseUrl/rest/v1/$table") {
            url {
                parameters.append(idColumn, "eq.$id")
            }
            setBody(mapOf("aktif" to false))
        }

        if (!response.status.isSuccess()) {
            error("Data belum berhasil dihapus.")
        }
    }
}
