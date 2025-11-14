package dev.animeshvarma.coeus.data.apdu

import kotlin.text.isNullOrEmpty

/**
 * Represents an APDU (Application Protocol Data Unit) command.
 *
 * APDU commands follow the structure:
 * - CLA (Class byte): Defines the group of APDU commands
 * - INS (Instruction byte): Identifies the instruction
 * - P1 (Parameter 1): First parameter
 * - P2 (Parameter 2): Second parameter
 * - Lc (Length of command data): Number of data bytes (calculated automatically)
 * - Data: Command data bytes (optional)
 * - Le (Expected response length): Maximum number of data bytes expected in response (optional)
 *
 * @property cla Class byte (0-255)
 * @property ins Instruction byte (0-255)
 * @property p1 First parameter (0-255)
 * @property p2 Second parameter (0-255)
 * @property data Command data bytes (optional)
 * @property le Expected response length (optional)
 */
data class ApduCommand(
    val cla: Byte,
    val ins: Byte,
    val p1: Byte,
    val p2: Byte,
    val data: ByteArray? = null,
    val le: Int? = null
) {
    /**
     * Builds the complete APDU command as a byte array according to ISO/IEC 7816-4 specification
     *
     * APDU Command Format:
     * - Case 1: CLA INS P1 P2
     * - Case 2: CLA INS P1 P2 Le
     * - Case 3: CLA INS P1 P2 Lc Data
     * - Case 4: CLA INS P1 P2 Lc Data Le
     *
     * @return Byte array representing the complete APDU command
     */
    fun toByteArray(): ByteArray {
        val hasData = data != null && data.isNotEmpty()
        val hasLe = le != null
        
        return when {
            // Case 1: No data, no Le
            !hasData && !hasLe -> byteArrayOf(cla, ins, p1, p2)
            
            // Case 2: No data, but Le is present
            !hasData && hasLe -> {
                val leByte = if (le == 0) 0x00.toByte() else (le!! and 0xFF).toByte()
                byteArrayOf(cla, ins, p1, p2, leByte)
            }
            
            // Case 3: Data present, no Le
            hasData && !hasLe -> {
                val lc = data!!.size and 0xFF
                val result = ByteArray(5 + data!!.size)
                result[0] = cla
                result[1] = ins
                result[2] = p1
                result[3] = p2
                result[4] = lc.toByte()
                System.arraycopy(data, 0, result, 5, data!!.size)
                result
            }
            
            // Case 4: Data present and Le
            hasData && hasLe -> {
                val lc = data!!.size and 0xFF
                val result = ByteArray(6 + data!!.size)
                result[0] = cla
                result[1] = ins
                result[2] = p1
                result[3] = p2
                result[4] = lc.toByte()
                System.arraycopy(data, 0, result, 5, data!!.size)
                
                // Handle different LE formats based on spec
                if (le == 0) {
                    result[result.size - 1] = 0x00.toByte()
                } else {
                    result[result.size - 1] = (le!! and 0xFF).toByte()
                }
                result
            }
            
            else -> byteArrayOf(cla, ins, p1, p2) // Default case
        }
    }
    
    /**
     * Returns the hex representation of this APDU command
     * Each byte is represented in uppercase hex format separated by spaces
     *
     * @return Hex string representation (e.g., "00 A4 04 00")
     */
    fun toHexString(): String {
        return toByteArray().joinToString(" ") { String.format("%02X", it) }
    }
}

/**
 * Represents an APDU (Application Protocol Data Unit) response.
 *
 * APDU responses follow the structure:
 * - Data: Response data bytes (0 or more)
 * - SW1 (Status Word 1): First byte of status word
 * - SW2 (Status Word 2): Second byte of status word
 * - Total structure: [Data][SW1][SW2] - where data is optional
 *
 * @property data Response data bytes (may be empty)
 * @property sw1 Status word byte 1
 * @property sw2 Status word byte 2
 */
data class ApduResponse(
    val data: ByteArray,
    val sw1: Byte,
    val sw2: Byte
) {
    /**
     * Checks if this APDU response indicates success.
     * Success is defined as SW1=0x90 and SW2=0x00, which means "Normal processing: Command accepted".
     *
     * @return True if the response indicates success, false otherwise
     */
    fun isSuccess(): Boolean {
        return sw1 == 0x90.toByte() && sw2 == 0x00.toByte()
    }
    
    /**
     * Returns the hex representation of this APDU response
     * All data bytes and status words are represented in uppercase hex format separated by spaces
     *
     * @return Hex string representation (e.g., "6F 12 90 00")
     */
    fun toHexString(): String {
        val responseBytes = ByteArray(data.size + 2)
        System.arraycopy(data, 0, responseBytes, 0, data.size)
        responseBytes[data.size] = sw1
        responseBytes[data.size + 1] = sw2
        
        return responseBytes.joinToString(" ") { String.format("%02X", it) }
    }
    
    /**
     * Returns the combined status word as an integer.
     * SW1 is the high-order byte, SW2 is the low-order byte.
     *
     * @return Combined status word as integer (e.g., 0x9000 for success)
     */
    fun getStatusWord(): Int {
        return ((sw1.toInt() and 0xFF) shl 8) or (sw2.toInt() and 0xFF)
    }
}