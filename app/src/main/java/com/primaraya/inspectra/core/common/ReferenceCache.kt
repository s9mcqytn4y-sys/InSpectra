package com.primaraya.inspectra.core.common

class ReferenceCache<T>(
    private val ttlMillis: Long = 5 * 60 * 1000L
) {
    private var value: T? = null
    private var expiredAt: Long = 0L

    fun getOrNull(): T? {
        return if (System.currentTimeMillis() < expiredAt) value else null
    }

    fun put(newValue: T) {
        value = newValue
        expiredAt = System.currentTimeMillis() + ttlMillis
    }

    fun clear() {
        value = null
        expiredAt = 0L
    }
}
