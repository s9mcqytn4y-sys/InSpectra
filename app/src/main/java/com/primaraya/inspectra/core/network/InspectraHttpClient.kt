package com.primaraya.inspectra.core.network

import com.primaraya.inspectra.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.header
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Penyedia HTTP client utama aplikasi.
 *
 * Client ini memakai Ktor dengan OkHttp Engine agar koneksi REST ke Supabase
 * memakai connection pooling, timeout yang konsisten, dan retry terbatas.
 */
object InspectraHttpClient {

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    /**
     * Instance tunggal HTTP client untuk seluruh aplikasi.
     *
     * Jangan membuat client baru di Composable, ViewModel, atau Repository.
     */
    val client: HttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        HttpClient(OkHttp) {
            engine {
                preconfigured = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .connectionPool(
                        ConnectionPool(
                            maxIdleConnections = 5,
                            keepAliveDuration = 5,
                            timeUnit = TimeUnit.MINUTES
                        )
                    )
                    .retryOnConnectionFailure(true)
                    .build()
            }

            install(ContentNegotiation) {
                json(json)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }

            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 1)
                retryOnException(maxRetries = 1, retryOnTimeout = true)
                exponentialDelay()
            }

            install(DefaultRequest) {
                val key = BuildConfig.SUPABASE_KEY
                    .trim()
                    .removeSurrounding("\"")

                header("apikey", key)
                header(HttpHeaders.Authorization, "Bearer $key")
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }

            if (BuildConfig.DEBUG) {
                install(Logging) {
                    logger = SanitizedKtorLogger
                    level = LogLevel.INFO
                    sanitizeHeader { header ->
                        header.equals(HttpHeaders.Authorization, ignoreCase = true) ||
                            header.equals("apikey", ignoreCase = true)
                    }
                }
            }
        }
    }
}

/**
 * Logger Ktor yang menghapus token dan header sensitif sebelum masuk Logcat.
 */
object SanitizedKtorLogger : io.ktor.client.plugins.logging.Logger {

    override fun log(message: String) {
        val sanitized = message
            .replace(Regex("Bearer\\s+[A-Za-z0-9._\\-]+"), "Bearer ***")
            .replace(Regex("apikey=[A-Za-z0-9._\\-]+"), "apikey=***")
            .replace(Regex("apikey:.*", RegexOption.IGNORE_CASE), "apikey: ***")
            .replace(Regex("Authorization:.*"), "Authorization: ***")

        android.util.Log.d("InspectraNetwork", sanitized)
    }
}
