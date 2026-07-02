package com.primaraya.inspectra.fitur.laporan.ui

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

object LaporanContract {
    enum class Step {
        PILIH_PART,
        ISI_FORM
    }

    @Immutable
    data class State(
        val step: Step = Step.PILIH_PART,
        val isLoading: Boolean = false,
        val masterParts: ImmutableList<com.primaraya.inspectra.fitur.checksheet.domain.PartAcuan> = persistentListOf(),
        val selectedPartIds: Set<String> = persistentSetOf(),
        val tanggal: String = "",
        val tipeProses: String = "",
        val mpDirect: String = "",
        val mpIndirect: String = "",
        val jknHour: String = "",
        val jknMenit: String = "",
        val lemburProd: String = "",
        val lemburNon: String = "",
        val bantuanKeluar: String = "",
        val bantuanMasuk: String = "",
        val details: PersistentList<DetailLaporanState> = persistentListOf(),
        val errorMessage: String? = null
    ) {
        val totalActual: Int get() = details.sumOf { it.actual.toIntOrNull() ?: 0 }
        val totalNg: Int get() = details.sumOf { it.ng.toIntOrNull() ?: 0 }
        val totalPlanning: Int get() = details.sumOf { it.planning.toIntOrNull() ?: 0 }
        val isValid: Boolean get() = tipeProses.isNotBlank() && tanggal.isNotBlank() && details.isNotEmpty()
    }

    @Immutable
    data class DetailLaporanState(
        val idPart: String,
        val namaPart: String,
        val planning: String = "",
        val actual: String = "",
        val ng: String = ""
    )

    sealed interface Intent {
        data class Muat(val tipeProses: String) : Intent
        data class UpdateTanggal(val tanggal: String) : Intent
        data class UpdateMpDirect(val value: String) : Intent
        data class UpdateMpIndirect(val value: String) : Intent
        data class UpdateJknHour(val value: String) : Intent
        data class UpdateJknMenit(val value: String) : Intent
        data class UpdateLemburProd(val value: String) : Intent
        data class UpdateLemburNon(val value: String) : Intent
        data class UpdateBantuanKeluar(val value: String) : Intent
        data class UpdateBantuanMasuk(val value: String) : Intent
        
        data class UpdateDetailPlanning(val index: Int, val value: String) : Intent
        data class UpdateDetailActual(val index: Int, val value: String) : Intent
        data class UpdateDetailNg(val index: Int, val value: String) : Intent
        
        object Submit : Intent
        object DismissError : Intent
        data class TogglePilihPart(val idPart: String) : Intent
        object LanjutKeForm : Intent
        object KembaliKePilihPart : Intent
    }

    sealed interface Effect {
        data class ShowSnackbar(val message: String) : Effect
        object NavigateBack : Effect
    }
}
