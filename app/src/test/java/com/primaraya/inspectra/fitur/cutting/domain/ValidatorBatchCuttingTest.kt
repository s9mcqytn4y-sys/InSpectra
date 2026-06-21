package com.primaraya.inspectra.fitur.cutting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorBatchCuttingTest {

    @Test
    fun `menolak nilai negatif dan ukuran tidak valid`() {
        val pesan = ValidatorBatchCutting.validasi(
            inputDasar().copy(
                ukuranCuttingCm = "0",
                qtyLayerOk = "-1",
                qtyLayerNg = "-2",
                wastePanjangCm = "-3"
            )
        )

        assertTrue(pesan.any { it.contains("Ukuran cutting") })
        assertTrue(pesan.any { it.contains("layer OK") })
        assertTrue(pesan.any { it.contains("layer NG") })
        assertTrue(pesan.any { it.contains("Waste") })
    }

    @Test
    fun `ng wajib memiliki defect dan jumlah defect tidak boleh melebihi ng`() {
        val tanpaDefect = ValidatorBatchCutting.validasi(inputDasar().copy(qtyLayerNg = "2"))
        assertTrue(tanpaDefect.any { it.contains("minimal satu defect") })

        val melebihiNg = ValidatorBatchCutting.validasi(
            inputDasar().copy(
                qtyLayerNg = "2",
                daftarDefect = listOf(defect(jumlahLayer = 3))
            )
        )
        assertTrue(melebihiNg.any { it.contains("tidak boleh melebihi") })
    }

    @Test
    fun `menghitung panjang dan rasio waste secara konsisten`() {
        val input = inputDasar().copy(
            ukuranCuttingCm = "100",
            qtyLayerOk = "8",
            qtyLayerNg = "2",
            wastePanjangCm = "20",
            daftarDefect = listOf(defect(jumlahLayer = 2))
        )

        assertTrue(ValidatorBatchCutting.validasi(input).isEmpty())
        assertEquals(20.0, input.rasioNgLayer, 0.001)
        assertEquals(800.0, input.estimasiPanjangOkCm, 0.001)
        assertEquals(200.0, input.estimasiPanjangNgCm, 0.001)
        assertEquals(1.961, input.rasioWastePanjang, 0.001)
    }

    private fun inputDasar(): InputBatchCutting = InputBatchCutting(
        tanggalPemeriksaan = "2026-06-21",
        materialId = "material-1",
        namaMaterial = "Material Uji",
        ukuranCuttingCm = "50",
        qtyLayerOk = "1",
        qtyLayerNg = "0",
        wastePanjangCm = "0"
    )

    private fun defect(jumlahLayer: Int): InputDefectCutting = InputDefectCutting(
        idDefect = "D-001",
        namaDefect = "Defect Uji",
        jumlahLayerTerdampak = jumlahLayer
    )
}
