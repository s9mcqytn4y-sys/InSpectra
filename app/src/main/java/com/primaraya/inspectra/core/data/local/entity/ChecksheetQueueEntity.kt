package com.primaraya.inspectra.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checksheet_queue")
data class ChecksheetQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val payloadJson: String,
    val status: String, // "PENDING", "PROCESSING", "FAILED"
    val createdAt: Long,
    val retryCount: Int = 0
)
