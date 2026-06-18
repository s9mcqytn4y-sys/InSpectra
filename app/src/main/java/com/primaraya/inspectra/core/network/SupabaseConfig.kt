package com.primaraya.inspectra.core.network

import com.primaraya.inspectra.BuildConfig

data class SupabaseConfig(
    val url: String,
    val key: String
) {
    val isValid: Boolean
        get() = url.startsWith("https://") &&
            url.contains(".supabase.co") &&
            key.startsWith("sb_publishable_")

    companion object {
        fun fromBuildConfig(): SupabaseConfig {
            return SupabaseConfig(
                url = BuildConfig.SUPABASE_URL.trim().removeSurrounding("\""),
                key = BuildConfig.SUPABASE_KEY.trim().removeSurrounding("\"")
            )
        }
    }
}
