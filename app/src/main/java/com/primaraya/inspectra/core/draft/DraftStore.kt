package com.primaraya.inspectra.core.draft

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.inspectraDraftDataStore by preferencesDataStore("inspectra_drafts")

/**
 * Store ringan untuk menyimpan draft form.
 *
 * Digunakan agar input user tidak hilang saat rotate, app keluar,
 * atau proses Android dibunuh.
 */
class DraftStore(
    private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun <T> readDraft(
        key: String,
        serializer: KSerializer<T>
    ): Flow<T?> {
        val prefKey = stringPreferencesKey(key)
        return context.inspectraDraftDataStore.data.map { pref ->
            pref[prefKey]?.let {
                runCatching { json.decodeFromString(serializer, it) }.getOrNull()
            }
        }
    }

    suspend fun <T> saveDraft(
        key: String,
        serializer: KSerializer<T>,
        data: T
    ) {
        val prefKey = stringPreferencesKey(key)
        context.inspectraDraftDataStore.edit { pref ->
            pref[prefKey] = json.encodeToString(serializer, data)
        }
    }

    suspend fun clearDraft(key: String) {
        val prefKey = stringPreferencesKey(key)
        context.inspectraDraftDataStore.edit { pref ->
            pref.remove(prefKey)
        }
    }
}
