package com.primaraya.inspectra.fitur.checksheet.domain

import kotlinx.serialization.Serializable

@Serializable
data class DetailCuttingDto(
    val id: String? = null,
    val id_item: String,
    val no_lot: String? = null,
    val no_roll: String? = null,
    val size_cutting_cm: String? = null,
    val waste: Double? = null,
    val pic: String? = null
)
