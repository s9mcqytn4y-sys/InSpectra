package com.primaraya.inspectra.ui.screens.checksheet

import androidx.lifecycle.ViewModel
import com.primaraya.inspectra.domain.model.DefectInput
import com.primaraya.inspectra.domain.model.PartChecksheetSummary
import com.primaraya.inspectra.domain.model.ProcessType
import com.primaraya.inspectra.domain.model.ChecksheetSubmitPayload
import com.primaraya.inspectra.domain.model.PartInspectedPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChecksheetViewModel : ViewModel() {

    // Master data riil yang telah dibersihkan dari prefix abjad (A, B, C)
    private val masterDataInspectra = listOf(
        PartChecksheetSummary(
            uniqNo = "BJ1", partNumber = "79977-BZ020", partName = "FELT SEAT BACK",
            commodity = ProcessType.SEWING, imageUrl = null,
            defects = listOf(
                DefectInput("S1", "SOBEK"), DefectInput("S2", "BRUDUL"),
                DefectInput("S3", "TIPIS"), DefectInput("S4", "SPUNBOUND KOTOR"),
                DefectInput("S5", "LAMINATING BOLONG")
            )
        ),
        PartChecksheetSummary(
            uniqNo = "CB9", partNumber = "58815-KK010", partName = "CARPET CONSOLE BOX",
            commodity = ProcessType.PRESS, imageUrl = null,
            defects = listOf(
                DefectInput("P1", "DENT"), DefectInput("P2", "GALER"),
                DefectInput("P3", "CARPET TIPIS"), DefectInput("P4", "BELANG"),
                DefectInput("P5", "HOLE T/A"), DefectInput("P6", "OVERCUTTING"),
                DefectInput("P7", "SOBEK"), DefectInput("P8", "TERLIPAT")
            )
        )
    )

    private val _uiState = MutableStateFlow<List<PartChecksheetSummary>>(emptyList())
    val uiState: StateFlow<List<PartChecksheetSummary>> = _uiState.asStateFlow()

    fun loadChecksheetByProcess(processType: ProcessType) {
        _uiState.value = masterDataInspectra.filter { it.commodity == processType }.map { it.copy(defects = it.defects.map { d -> d.copy() }) }
    }

    fun toggleExpand(uniqNo: String) {
        _uiState.update { list ->
            list.map { if (it.uniqNo == uniqNo) it.copy(isExpanded = !it.isExpanded) else it }
        }
    }

    fun updateSampling(uniqNo: String, sampling: Int) {
        _uiState.update { list ->
            list.map { if (it.uniqNo == uniqNo) it.copy(totalSampling = sampling) else it }
        }
    }

    // Mengubah kuantitas via klik tombol + / -
    fun updateDefectQty(uniqNo: String, defectId: String, isIncrement: Boolean) {
        _uiState.update { list ->
            list.map { part ->
                if (part.uniqNo == uniqNo) {
                    val updatedDefects = part.defects.map { defect ->
                        if (defect.defectId == defectId) {
                            val newQty = if (isIncrement) defect.ngQty + 1 else (defect.ngQty - 1).coerceAtLeast(0)
                            defect.copy(ngQty = newQty)
                        } else defect
                    }
                    part.copy(defects = updatedDefects)
                } else part
            }
        }
    }

    // BARU: Mengubah kuantitas langsung via input teks keyboard manual
    fun setDefectQty(uniqNo: String, defectId: String, qty: Int) {
        _uiState.update { list ->
            list.map { part ->
                if (part.uniqNo == uniqNo) {
                    val updatedDefects = part.defects.map { defect ->
                        if (defect.defectId == defectId) {
                            defect.copy(ngQty = qty.coerceAtLeast(0))
                        } else defect
                    }
                    part.copy(defects = updatedDefects)
                } else part
            }
        }
    }

    fun generateFinalPayload(processType: ProcessType): ChecksheetSubmitPayload {
        return ChecksheetSubmitPayload(
            processType = processType.name,
            timestamp = System.currentTimeMillis(),
            parts = _uiState.value.filter { it.totalSampling > 0 }.map { part ->
                PartInspectedPayload(
                    uniqNo = part.uniqNo,
                    totalSampling = part.totalSampling,
                    totalOk = part.totalOk,
                    totalNg = part.totalNg,
                    defects = part.defects.filter { it.ngQty > 0 }
                )
            }
        )
    }
}
