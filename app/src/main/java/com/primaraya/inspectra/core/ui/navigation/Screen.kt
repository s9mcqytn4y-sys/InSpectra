package com.primaraya.inspectra.core.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Splash : Screen

    @Serializable
    data object Dashboard : Screen

    @Serializable
    data object MenuChecksheet : Screen

    @Serializable
    data class FormChecksheet(val tipeProses: String) : Screen

    @Serializable
    data object MenuLaporan : Screen

    @Serializable
    data class FormLaporan(val tipeProses: String) : Screen

    @Serializable
    data object MasterData : Screen
}
