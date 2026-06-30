package com.primaraya.inspectra.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "m_material")
data class MaterialEntity(
    @PrimaryKey
    val idMaterial: String,
    val namaMaterial: String,
    val supplier: String?,
    val terakhirDisinkronkan: Long
)
