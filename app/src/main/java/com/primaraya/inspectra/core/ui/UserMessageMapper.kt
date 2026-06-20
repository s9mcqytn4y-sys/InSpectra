package com.primaraya.inspectra.core.ui

data class UserMessage(
    val title: String,
    val body: String,
    val actionLabel: String? = null
)

object UserMessageMapper {

    fun fromThrowableMessage(raw: String?): UserMessage {
        val message = raw.orEmpty()

        return when {
            message.contains("Could not find the table", ignoreCase = true) ||
                message.contains("schema cache", ignoreCase = true) -> {
                UserMessage(
                    title = "Database belum siap",
                    body = "Data belum bisa dikirim karena konfigurasi server belum lengkap. Silakan hubungi admin.",
                    actionLabel = "Mengerti"
                )
            }

            message.contains("JWT", ignoreCase = true) ||
                message.contains("apikey", ignoreCase = true) ||
                message.contains("Authorization", ignoreCase = true) -> {
                UserMessage(
                    title = "Sesi server tidak valid",
                    body = "Aplikasi belum bisa terhubung dengan aman. Silakan tutup aplikasi lalu buka kembali.",
                    actionLabel = "Coba Lagi"
                )
            }

            message.contains("timeout", ignoreCase = true) ||
                message.contains("timed out", ignoreCase = true) -> {
                UserMessage(
                    title = "Koneksi lambat",
                    body = "Server membutuhkan waktu terlalu lama untuk merespons. Coba kirim ulang.",
                    actionLabel = "Coba Lagi"
                )
            }

            message.contains("Unable to resolve host", ignoreCase = true) ||
                message.contains("UnknownHost", ignoreCase = true) -> {
                UserMessage(
                    title = "Tidak ada koneksi",
                    body = "Periksa koneksi internet perangkat, lalu coba lagi.",
                    actionLabel = "Coba Lagi"
                )
            }

            else -> {
                UserMessage(
                    title = "Data belum terkirim",
                    body = "Terjadi kendala saat menyimpan data. Periksa koneksi lalu coba lagi.",
                    actionLabel = "Coba Lagi"
                )
            }
        }
    }
}
