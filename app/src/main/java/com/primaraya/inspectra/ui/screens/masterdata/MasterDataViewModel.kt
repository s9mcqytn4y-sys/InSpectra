package com.primaraya.inspectra.ui.screens.masterdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.ui.UserMessage
import com.primaraya.inspectra.core.ui.UserMessageMapper
import com.primaraya.inspectra.masterdata.data.MasterDataRepository
import com.primaraya.inspectra.masterdata.data.SupabaseMasterDataRepository
import com.primaraya.inspectra.masterdata.domain.MasterPartDto
import com.primaraya.inspectra.masterdata.domain.MasterMaterialDto
import com.primaraya.inspectra.masterdata.domain.MasterDefectDto
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MasterDataUiState(
    val isLoading: Boolean = false,
    val parts: List<MasterPartDto> = emptyList(),
    val materials: List<MasterMaterialDto> = emptyList(),
    val defects: List<MasterDefectDto> = emptyList(),
    val userMessage: UserMessage? = null,
    val selectedTab: Int = 0
)

class MasterDataViewModel : ViewModel() {
    private val repository: MasterDataRepository = SupabaseMasterDataRepository()

    private val _uiState = MutableStateFlow(MasterDataUiState())
    val uiState: StateFlow<MasterDataUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun setTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
        loadData()
    }

    fun loadData() {
        val currentTab = _uiState.value.selectedTab
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userMessage = null) }
            
            when (currentTab) {
                0 -> loadParts()
                1 -> loadMaterials()
                2 -> loadDefects()
            }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadParts() {
        when (val result = repository.getParts()) {
            is NetworkResult.Success -> _uiState.update { it.copy(parts = result.data) }
            is NetworkResult.Error -> _uiState.update { it.copy(userMessage = UserMessageMapper.fromThrowableMessage(result.message)) }
            else -> Unit
        }
    }

    private suspend fun loadMaterials() {
        when (val result = repository.getMaterials()) {
            is NetworkResult.Success -> _uiState.update { it.copy(materials = result.data) }
            is NetworkResult.Error -> _uiState.update { it.copy(userMessage = UserMessageMapper.fromThrowableMessage(result.message)) }
            else -> Unit
        }
    }

    private suspend fun loadDefects() {
        when (val result = repository.getDefects()) {
            is NetworkResult.Success -> _uiState.update { it.copy(defects = result.data) }
            is NetworkResult.Error -> _uiState.update { it.copy(userMessage = UserMessageMapper.fromThrowableMessage(result.message)) }
            else -> Unit
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
