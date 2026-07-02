package com.primaraya.inspectra.core.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Enumerasi departemen/line produksi di InSpectra.
 */
@Serializable
enum class LineProcess {
    @SerialName("PRESS_SPRAY") PRESS_SPRAY,
    @SerialName("PRESS_CUTTING") PRESS_CUTTING,
    @SerialName("PRESS_PRESS") PRESS_PRESS,
    @SerialName("SEWING") SEWING,
    @SerialName("QUALITY_CONTROL") QUALITY_CONTROL
}

/**
 * Tipe pekerja (Karyawan internal vs PKL/Intern).
 */
@Serializable
enum class TipePekerja {
    @SerialName("KARYAWAN") KARYAWAN,
    @SerialName("PKL") PKL
}

/**
 * Status kehadiran harian.
 */
@Serializable
enum class KeteranganHadir {
    @SerialName("HADIR") HADIR,
    @SerialName("SAKIT") SAKIT,
    @SerialName("TELAT") TELAT,
    @SerialName("CUTI") CUTI,
    @SerialName("IZIN_PULANG") IZIN_PULANG,
    @SerialName("IZIN_TELAT") IZIN_TELAT
}

/**
 * Kategori aktivitas lembur di luar pekerjaan utama.
 */
@Serializable
enum class LemburNonMainJob {
    @SerialName("TIDAK_ADA") TIDAK_ADA,
    @SerialName("BANTU_LINE_LAIN") BANTU_LINE_LAIN,
    @SerialName("5S") AKTIVITAS_5S,
    @SerialName("REWORK_REPAIR") REWORK_REPAIR,
    @SerialName("SETTING_MESIN") SETTING_MESIN
}
