package dev.animeshvarma.coeus.data.models

data class ApduCommand(
    val command: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ApduResponse(
    val response: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)