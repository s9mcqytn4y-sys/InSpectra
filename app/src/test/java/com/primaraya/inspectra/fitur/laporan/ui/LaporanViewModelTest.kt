package com.primaraya.inspectra.fitur.laporan.ui

import android.app.Application
import com.primaraya.inspectra.core.common.CoroutineDispatchersProvider
import com.primaraya.inspectra.fitur.laporan.domain.LaporanRepository
import com.primaraya.inspectra.fitur.checksheet.domain.PartAcuan
import com.primaraya.inspectra.fitur.checksheet.domain.TipeProses
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class LaporanViewModelTest {

    private lateinit var viewModel: LaporanViewModel
    private val repository: LaporanRepository = mockk()
    private val application: Application = mockk()

    // Test dispatcher
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private val testDispatchersProvider = object : CoroutineDispatchersProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LaporanViewModel(application, repository, testDispatchersProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `muat should update state with master parts`() = runTest(testDispatcher) {
        val states = mutableListOf<LaporanContract.State>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.toList(states)
        }

        val tipeProses = "PRESS"
        val mockParts = listOf(
            PartAcuan(1, "P1", "123", "Part1", "Model1", null, TipeProses.PRESS),
            PartAcuan(2, "P2", "124", "Part2", "Model1", null, TipeProses.PRESS)
        )
        coEvery { repository.getPartsForProcess(tipeProses) } returns mockParts

        viewModel.onIntent(LaporanContract.Intent.Muat(tipeProses))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(tipeProses, state.tipeProses)
        assertEquals(2, state.masterParts.size)
        assertEquals("123", state.masterParts[0].uniqNo)
        
        job.cancel()
    }

    @Test
    fun `togglePilihPart should add and remove parts from selection`() = runTest(testDispatcher) {
        val states = mutableListOf<LaporanContract.State>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.toList(states)
        }

        // Add part
        viewModel.onIntent(LaporanContract.Intent.TogglePilihPart("123"))
        advanceUntilIdle()
        var state = viewModel.state.value
        assertTrue(state.selectedPartIds.contains("123"))

        // Add another part
        viewModel.onIntent(LaporanContract.Intent.TogglePilihPart("124"))
        advanceUntilIdle()
        state = viewModel.state.value
        assertTrue(state.selectedPartIds.contains("124"))
        assertEquals(2, state.selectedPartIds.size)

        // Remove first part
        viewModel.onIntent(LaporanContract.Intent.TogglePilihPart("123"))
        advanceUntilIdle()
        state = viewModel.state.value
        assertFalse(state.selectedPartIds.contains("123"))
        assertEquals(1, state.selectedPartIds.size)
        
        job.cancel()
    }

    @Test
    fun `lanjutKeForm with empty selection should emit ShowSnackbar`() = runTest(testDispatcher) {
        val effects = mutableListOf<LaporanContract.Effect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effect.toList(effects)
        }

        viewModel.onIntent(LaporanContract.Intent.LanjutKeForm)
        advanceUntilIdle()

        assertTrue(effects.isNotEmpty())
        val effect = effects.first()
        assertTrue(effect is LaporanContract.Effect.ShowSnackbar)
        assertEquals("Pilih minimal satu part", (effect as LaporanContract.Effect.ShowSnackbar).message)
        
        job.cancel()
    }

    @Test
    fun `submitLaporan should call repository when state is valid`() = runTest(testDispatcher) {
        // Setup initial state to be valid
        val tipeProses = "PRESS"
        val mockParts = listOf(PartAcuan(1, "P1", "123", "Part1", "Model1", null, TipeProses.PRESS))
        coEvery { repository.getPartsForProcess(tipeProses) } returns mockParts
        coEvery { repository.submitLaporan(any()) } returns Unit

        val effects = mutableListOf<LaporanContract.Effect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effect.toList(effects)
        }

        val states = mutableListOf<LaporanContract.State>()
        val stateJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.toList(states)
        }

        viewModel.onIntent(LaporanContract.Intent.Muat(tipeProses))
        advanceUntilIdle() // Wait for masterParts to load
        
        viewModel.onIntent(LaporanContract.Intent.UpdateMpDirect("5"))
        viewModel.onIntent(LaporanContract.Intent.UpdateMpIndirect("2"))
        viewModel.onIntent(LaporanContract.Intent.UpdateJknHour("8"))
        viewModel.onIntent(LaporanContract.Intent.TogglePilihPart("123"))
        viewModel.onIntent(LaporanContract.Intent.LanjutKeForm)
        
        // Update detail values
        viewModel.onIntent(LaporanContract.Intent.UpdateDetailPlanning(0, "100"))
        viewModel.onIntent(LaporanContract.Intent.UpdateDetailActual(0, "95"))
        
        advanceUntilIdle()

        // Submit
        viewModel.onIntent(LaporanContract.Intent.Submit)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.submitLaporan(any()) }
        
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        
        // Effects should contain Success and NavigateBack
        val snackbarEffect = effects.find { it is LaporanContract.Effect.ShowSnackbar && it.message.contains("berhasil") }
        assertNotNull(snackbarEffect)
        
        val navEffect = effects.find { it is LaporanContract.Effect.NavigateBack }
        assertNotNull(navEffect)

        job.cancel()
        stateJob.cancel()
    }
}
