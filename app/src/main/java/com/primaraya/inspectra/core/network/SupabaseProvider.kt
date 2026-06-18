package com.primaraya.inspectra.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType

object SupabaseProvider {

    private val config: SupabaseConfig by lazy {
        SupabaseConfig.fromBuildConfig()
    }

    @OptIn(SupabaseInternal::class)
    val client: SupabaseClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        require(config.isValid) {
            "Konfigurasi Supabase tidak valid. Cek SUPABASE_URL dan SUPABASE_KEY di local.properties."
        }

        createSupabaseClient(
            supabaseUrl = config.url,
            supabaseKey = config.key
        ) {
            install(Postgrest)
            install(Auth)

            httpConfig {
                expectSuccess = false

                install(HttpTimeout) {
                    requestTimeoutMillis = 15_000
                    connectTimeoutMillis = 10_000
                    socketTimeoutMillis = 15_000
                }

                install(HttpRequestRetry) {
                    retryOnServerErrors(maxRetries = 2)
                    exponentialDelay()
                }

                defaultRequest {
                    contentType(ContentType.Application.Json)
                }
            }
        }
    }

    fun isConfigReady(): Boolean = config.isValid
}
