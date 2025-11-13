package dev.animeshvarma.coeus.data

import android.nfc.Tag
import android.nfc.tech.IsoDep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException

/**
 * Sealed class to represent the result of NFC operations
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Exception? = null) : Result<Nothing>()
}

/**
 * Data class to hold tag information
 */
data class TagInfo(
    val uid: String,              // UID as hex string
    val tagType: String,          // Tag type/technology
    val historicalBytes: String?, // Historical bytes from tag response, if available
    val ats: String?              // ATS (Answer To Select) or historical bytes, if available
)

/**
 * Class responsible for managing NFC tag connections using IsoDep
 */
class TagConnectionManager(private val tag: Tag) {
    private var isoDep: IsoDep? = null
    private var timeout: Int = 5000 // Default timeout in milliseconds
    
    /**
     * Sets a custom timeout for tag communication
     */
    fun setTimeout(timeoutMs: Int) {
        timeout = timeoutMs
    }
    
    /**
     * Connects to the tag using IsoDep
     * @return Result object with connection status
     */
    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            isoDep = IsoDep.get(tag)
            isoDep?.let { 
                if (!it.isConnected) {
                    it.connect()
                    it.timeout = timeout
                    Timber.d("Connected to tag with UID: ${formatUid(tag.id)}")
                    Result.Success(Unit)
                } else {
                    Result.Success(Unit) // Already connected
                }
            } ?: Result.Error("IsoDep is not supported by this tag")
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception while connecting to tag")
            Result.Error("Security exception: ${e.message}", e)
        } catch (e: IOException) {
            Timber.e(e, "IO exception while connecting to tag")
            Result.Error("Connection failed: ${e.message}", e)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error while connecting to tag")
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }
    
    /**
     * Disconnects from the tag and cleans up resources
     */
    fun disconnect() {
        try {
            isoDep?.let { 
                if (it.isConnected) {
                    it.close()
                    Timber.d("Disconnected from tag")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during disconnect: ${e.message}")
        } finally {
            isoDep = null
        }
    }
    
    /**
     * Checks if the tag is currently connected
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean {
        return isoDep?.isConnected == true
    }
    
    /**
     * Retrieves tag information including UID, type, historical bytes, and ATS
     * @return Result object containing TagInfo or an error
     */
    suspend fun getTagInfo(): Result<TagInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            isoDep?.let { 
                if (!it.isConnected) {
                    return@withContext Result.Error("Not connected to tag")
                }
                
                // Get UID
                val uid = formatUid(tag.id)
                
                // Get tag type/technology
                val tagType = tag.techList.joinToString(", ")
                
                // Get historical bytes from ATS (Answer To Select)
                val historicalBytes = if (it.historicalBytes != null) {
                    formatBytes(it.historicalBytes)
                } else {
                    null
                }
                
                // Get ATS (Answer To Select) - Note: IsoDep doesn't have direct ATS property
                // Instead we can get the historical bytes, which often contain ATS information
                val ats = if (it.historicalBytes != null) {
                    formatBytes(it.historicalBytes)
                } else {
                    null
                }
                
                val tagInfo = TagInfo(
                    uid = uid,
                    tagType = tagType,
                    historicalBytes = historicalBytes,
                    ats = ats
                )
                
                Timber.d("Retrieved tag info: UID=$uid, Type=$tagType, HistoricalBytes=$historicalBytes, ATS=$ats")
                Result.Success(tagInfo)
            } ?: Result.Error("IsoDep is not initialized. Call connect() first.")
        } catch (e: IOException) {
            Timber.e(e, "IO exception while getting tag info: ${e.message}")
            Result.Error("IO exception while getting tag info: ${e.message}", e)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error while getting tag info: ${e.message}")
            Result.Error("Unexpected error while getting tag info: ${e.message}", e)
        }
    }
    
    /**
     * Sends a command to the tag and returns the response
     * @param command The command bytes to send
     * @return Result object containing the response bytes or an error
     */
    suspend fun transceive(command: ByteArray): Result<ByteArray> = withContext(Dispatchers.IO) {
        return@withContext try {
            isoDep?.let {
                if (!it.isConnected) {
                    return@withContext Result.Error("Not connected to tag")
                }
                
                val response = it.transceive(command)
                Timber.d("Sent command: ${formatBytes(command)}, received response: ${formatBytes(response)}")
                Result.Success(response)
            } ?: Result.Error("IsoDep is not initialized. Call connect() first.")
        } catch (e: IOException) {
            Timber.e(e, "IO exception during transceive: ${e.message}")
            Result.Error("IO exception during transceive: ${e.message}", e)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during transceive: ${e.message}")
            Result.Error("Unexpected error during transceive: ${e.message}", e)
        }
    }
    
    /**
     * Formats a byte array as a hex string with spaces between bytes
     */
    private fun formatBytes(bytes: ByteArray?): String {
        if (bytes == null) return "null"
        return bytes.joinToString(" ") { String.format("%02X", it) }
    }
    
    /**
     * Formats a UID as a hex string with colons between bytes (e.g., "04:A1:B2:C3")
     */
    private fun formatUid(uid: ByteArray): String {
        return uid.joinToString(separator = ":") { String.format("%02X", it) }
    }
    
    /**
     * Closes the connection and performs cleanup
     */
    fun close() {
        disconnect()
    }
}