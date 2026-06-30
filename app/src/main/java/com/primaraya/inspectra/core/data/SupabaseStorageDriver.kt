package com.primaraya.inspectra.core.data

import com.primaraya.inspectra.core.network.InspectraHttpClient
import com.primaraya.inspectra.BuildConfig
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import java.io.File

/**
 * Driver untuk berinteraksi dengan Supabase Storage API.
 */
class SupabaseStorageDriver {

    private val baseUrl = BuildConfig.SUPABASE_URL
        .trim()
        .removeSurrounding("\"")
        .trimEnd('/')

    private val storageUrl = "$baseUrl/storage/v1"

    /**
     * Upload file ke bucket Supabase.
     * Mengembalikan URL publik file jika berhasil.
     */
    suspend fun uploadFile(
        bucket: String,
        path: String,
        file: File
    ): String {
        val url = "$storageUrl/object/$bucket/$path"
        
        val response = InspectraHttpClient.client.post(url) {
            header("x-upsert", "true")
            setBody(file.readBytes())
            contentType(ContentType.Image.JPEG)
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            handleStorageError(response.status, errorBody)
        }

        return "$baseUrl/storage/v1/object/public/$bucket/$path"
    }

    suspend fun deleteFile(bucket: String, path: String) {
        val url = "$storageUrl/object/$bucket/$path"
        val response = InspectraHttpClient.client.delete(url)
        
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            handleStorageError(response.status, errorBody)
        }
    }

    private fun handleStorageError(status: HttpStatusCode, body: String) {
        if (status == HttpStatusCode.TooManyRequests || status == HttpStatusCode.ServiceUnavailable) {
            throw DatabaseDriverException(
                message = "Gagal memproses media karena batas penggunaan gratis Supabase terlampaui (429/503).",
                code = status.value.toString()
            )
        }
        throw Exception("Gagal berinteraksi dengan Storage: $body")
    }
}
