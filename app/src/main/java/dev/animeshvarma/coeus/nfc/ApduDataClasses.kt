package dev.animeshvarma.coeus.nfc

data class ApduCommand(
    val cla: Byte,
    val ins: Byte,
    val p1: Byte,
    val p2: Byte,
    val data: ByteArray? = null,
    val le: Int? = null
) {
    fun toByteArray(): ByteArray {
        // Simplified case 1/3 generation for MVP
        val header = byteArrayOf(cla, ins, p1, p2)
        if (data != null && data.isNotEmpty()) {
            return header + (data.size.toByte()) + data
        }
        return header
    }

    fun toHexString(): String = toByteArray().joinToString(" ") { "%02X".format(it) }
}

data class ApduResponse(
    val data: ByteArray,
    val sw1: Byte,
    val sw2: Byte
) {
    fun isSuccess(): Boolean = sw1 == 0x90.toByte() && sw2 == 0x00.toByte()

    fun toHexString(): String {
        val responseBytes = data + sw1 + sw2
        return responseBytes.joinToString(" ") { "%02X".format(it) }
    }
}