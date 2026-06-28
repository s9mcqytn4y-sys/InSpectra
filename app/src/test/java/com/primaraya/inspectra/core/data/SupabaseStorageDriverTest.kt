package com.primaraya.inspectra.core.data

import com.primaraya.inspectra.core.network.InspectraHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertFailsWith

class SupabaseStorageDriverTest {

    private lateinit var driver: SupabaseStorageDriver

    @Before
    fun setup() {
        mockkObject(InspectraHttpClient)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun mockClientWithStatus(status: HttpStatusCode, content: String = "") {
        val mockEngine = MockEngine { request ->
            respond(
                content = content,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val mockClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(InspectraHttpClient.json)
            }
        }
        every { InspectraHttpClient.client } returns mockClient
        driver = SupabaseStorageDriver()
    }

    @Test
    fun `uploadFile throws exception when 429 TooManyRequests`() = runTest {
        mockClientWithStatus(HttpStatusCode.TooManyRequests)
        
        // Create a dummy temp file
        val tempFile = File.createTempFile("test_upload", ".jpg")
        tempFile.writeBytes(byteArrayOf(1, 2, 3))

        val exception = assertFailsWith<DatabaseDriverException> {
            driver.uploadFile("parts", "test.jpg", tempFile)
        }

        assertTrue(exception.message!!.contains("batas penggunaan gratis"))
        tempFile.delete()
    }

    @Test
    fun `uploadFile throws exception when 503 ServiceUnavailable`() = runTest {
        mockClientWithStatus(HttpStatusCode.ServiceUnavailable)
        
        val tempFile = File.createTempFile("test_upload", ".jpg")
        tempFile.writeBytes(byteArrayOf(1, 2, 3))

        val exception = assertFailsWith<DatabaseDriverException> {
            driver.uploadFile("parts", "test.jpg", tempFile)
        }

        assertTrue(exception.message!!.contains("batas penggunaan gratis"))
        tempFile.delete()
    }
    
    @Test
    fun `deleteFile throws exception when 429 TooManyRequests`() = runTest {
        mockClientWithStatus(HttpStatusCode.TooManyRequests)

        val exception = assertFailsWith<DatabaseDriverException> {
            driver.deleteFile("parts", "test.jpg")
        }

        assertTrue(exception.message!!.contains("batas penggunaan gratis"))
    }
}
