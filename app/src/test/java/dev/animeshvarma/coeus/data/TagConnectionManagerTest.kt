package dev.animeshvarma.coeus.data

import android.nfc.Tag
import android.nfc.tech.IsoDep
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import java.io.IOException

/**
 * Unit tests for TagConnectionManager
 */
@RunWith(MockitoJUnitRunner::class)
class TagConnectionManagerTest {
    
    @Mock
    private lateinit var mockTag: Tag
    
    @Mock
    private lateinit var mockIsoDep: IsoDep
    
    private lateinit var tagConnectionManager: TagConnectionManager
    private val mockTagId = byteArrayOf(0x01, 0x02, 0x03, 0x04)
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock tag behavior
        `when`(mockTag.id).thenReturn(mockTagId)
        `when`(mockTag.techList).thenReturn(arrayOf("android.nfc.tech.IsoDep"))
        
        // Mock IsoDep behavior
        `when`(mockIsoDep.isConnected).thenReturn(false)
        `when`(mockIsoDep.historicalBytes).thenReturn(byteArrayOf(0x01, 0x02, 0x03))

        // Mock IsoDep.get to return our mock IsoDep
        `when`(IsoDep.get(mockTag)).thenReturn(mockIsoDep)
        
        tagConnectionManager = TagConnectionManager(mockTag)
    }
    
    @Test
    fun `connect should return Success when connection is successful`() = runBlocking {
        `when`(mockIsoDep.isConnected).thenReturn(false)
        `when`(mockIsoDep.connect()).then {
            `when`(mockIsoDep.isConnected).thenReturn(true)
            Unit
        }
        
        val result = tagConnectionManager.connect()
        
        assert(result is Result.Success)
        assert(tagConnectionManager.isConnected())
    }
    
    @Test
    fun `connect should return Error when IOException occurs`() = runBlocking {
        `when`(mockIsoDep.connect()).thenThrow(IOException("Connection failed"))
        
        val result = tagConnectionManager.connect()
        
        assert(result is Result.Error)
        assert((result as Result.Error).message.contains("Connection failed"))
    }
    
    @Test
    fun `getTagInfo should return TagInfo when connected`() = runBlocking {
        `when`(mockIsoDep.isConnected).thenReturn(true)
        `when`(mockIsoDep.historicalBytes).thenReturn(byteArrayOf(0x01.toByte(), 0x02.toByte()))

        val result = tagConnectionManager.getTagInfo()

        assert(result is Result.Success)
        if (result is Result.Success) {
            val tagInfo = result.data
            assert(tagInfo.uid == "01:02:03:04")
            assert(tagInfo.tagType == "android.nfc.tech.IsoDep")
            assert(tagInfo.historicalBytes == "01 02")
            assert(tagInfo.ats == "01 02") // Since ATS uses historical bytes in our implementation
        }
    }
    
    @Test
    fun `getTagInfo should return Error when not connected`() = runBlocking {
        `when`(mockIsoDep.isConnected).thenReturn(false)
        
        val result = tagConnectionManager.getTagInfo()
        
        assert(result is Result.Error)
        assert((result as Result.Error).message.contains("Not connected"))
    }
    
    @Test
    fun `transceive should return response when connected`() = runBlocking {
        `when`(mockIsoDep.isConnected).thenReturn(true)
        val command = byteArrayOf(0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte())
        val response = byteArrayOf(0x90.toByte(), 0x00.toByte())
        `when`(mockIsoDep.transceive(command)).thenReturn(response)

        val result = tagConnectionManager.transceive(command)

        assert(result is Result.Success)
        if (result is Result.Success) {
            assert(result.data.contentEquals(response))
        }
    }
    
    @Test
    fun `transceive should return Error when not connected`() = runBlocking {
        `when`(mockIsoDep.isConnected).thenReturn(false)
        val command = byteArrayOf(0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte())
        
        val result = tagConnectionManager.transceive(command)
        
        assert(result is Result.Error)
        assert((result as Result.Error).message.contains("Not connected"))
    }
    
    @Test
    fun `isConnected should return true when IsoDep is connected`() {
        `when`(mockIsoDep.isConnected).thenReturn(true)
        
        val isConnected = tagConnectionManager.isConnected()
        
        assert(isConnected)
    }
    
    @Test
    fun `isConnected should return false when IsoDep is not connected`() {
        `when`(mockIsoDep.isConnected).thenReturn(false)
        
        val isConnected = tagConnectionManager.isConnected()
        
        assert(!isConnected)
    }
    
    @Test
    fun `disconnect should close the connection`() {
        `when`(mockIsoDep.isConnected).thenReturn(true)
        var closed = false
        `when`(mockIsoDep.close()).then { closed = true }
        
        tagConnectionManager.disconnect()
        
        assert(closed)
    }
}