package dev.animeshvarma.coeus.data.apdu

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for APDU data classes
 */
class ApduDataClassesTest {
    
    @Test
    fun `ApduCommand toByteArray creates correct Case 1 APDU (no data, no Le)`() {
        val command = ApduCommand(
            cla = 0x00,
            ins = 0xA4.toByte(),
            p1 = 0x04,
            p2 = 0x00,
            data = null,
            le = null
        )
        
        val expected = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)
        val actual = command.toByteArray()
        
        assertArrayEquals(expected, actual)
        assertEquals("00 A4 04 00", command.toHexString())
    }
    
    @Test
    fun `ApduCommand toByteArray creates correct Case 2 APDU (no data, with Le)`() {
        val command = ApduCommand(
            cla = 0x00,
            ins = 0xB0.toByte(),
            p1 = 0x00,
            p2 = 0x00,
            data = null,
            le = 256 // This should be handled as 0x00 for extended length
        )
        
        // For case 2, with Le=256, it will be 0x00 (normalized)
        val expected = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x00)
        val actual = command.toByteArray()
        
        assertArrayEquals(expected, actual)
    }
    
    @Test
    fun `ApduCommand toByteArray creates correct Case 2 with small Le`() {
        val command = ApduCommand(
            cla = 0x00,
            ins = 0xB0.toByte(),
            p1 = 0x00,
            p2 = 0x00,
            data = null,
            le = 32
        )
        
        val expected = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x20) // 32 = 0x20
        val actual = command.toByteArray()
        
        assertArrayEquals(expected, actual)
        assertEquals("00 B0 00 00 20", command.toHexString())
    }
    
    @Test
    fun `ApduCommand toByteArray creates correct Case 3 APDU (with data, no Le)`() {
        val data = byteArrayOf(0x01, 0x02, 0x03)
        val command = ApduCommand(
            cla = 0x00,
            ins = 0xD6.toByte(),
            p1 = 0x00,
            p2 = 0x00,
            data = data,
            le = null
        )
        
        val expected = byteArrayOf(0x00, 0xD6.toByte(), 0x00, 0x00, 0x03, 0x01, 0x02, 0x03)
        val actual = command.toByteArray()
        
        assertArrayEquals(expected, actual)
        assertEquals("00 D6 00 00 03 01 02 03", command.toHexString())
    }
    
    @Test
    fun `ApduCommand toByteArray creates correct Case 4 APDU (with data and Le)`() {
        val data = byteArrayOf(0x01, 0x02)
        val command = ApduCommand(
            cla = 0x00,
            ins = 0xD6.toByte(),
            p1 = 0x00,
            p2 = 0x00,
            data = data,
            le = 16
        )
        
        val expected = byteArrayOf(0x00, 0xD6.toByte(), 0x00, 0x00, 0x02, 0x01, 0x02, 0x10) // 16 = 0x10
        val actual = command.toByteArray()
        
        assertArrayEquals(expected, actual)
        assertEquals("00 D6 00 00 02 01 02 10", command.toHexString())
    }
    
    @Test
    fun `ApduCommand toByteArray handles empty data correctly`() {
        val command = ApduCommand(
            cla = 0x00,
            ins = 0xA4.toByte(),
            p1 = 0x04,
            p2 = 0x00,
            data = byteArrayOf(), // Empty but not null
            le = null
        )
        
        // This should result in a Case 3 format with Lc=0
        val expected = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x00)
        val actual = command.toByteArray()
        
        assertArrayEquals(expected, actual)
        assertEquals("00 A4 04 00 00", command.toHexString())
    }
    
    @Test
    fun `ApduResponse isSuccess returns true for success status (90 00)`() {
        val response = ApduResponse(
            data = byteArrayOf(),
            sw1 = 0x90.toByte(),
            sw2 = 0x00.toByte()
        )
        
        assertTrue(response.isSuccess())
        assertEquals(0x9000, response.getStatusWord())
        assertEquals("90 00", response.toHexString())
    }
    
    @Test
    fun `ApduResponse isSuccess returns false for error status`() {
        val response = ApduResponse(
            data = byteArrayOf(),
            sw1 = 0x6A.toByte(),
            sw2 = 0x82.toByte()
        )
        
        assertFalse(response.isSuccess())
        assertEquals(0x6A82, response.getStatusWord())
        assertEquals("6A 82", response.toHexString())
    }
    
    @Test
    fun `ApduResponse isSuccess returns false for other success-like codes`() {
        val response = ApduResponse(
            data = byteArrayOf(),
            sw1 = 0x91.toByte(), // Not exactly 0x90
            sw2 = 0x00.toByte()
        )
        
        assertFalse(response.isSuccess())
        assertEquals(0x9100, response.getStatusWord())
    }
    
    @Test
    fun `ApduResponse with data returns correct hex string`() {
        val response = ApduResponse(
            data = byteArrayOf(0x6F.toByte(), 0x12.toByte(), 0x84.toByte(), 0x00.toByte()),
            sw1 = 0x90.toByte(),
            sw2 = 0x00.toByte()
        )
        
        assertEquals("6F 12 84 00 90 00", response.toHexString())
        assertEquals(0x9000, response.getStatusWord())
    }
    
    @Test
    fun `ApduResponse getStatusWord combines SW1 and SW2 correctly`() {
        val response = ApduResponse(
            data = byteArrayOf(),
            sw1 = 0x6A.toByte(),
            sw2 = 0x87.toByte()
        )
        
        assertEquals(0x6A87, response.getStatusWord())
    }
    
    @Test
    fun `ApduCommand with null data is handled correctly`() {
        val command = ApduCommand(
            cla = 0x00,
            ins = 0xA4.toByte(),
            p1 = 0x00,
            p2 = 0x00,
            data = null, // Explicitly null
            le = 255
        )
        
        val expected = byteArrayOf(0x00, 0xA4.toByte(), 0x00, 0x00, (255 and 0xFF).toByte())
        val actual = command.toByteArray()
        
        assertArrayEquals(expected, actual)
    }
}