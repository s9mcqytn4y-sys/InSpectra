package com.primaraya.inspectra.core.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primaraya.inspectra.core.data.AppBootstrapDto
import com.primaraya.inspectra.core.data.BootstrapRepository
import com.primaraya.inspectra.core.data.SupabaseBootstrapRepository
import com.primaraya.inspectra.core.network.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val CURRENT_SCHEMA_REVISION = "2026-06-21-master-relation-media-cutting-input-vnext"

class AppViewModel(
    private val repository: BootstrapRepository = SupabaseBootstrapRepository()
) : ViewModel() {

    private val _bootstrap = MutableStateFlow<NetworkResult<AppBootstrapDto>>(NetworkResult.Loading)
    val bootstrap = _bootstrap.asStateFlow()

    private val _isSchemaCompatible = MutableStateFlow(true)
    val isSchemaCompatible = _isSchemaCompatible.asStateFlow()

    init {
        checkBootstrap()
    }

    fun checkBootstrap() {
        viewModelScope.launch {
            _bootstrap.value = NetworkResult.Loading
            val result = repository.getBootstrapData()
            _bootstrap.value = result
            
            if (result is NetworkResult.Success) {
                _isSchemaCompatible.update { 
                    result.data.schema_revision == CURRENT_SCHEMA_REVISION 
                }
            }
        }
    }
}
