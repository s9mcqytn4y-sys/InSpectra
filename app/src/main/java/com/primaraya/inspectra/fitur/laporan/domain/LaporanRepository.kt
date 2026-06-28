package com.primaraya.inspectra.fitur.laporan.domain

import com.primaraya.inspectra.fitur.checksheet.domain.PartAcuan

interface LaporanRepository {
    suspend fun getPartsForProcess(tipeProses: String): List<PartAcuan>
    suspend fun submitLaporan(dto: LaporanSubmitDto)
}
