package dev.animeshvarma.coeus

import android.nfc.Tag
import android.nfc.tech.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.animeshvarma.coeus.model.AppScreen
import dev.animeshvarma.coeus.model.GeneralTab
import dev.animeshvarma.coeus.model.NfcCardData
import dev.animeshvarma.coeus.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class CoeusViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun onScreenSelected(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun onTabSelected(tab: GeneralTab) {
        _uiState.update { it.copy(generalTab = tab) }
    }

    fun clearScan() {
        _uiState.update { it.copy(isScanning = true, lastScannedTag = null, error = null) }
    }

    fun onTagDiscovered(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isScanning = true) }

                val uid = tag.id.joinToString(":") { "%02X".format(it) }
                val techs = tag.techList.map { it.substringAfterLast(".") }
                val details = mutableMapOf<String, String>()

                // [NEW] Detailed Extraction Logic

                // 1. Low Level Info (NfcA)
                if (techs.contains("NfcA")) {
                    val nfcA = NfcA.get(tag)
                    details["ATQA"] = nfcA.atqa.joinToString("") { "%02X".format(it) }
                    details["SAK"] = "%02X".format(nfcA.sak)
                    details["Max Transceive"] = "${nfcA.maxTransceiveLength} bytes"
                }

                // 2. Mifare Classic
                if (techs.contains("MifareClassic")) {
                    val mifare = MifareClassic.get(tag)
                    val typeStr = when(mifare.type) {
                        MifareClassic.TYPE_CLASSIC -> "Classic"
                        MifareClassic.TYPE_PLUS -> "Plus"
                        MifareClassic.TYPE_PRO -> "Pro"
                        else -> "Unknown"
                    }
                    details["Mifare Type"] = typeStr
                    details["Size"] = "${mifare.size} bytes"
                    details["Sectors"] = "${mifare.sectorCount}"
                    details["Blocks"] = "${mifare.blockCount}"
                }

                // 3. ISO-DEP (Cards/Passports)
                if (techs.contains("IsoDep")) {
                    val isoDep = IsoDep.get(tag)
                    isoDep?.use {
                        it.connect()
                        details["Standard"] = "ISO 14443-4"
                        it.historicalBytes?.let { hb ->
                            details["Historical Bytes"] = hb.joinToString(" ") { b -> "%02X".format(b) }
                        }
                    }
                }

                // 4. NDEF
                if (techs.contains("Ndef")) {
                    val ndef = Ndef.get(tag)
                    ndef?.use {
                        it.connect()
                        details["NDEF Type"] = it.type
                        details["Writable"] = if(it.isWritable) "Yes" else "No (Read-only)"
                        details["Capacity"] = "${it.maxSize} bytes"
                    }
                }

                val cardData = NfcCardData(uid, techs, details)

                _uiState.update {
                    it.copy(
                        isScanning = false,
                        lastScannedTag = cardData,
                        error = null
                    )
                }

            } catch (e: Exception) {
                Timber.e(e, "Tag processing failed")
                _uiState.update { it.copy(error = "Read Failed: ${e.message}", isScanning = false) }
            }
        }
    }
}