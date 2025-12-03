package dev.animeshvarma.coeus.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException

sealed class TagResult<out T> {
    data class Success<T>(val data: T) : TagResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : TagResult<Nothing>()
}

class TagConnectionManager(private val tag: Tag) {
    private var isoDep: IsoDep? = null
    private var timeout: Int = 5000

    fun setTimeout(timeoutMs: Int) {
        timeout = timeoutMs
    }

    suspend fun connect(): TagResult<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            isoDep = IsoDep.get(tag)
            isoDep?.let {
                if (!it.isConnected) {
                    it.connect()
                    it.timeout = timeout
                    Timber.d("Connected via IsoDep")
                    TagResult.Success(Unit)
                } else {
                    TagResult.Success(Unit)
                }
            } ?: TagResult.Error("IsoDep not supported by this tag")
        } catch (e: Exception) {
            Timber.e(e, "Connection failed")
            TagResult.Error("Connection failed: ${e.message}", e)
        }
    }

    fun close() {
        try {
            isoDep?.close()
        } catch (e: Exception) {
            Timber.e(e, "Error closing tag")
        }
    }

    suspend fun transceive(command: ByteArray): TagResult<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val response = isoDep?.transceive(command)
                ?: return@withContext TagResult.Error("Not connected")
            TagResult.Success(response)
        } catch (e: Exception) {
            TagResult.Error("Transceive failed: ${e.message}", e)
        }
    }
}