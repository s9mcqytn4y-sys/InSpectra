package com.primaraya.inspectra.fitur.checksheet.ui

import android.app.Application
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.fitur.checksheet.data.ChecksheetRepository
import com.primaraya.inspectra.fitur.checksheet.domain.TipeProses
import com.primaraya.inspectra.fitur.masterdata.data.MasterDataRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChecksheetViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var app: Application
    private lateinit var masterRepository: MasterDataRepository
    private lateinit var checksheetRepository: ChecksheetRepository
    private lateinit var viewModel: ChecksheetMviViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = mockk(relaxed = true)
        masterRepository = mockk(relaxed = true)
        checksheetRepository = mockk(relaxed = true)
        viewModel = ChecksheetMviViewModel(app, masterRepository, checksheetRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `muat should call repository and update state`() {
        // Arrange
        coEvery { masterRepository.getPartPickerItems(any()) } returns NetworkResult.Loading
        
        // Act
        viewModel.onIntent(ChecksheetContract.Intent.Muat(TipeProses.SEWING))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        assertTrue(viewModel.state.value.dataPicker is AsyncData.Loading)
    }
}
