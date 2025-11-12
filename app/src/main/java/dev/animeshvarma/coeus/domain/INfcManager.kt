package dev.animeshvarma.coeus.domain

/**
 * Interface for NFC operations
 */
interface INfcManager {
    suspend fun sendApduCommand(command: String): String
    fun isNfcSupported(): Boolean
    fun isNfcEnabled(): Boolean
}