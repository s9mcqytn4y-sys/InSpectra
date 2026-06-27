package com.primaraya.inspectra.core.data

import com.primaraya.inspectra.BuildConfig
import com.primaraya.inspectra.core.network.InspectraHttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import java.io.File

/**
 * Driver untuk interaksi dengan Supabase Storage.
 */
class SupabaseStorageDriver {

    private val baseUrl = BuildConfig.SUPABASE_URL
        .trim()
        .removeSurrounding("\"")
        .trimEnd('/')

    private val client = InspectraHttpClient.client

    /**
     * Upload file ke bucket tertentu.
     * Mengembalikan URL publik jika berhasil.
     */
    suspend fun uploadFile(
        bucket: String,
        path: String,
        file: File,
        contentType: String = "image/jpeg"
    ): String {
        val response = client.post("$baseUrl/storage/v1/object/$bucket/$path") {
            header("x-upsert", "true")
            setBody(file.readBytes())
            contentType(ContentType.parse(contentType))
        }

        if (!response.status.isSuccess()) {
            val error = response.bodyAsText()
            throw Exception("Gagal upload file: $error")
        }

        // Supabase Public URL Pattern
        return "$baseUrl/storage/v1/object/public/$bucket/$path"
    }

    suspend fun deleteFile(bucket: String, path: String) {
        val response = client.delete("$baseUrl/storage/v1/object/$bucket/$path")
        
        if (!response.status.isSuccess() && response.status != HttpStatusCode.NotFound) {
            val error = response.bodyAsText()
            throw Exception("Gagal hapus file: $error")
        }
    }
}
