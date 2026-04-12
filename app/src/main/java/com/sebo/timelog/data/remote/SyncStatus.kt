package com.sebo.timelog.data.remote

data class SyncStatus(
    val configured: Boolean,
    val isSyncing: Boolean = false,
    val lastSuccessAt: Long? = null,
    val lastErrorAt: Long? = null,
    val lastErrorMessage: String? = null
) {
    companion object {
        fun notConfigured(): SyncStatus = SyncStatus(configured = false)
        fun idleConfigured(): SyncStatus = SyncStatus(configured = true)
    }
}

