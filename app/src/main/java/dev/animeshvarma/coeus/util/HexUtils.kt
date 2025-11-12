package dev.animeshvarma.coeus.util

import java.util.regex.Pattern

/**
 * Utility class for hex string validation and conversion
 */
object HexUtils {
    private val HEX_PATTERN = Pattern.compile("[0-9A-Fa-f]+")

    /**
     * Validates if the input string is a valid hex string
     */
    fun isValidHexString(input: String): Boolean {
        return input.isNotEmpty() && HEX_PATTERN.matcher(input).matches()
    }

    /**
     * Converts a hex string to byte array
     */
    fun hexStringToByteArray(hex: String): ByteArray? {
        if (!isValidHexString(hex)) {
            return null
        }

        val cleanHex = hex.replace(" ", "")
        val bytes = ByteArray(cleanHex.length / 2)
        for (i in cleanHex.indices step 2) {
            bytes[i / 2] = ((Character.digit(cleanHex[i], 16) shl 4)
                    + Character.digit(cleanHex[i + 1], 16)).toByte()
        }
        return bytes
    }

    /**
     * Converts a byte array to hex string
     */
    fun byteArrayToHexString(bytes: ByteArray): String {
        val result = StringBuilder()
        for (b in bytes) {
            result.append(String.format("%02X", b))
        }
        return result.toString()
    }
}