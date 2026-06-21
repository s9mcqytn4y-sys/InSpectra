package com.primaraya.inspectra.core.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory kecil untuk ViewModel yang membutuhkan Application tanpa menambah
 * dependency injection framework sebelum proyek benar-benar membutuhkannya.
 */
fun <VM : ViewModel> pabrikViewModelAplikasi(
    application: Application,
    pembuat: (Application) -> VM
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return pembuat(application) as T
        }
    }
}
