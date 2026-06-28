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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

class SupabasePgRestDriverTest {

    private lateinit var driver: SupabasePgRestDriver

    @Before
    fun setup() {
        mockkObject(InspectraHttpClient)
        driver = SupabasePgRestDriver()
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
    }

    @Test
    fun `getList throws exception when 429 TooManyRequests`() = runTest {
        mockClientWithStatus(HttpStatusCode.TooManyRequests)

        val exception = assertFailsWith<DatabaseDriverException> {
            driver.getList(RemoteTable.Part, "") { it }
        }

        assertTrue(exception.message!!.contains("batas penggunaan gratis"))
    }

    @Test
    fun `getList throws exception when 503 ServiceUnavailable`() = runTest {
        mockClientWithStatus(HttpStatusCode.ServiceUnavailable)

        val exception = assertFailsWith<DatabaseDriverException> {
            driver.getList(RemoteTable.Material, "") { it }
        }

        assertTrue(exception.message!!.contains("batas penggunaan gratis"))
    }
    
    @Test
    fun `getList succeeds when 200 OK`() = runTest {
        mockClientWithStatus(HttpStatusCode.OK, """[{"id": "1"}]""")

        val result = driver.getList(RemoteTable.Defect, "") { it }
        assertEquals("""[{"id": "1"}]""", result)
    }

    @Test
    fun `rpc throws exception when 429 TooManyRequests`() = runTest {
        mockClientWithStatus(HttpStatusCode.TooManyRequests)

        val exception = assertFailsWith<DatabaseDriverException> {
            driver.rpc("rpc_submit_checksheet", mapOf("test" to "123"), { "{}" }) { it }
        }

        assertTrue(exception.message!!.contains("batas penggunaan gratis"))
    }
}
