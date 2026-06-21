package com.primaraya.inspectra.fitur.splash

import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.network.StatusKoneksi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SplashStatusTest {

    @Test
    fun `status sukses menjadi online`() {
        val state = SplashStatus.pilih(NetworkResult.Success(Unit))

        assertEquals(StatusKoneksi.ONLINE, state.status)
    }

    @Test
    fun `error dan timeout menjadi offline tanpa pesan teknis`() {
        val error = NetworkResult.Error("DNS", "Unable to resolve host: rahasia")
        val saatError = SplashStatus.pilih(error)
        val saatTimeout = SplashStatus.pilih(null)

        assertEquals(StatusKoneksi.OFFLINE, saatError.status)
        assertEquals(StatusKoneksi.OFFLINE, saatTimeout.status)
        assertFalse(saatError.pesan.contains("resolve", ignoreCase = true))
        assertFalse(saatError.pesan.contains("rahasia", ignoreCase = true))
    }
}
