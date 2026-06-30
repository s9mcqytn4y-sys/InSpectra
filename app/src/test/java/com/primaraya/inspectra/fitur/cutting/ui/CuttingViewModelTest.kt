package com.primaraya.inspectra.fitur.cutting.ui

import android.app.Application
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.fitur.cutting.data.CuttingRepository
import com.primaraya.inspectra.fitur.cutting.data.BootstrapCutting
import com.primaraya.inspectra.fitur.cutting.domain.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CuttingViewModelTest {

    private lateinit var viewModel: CuttingViewModel
    private val repository: CuttingRepository = mockk(relaxed = true)
    private val application: Application = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CuttingViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Muat should load bootstrap data successfully`() = runTest {
        val mockBootstrap = BootstrapCutting(
            material_option = listOf(OpsiMaterialCutting("M1", "Mat 1", "Spec", "ROLL")),
            part_size_option = emptyList(),
            defect_option = emptyList(),
            slot_waktu = emptyList()
        )
        coEvery { repository.bacaBootstrap() } returns NetworkResult.Success(mockBootstrap)
        coEvery { repository.bacaRingkasanHarian(any()) } returns NetworkResult.Success(emptyList())

        viewModel.onIntent(CuttingContract.Intent.Muat)

        val state = viewModel.state.value
        assertTrue(state.material is AsyncData.Success)
        assertEquals("Mat 1", (state.material as AsyncData.Success).data.first().nama_material)
    }

    @Test
    fun `PilihMaterial should update input with defaults`() = runTest {
        val mat = OpsiMaterialCutting(
            material_id = "M1",
            nama_material = "Mat 1",
            spec_ringkas = "Spec A",
            satuan = "ROLL",
            daftar_ukuran_cutting = listOf(UkuranCuttingAcuan("U1", size_cutting_cm = 10.5, is_default = true))
        )

        viewModel.onIntent(CuttingContract.Intent.PilihMaterial(mat))

        val input = viewModel.state.value.input
        assertEquals("M1", input.materialId)
        assertEquals("Spec A", input.spesifikasiMaterial)
        assertEquals("10.5", input.ukuranCuttingCm)
    }

    @Test
    fun `Simpan with valid input should call repository`() = runTest {
        val validMat = OpsiMaterialCutting("M1", "Mat 1", "Spec", "ROLL")
        viewModel.onIntent(CuttingContract.Intent.PilihMaterial(validMat))
        viewModel.onIntent(CuttingContract.Intent.UbahInput(viewModel.state.value.input.copy(
            ukuranCuttingCm = "10",
            qtyLayerOk = "100",
            qtyLayerNg = "0",
            wastePanjangCm = "5"
        )))

        coEvery { repository.simpanBatch(any()) } returns NetworkResult.Success("ID-123")

        viewModel.onIntent(CuttingContract.Intent.Simpan)

        coVerify { repository.simpanBatch(any()) }
        assertTrue(viewModel.state.value.berhasil)
    }
}
