package dev.animeshvarma.coeus.domain

/**
 * Use case for sending APDU commands via NFC
 */
class SendApduUseCase(private val nfcManager: INfcManager) {
    suspend operator fun invoke(command: String): Result<String> {
        return try {
            if (!nfcManager.isNfcSupported()) {
                return Result.failure(Exception("NFC is not supported on this device"))
            }
            
            if (!nfcManager.isNfcEnabled()) {
                return Result.failure(Exception("NFC is not enabled. Please enable NFC in settings"))
            }
            
            val response = nfcManager.sendApduCommand(command)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}