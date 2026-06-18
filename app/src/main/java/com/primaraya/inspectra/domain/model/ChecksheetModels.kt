package com.primaraya.inspectra.domain.model

import kotlinx.serialization.Serializable

/**
 * Representasi tipe lini proses komoditas harian.
 */
enum class ProcessType { PRESS, SEWING }

/**
 * Input kuantitas untuk tiap jenis defect yang ditemukan di lantai produksi.
 */
@Serializable
data class DefectInput(
    val defectId: String,
    val defectName: String,
    var ngQty: Int = 0
)

/**
 * Representasi mutlak komponen master data part di checksheet.
 */
data class PartChecksheetSummary(
    val uniqNo: String,
    val partNumber: String,
    val partName: String,
    val commodity: ProcessType,
    val imageUrl: String?,
    var totalSampling: Int = 0,
    val defects: List<DefectInput> = emptyList(),
    val isExpanded: Boolean = false
) {
    val totalNg: Int get() = defects.sumOf { it.ngQty }
    val totalOk: Int get() = if (totalSampling >= totalNg) totalSampling - totalNg else 0
    val ngRatio: Double get() = if (totalSampling > 0) (totalNg.toDouble() / totalSampling) * 100 else 0.0
}

/**
 * Payload data class terstandardisasi untuk verifikasi log sebelum sinkronisasi Supabase.
 */
@Serializable
data class ChecksheetSubmitPayload(
    val processType: String,
    val timestamp: Long,
    val parts: List<PartInspectedPayload>
)

@Serializable
data class PartInspectedPayload(
    val uniqNo: String,
    val totalSampling: Int,
    val totalOk: Int,
    val totalNg: Int,
    val defects: List<DefectInput>
)
