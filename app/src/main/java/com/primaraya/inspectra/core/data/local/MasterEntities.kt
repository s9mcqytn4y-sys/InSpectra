package com.primaraya.inspectra.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "m_part_local")
data class PartEntity(
    @PrimaryKey val id: String,
    val part_no: String?,
    val uniq_no: String,
    val nama_part: String,
    val model: String?,
    val customer: String?,
    val komoditas: String,
    val lokasi_gambar: String?,
    val aktif: Boolean,
    val status_kelengkapan: String?,
    val butuh_review: Boolean,
    val last_sync: Long = System.currentTimeMillis()
)

@Entity(tableName = "m_material_local")
data class MaterialEntity(
    @PrimaryKey val id: String,
    val supplier_id: String?,
    val supplier: String?,
    val nama_material: String,
    val spec: String?,
    val satuan: String?,
    val last_sync: Long = System.currentTimeMillis()
)

@Entity(tableName = "m_defect_local")
data class DefectEntity(
    @PrimaryKey val id_defect: String,
    val nama_defect: String,
    val kategori: String,
    val aktif: Boolean,
    val last_sync: Long = System.currentTimeMillis()
)

@Entity(tableName = "m_karyawan_local")
data class KaryawanLocalEntity(
    @PrimaryKey val id: String,
    val nama_lengkap: String,
    val tipe_pekerja: String,
    val no_reg: String?,
    val line_process: String,
    val aktif: Boolean,
    val last_sync: Long = System.currentTimeMillis()
)

@Entity(tableName = "t_kehadiran_local")
data class KehadiranLocalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tanggal: String,
    val line_process: String,
    val karyawan_id: String,
    val keterangan: String,
    val jam_lembur_aktual: Float,
    val lembur_non_main_job: String,
    val is_synced: Boolean = false
)
