package com.primaraya.inspectra.fitur.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.network.StatusKoneksi
import com.primaraya.inspectra.fitur.masterdata.data.MasterDataRepository
import com.primaraya.inspectra.fitur.masterdata.data.SupabaseMasterDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class SplashState(
    val status: StatusKoneksi = StatusKoneksi.MEMERIKSA,
    val pesan: String = "Menyiapkan aplikasi..."
)

internal object SplashStatus {
    fun pilih(hasil: NetworkResult<Unit>?): SplashState = when (hasil) {
        is NetworkResult.Success -> SplashState(StatusKoneksi.ONLINE, "Sistem siap. Sinkronisasi aktif.")
        else -> SplashState(StatusKoneksi.OFFLINE, "Koneksi belum tersedia. Draft tetap dapat disiapkan.")
    }
}

class SplashViewModel(
    application: Application,
    private val repository: MasterDataRepository = SupabaseMasterDataRepository()
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    fun periksaKoneksi() {
        viewModelScope.launch {
            _state.value = SplashState(StatusKoneksi.MEMERIKSA, "Memeriksa koneksi server...")
            val hasil = withTimeoutOrNull(7_000) { repository.healthCheck() }
            _state.value = statusDariHasil(hasil)
        }
    }

    internal fun statusDariHasil(hasil: NetworkResult<Unit>?): SplashState = SplashStatus.pilih(hasil)
}
