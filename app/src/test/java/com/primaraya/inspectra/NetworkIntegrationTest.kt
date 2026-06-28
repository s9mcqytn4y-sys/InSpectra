package com.primaraya.inspectra

import com.primaraya.inspectra.core.data.DatabaseDriverException
import com.primaraya.inspectra.core.data.SupabasePgRestDriver
import com.primaraya.inspectra.core.network.InspectraHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlinx.serialization.json.Json

/**
 * Integration Test untuk memverifikasi network layer
 */
class NetworkIntegrationTest {

    @Test
    fun `test DatabaseDriverException fromSupabase with valid json`() {
        val errorJson = """
            {
                "message": "duplicate key value violates unique constraint",
                "code": "23505",
                "details": "Key (id)=(1) already exists."
            }
        """.trimIndent()
        
        val exception = DatabaseDriverException.fromSupabase(errorJson)
        assertEquals("23505", exception.code)
        assertEquals("duplicate key value violates unique constraint", exception.message)
    }

    @Test
    fun `test DatabaseDriverException fromSupabase with empty body`() {
        val exception = DatabaseDriverException.fromSupabase("")
        assertNotNull(exception.message)
        assertEquals(null, exception.code)
    }
    
    @Test
    fun `test DatabaseDriverException fromSupabase with invalid json HTML`() {
        val errorHtml = "<html>502 Bad Gateway</html>"
        val exception = DatabaseDriverException.fromSupabase(errorHtml)
        assertEquals("Terjadi kesalahan pada server (Respon tidak dikenali).", exception.message)
    }
}
