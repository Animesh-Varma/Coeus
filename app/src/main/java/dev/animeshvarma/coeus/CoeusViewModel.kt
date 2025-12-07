package dev.animeshvarma.coeus

import android.nfc.Tag
import android.nfc.tech.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.animeshvarma.coeus.model.AppScreen
import dev.animeshvarma.coeus.model.GeneralTab
import dev.animeshvarma.coeus.model.NdefRecordInfo
import dev.animeshvarma.coeus.model.NfcCardData
import dev.animeshvarma.coeus.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.charset.Charset
import java.util.Arrays

class CoeusViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState
    private var apduLog = StringBuilder()

    fun onScreenSelected(screen: AppScreen) { _uiState.update { it.copy(currentScreen = screen) } }
    fun onTabSelected(tab: GeneralTab) { _uiState.update { it.copy(generalTab = tab) } }
    fun clearScan() { _uiState.update { it.copy(isScanning = true, lastScannedTag = null, error = null) } }

    fun onTagDiscovered(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isScanning = true) }
                apduLog.setLength(0)

                val uid = tag.id.joinToString(":") { "%02X".format(it) }
                val techs = tag.techList.map { it.substringAfterLast(".") }
                val details = LinkedHashMap<String, String>()
                val ndefRecords = mutableListOf<NdefRecordInfo>()

                if (techs.contains("IsoDep")) {
                    val isoDep = IsoDep.get(tag)
                    isoDep?.use {
                        it.connect()
                        it.timeout = 5000
                        details["Standard"] = "ISO 14443-4"

                        try {
                            val desfireData = readDesfireLikeLog(it)
                            details.putAll(desfireData)
                        } catch (e: Exception) {
                            details["DESFire Error"] = "${e.message}"
                            Timber.e(e)
                        }

                        if (apduLog.isNotEmpty()) details["Comm Log"] = apduLog.toString()

                        if (!details.containsKey("Card Type")) {
                            it.historicalBytes?.let { hb -> details["Historical Bytes"] = hb.toHex() }
                        }
                    }
                }

                if (techs.contains("Ndef")) {
                    val ndef = Ndef.get(tag)
                    ndef?.use {
                        it.connect()
                        details["NDEF Type"] = it.type
                        details["Writable"] = if(it.isWritable) "Yes" else "No"
                        details["Capacity"] = "${it.maxSize} bytes"
                        it.ndefMessage?.records?.forEachIndexed { index, record ->
                            val payload = tryParseRecord(record)
                            ndefRecords.add(NdefRecordInfo("Record ${index + 1}", payload, record.payload))
                        }
                    }
                }

                val cardData = NfcCardData(uid, techs, details, ndefRecords)
                _uiState.update { it.copy(isScanning = false, lastScannedTag = cardData, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Read Error: ${e.message}", isScanning = false) }
            }
        }
    }

    // ===========================================================================
    // DESFire SEQUENCE (Matching GitHub Logs)
    // ===========================================================================

    private fun readDesfireLikeLog(isoDep: IsoDep): Map<String, String> {
        val info = LinkedHashMap<String, String>()

        // 1. GET VERSION FLUSH (Essential step to clean state)
        // Command: 90 60 00 00 00
        log(">> GET VERSION (Flush)")
        var res = transceiveLog(isoDep, byteArrayOf(0x90.toByte(), 0x60, 0x00, 0x00, 0x00))

        if (res.isEmpty()) return info // Not a DESFire response

        // Loop while status is 91 AF (More Data)
        while (res.size >= 2 && res[res.size - 2] == 0x91.toByte() && res.last() == 0xAF.toByte()) {
            // Send Continue: 90 AF 00 00 00
            res = transceiveLog(isoDep, byteArrayOf(0x90.toByte(), 0xAF.toByte(), 0x00, 0x00, 0x00))
        }

        // Check if the final response ended in 91 00 (Success)
        if (res.size >= 2 && res[res.size - 2] == 0x91.toByte() && res.last() == 0x00.toByte()) {
            info["Card Type"] = "MIFARE DESFire EVx"

            // 2. SELECT ROOT (00 00 00)
            // Command: 90 5A 00 00 03 00 00 00 00
            log(">> SELECT ROOT (000000)")
            val cmdSelect = byteArrayOf(
                0x90.toByte(), 0x5A, 0x00, 0x00, 0x03, // Header + Length
                0x00, 0x00, 0x00, // Data (AID)
                0x00 // Le
            )
            res = transceiveLog(isoDep, cmdSelect)

            if (checkSuccess(res)) {
                info["Selected"] = "Root Application"

                // 3. GET APPLICATIONS
                // Command: 90 6A 00 00 00
                log(">> GET APPLICATIONS")
                val cmdGetApps = byteArrayOf(0x90.toByte(), 0x6A, 0x00, 0x00, 0x00)
                res = transceiveLog(isoDep, cmdGetApps)

                val appBytes = mutableListOf<Byte>()

                // Handle Chained Response (91 AF)
                while (res.size >= 2 && res[res.size - 2] == 0x91.toByte() && res.last() == 0xAF.toByte()) {
                    appBytes.addAll(res.copyOfRange(0, res.size - 2).toList())
                    // Continue
                    res = transceiveLog(isoDep, byteArrayOf(0x90.toByte(), 0xAF.toByte(), 0x00, 0x00, 0x00))
                }

                if (checkSuccess(res)) {
                    appBytes.addAll(res.copyOfRange(0, res.size - 2).toList())

                    if (appBytes.isEmpty()) {
                        info["Applications"] = "None (Empty)"
                    } else {
                        // Parse 3-byte AIDs (Little Endian)
                        val appList = mutableListOf<String>()
                        for (i in appBytes.indices step 3) {
                            if (i + 2 < appBytes.size) {
                                // Hex format: 01 02 03 -> 010203
                                val aid = "%02X%02X%02X".format(appBytes[i], appBytes[i+1], appBytes[i+2])
                                appList.add(aid)
                            }
                        }
                        info["Applications"] = appList.joinToString(", ")
                    }
                    info["Auth Status"] = "Public (No Auth)"
                } else {
                    info["Auth Status"] = "Access Denied (Requires Key)"
                }
            } else {
                info["Select Root"] = "Failed"
            }
        } else {
            info["Card Type"] = "Unknown (ISO-DEP)"
        }

        return info
    }

    // --- UTILS ---
    private fun transceiveLog(iso: IsoDep, cmd: ByteArray): ByteArray {
        log("TX: ${cmd.toHex()}")
        return try {
            val res = iso.transceive(cmd)
            log("RX: ${res.toHex()}")
            res
        } catch (e: Exception) {
            log("RX Error: ${e.message}")
            byteArrayOf()
        }
    }

    private fun checkSuccess(res: ByteArray): Boolean {
        // Expecting ISO Wrapped Success: 91 00
        return res.size >= 2 && res[res.size - 2] == 0x91.toByte() && res.last() == 0x00.toByte()
    }

    private fun log(msg: String) {
        Timber.d(msg)
        apduLog.append("$msg\n")
    }

    private fun tryParseRecord(record: android.nfc.NdefRecord): String {
        try {
            val tnf = record.tnf
            if (tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.type, android.nfc.NdefRecord.RTD_TEXT)) {
                val payload = record.payload
                val encoding = if ((payload[0].toInt() and 128) == 0) "UTF-8" else "UTF-16"
                val len = payload[0].toInt() and 63
                return String(payload, len + 1, payload.size - len - 1, Charset.forName(encoding))
            }
        } catch (e: Exception) {}

        val raw = record.payload
        val printable = raw.count { it in 32..126 }
        return if (raw.isNotEmpty() && printable.toDouble() / raw.size > 0.8) String(raw, Charset.forName("UTF-8")) else "Binary (${raw.size} B)"
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }
}