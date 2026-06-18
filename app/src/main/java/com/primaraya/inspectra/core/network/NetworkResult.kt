package com.primaraya.inspectra.core.network

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>

    data class Error(
        val title: String,
        val message: String,
        val throwable: Throwable? = null,
        val canRetry: Boolean = true
    ) : NetworkResult<Nothing>

    data object Loading : NetworkResult<Nothing>
}

inline fun <T> runNetworkCatching(block: () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(block())
    } catch (security: SecurityException) {
        NetworkResult.Error(
            title = "Izin internet belum aktif",
            message = "Aplikasi belum mendapat akses internet. Bersihkan build, uninstall APK lama, lalu install ulang.",
            throwable = security,
            canRetry = false
        )
    } catch (e: java.net.UnknownHostException) {
        NetworkResult.Error(
            title = "Server tidak ditemukan",
            message = "Periksa koneksi internet, URL Supabase, atau DNS emulator.",
            throwable = e
        )
    } catch (e: java.net.SocketTimeoutException) {
        NetworkResult.Error(
            title = "Koneksi terlalu lama",
            message = "Server tidak merespons dalam batas waktu. Coba lagi.",
            throwable = e
        )
    } catch (e: Throwable) {
        NetworkResult.Error(
            title = "Terjadi kesalahan",
            message = e.message ?: "Kesalahan tidak diketahui.",
            throwable = e
        )
    }
}
