package dev.animeshvarma.coeus.data

/**
 * Repository interface for NFC operations
 */
interface INfcRepository {
    suspend fun sendApduCommand(command: String): String
    fun isNfcSupported(): Boolean
    fun isNfcEnabled(): Boolean
}