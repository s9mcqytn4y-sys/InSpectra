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
    } catch (e: Throwable) {
        NetworkResult.Error(
            title = "Operasi gagal",
            message = e.message ?: "Unknown error",
            throwable = e,
            canRetry = true
        )
    }
}
