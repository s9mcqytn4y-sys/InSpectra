package com.primaraya.inspectra.core.common

class ReferenceCache<T>(
    private val ttlMillis: Long = 5 * 60 * 1000L
) {
    private var value: T? = null
    private var expiredAt: Long = 0L
    private var version: Long = -1L

    fun getIfValid(currentVersion: Long = -1L): T? {
        val now = System.currentTimeMillis()
        if (now >= expiredAt) return null
        if (currentVersion != -1L && version != -1L && currentVersion > version) return null
        return value
    }

    fun put(newValue: T, version: Long = -1L) {
        this.value = newValue
        this.version = version
        this.expiredAt = System.currentTimeMillis() + ttlMillis
    }

    fun clear() {
        value = null
        expiredAt = 0L
        version = -1L
    }
}
