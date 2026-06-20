package com.primaraya.inspectra.core.ui

/**
 * Konteks operasi yang sedang dilakukan user.
 */
enum class KonteksOperasi {
    MASTER_DATA,
    CHECKSHEET,
    UMUM
}

/**
 * Pesan aman untuk user tanpa membocorkan detail teknis.
 */
data class UserMessage(
    val title: String,
    val body: String,
    val actionLabel: String? = "Mengerti"
)

/**
 * Mapper error teknis menjadi pesan user-friendly.
 */
object UserMessageMapper {

    /**
     * Mengubah pesan error teknis menjadi pesan aman sesuai konteks layar.
     */
    fun fromThrowableMessage(
        raw: String?,
        konteks: KonteksOperasi = KonteksOperasi.UMUM
    ): UserMessage {
        val message = raw.orEmpty()

        return when {
            message.contains("Could not find the table", ignoreCase = true) ||
                message.contains("schema cache", ignoreCase = true) -> {
                when (konteks) {
                    KonteksOperasi.MASTER_DATA -> UserMessage(
                        title = "Data master belum siap",
                        body = "Struktur data master belum lengkap. Silakan hubungi admin untuk sinkronisasi database."
                    )

                    KonteksOperasi.CHECKSHEET -> UserMessage(
                        title = "Data pemeriksaan belum siap",
                        body = "Acuan part dan defect belum lengkap. Silakan hubungi admin."
                    )

                    KonteksOperasi.UMUM -> UserMessage(
                        title = "Database belum siap",
                        body = "Konfigurasi server belum lengkap. Silakan hubungi admin."
                    )
                }
            }

            message.contains("permission", ignoreCase = true) ||
                message.contains("policy", ignoreCase = true) ||
                message.contains("RLS", ignoreCase = true) -> UserMessage(
                title = "Akses belum aktif",
                body = "Aplikasi belum memiliki izin untuk menyimpan data. Silakan hubungi admin."
            )

            message.contains("timeout", ignoreCase = true) ||
                message.contains("timed out", ignoreCase = true) -> UserMessage(
                title = "Koneksi lambat",
                body = "Server membutuhkan waktu terlalu lama. Coba ulangi beberapa saat lagi.",
                actionLabel = "Coba Lagi"
            )

            message.contains("Unable to resolve host", ignoreCase = true) ||
                message.contains("UnknownHost", ignoreCase = true) -> UserMessage(
                title = "Tidak ada koneksi",
                body = "Periksa koneksi internet perangkat, lalu coba lagi.",
                actionLabel = "Coba Lagi"
            )

            else -> UserMessage(
                title = "Proses belum berhasil",
                body = "Terjadi kendala saat memproses data. Coba ulangi beberapa saat lagi.",
                actionLabel = "Coba Lagi"
            )
        }
    }
}
