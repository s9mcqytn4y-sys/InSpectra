package com.primaraya.inspectra.core.common

/**
 * State asinkron reusable untuk UI.
 */
sealed interface AsyncData<out T> {
    data object Idle : AsyncData<Nothing>
    data object Loading : AsyncData<Nothing>
    data class Success<T>(val data: T) : AsyncData<T>
    data class Empty(val title: String, val message: String) : AsyncData<Nothing>
    data class Error(val title: String, val message: String) : AsyncData<Nothing>
}
