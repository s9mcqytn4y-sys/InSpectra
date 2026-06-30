package com.primaraya.inspectra.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "m_part")
data class PartEntity(
    @PrimaryKey
    val uniqNo: String,
    val partNo: String?,
    val namaPart: String,
    val model: String?,
    val customer: String?,
    val komoditas: String,
    val imageUrl: String?,
    val menggunakanDefault: Boolean,
    val jumlahMaterial: Int,
    val jumlahDefect: Int,
    val statusInput: String, // "SIAP_INPUT", dll
    val terakhirDisinkronkan: Long
)
