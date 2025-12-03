package dev.animeshvarma.coeus

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
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

    // --- NFC LOGIC ---
    fun onTagDiscovered(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Reset state
                _uiState.update { it.copy(isScanning = true) }

                val uid = tag.id.joinToString(":") { "%02X".format(it) }
                val techs = tag.techList.map { it.substringAfterLast(".") }
                val details = mutableMapOf<String, String>()

                // 1. ISO-DEP (Type 4 / Credit Cards)
                if (techs.contains("IsoDep")) {
                    val isoDep = IsoDep.get(tag)
                    isoDep?.use {
                        it.connect()
                        details["Standard"] = "ISO 14443-4"
                        details["Max Transceive"] = "${it.maxTransceiveLength} bytes"
                        it.historicalBytes?.let { hb ->
                            details["Historical Bytes"] = hb.joinToString(" ") { b -> "%02X".format(b) }
                        }
                    }
                }

                // 2. NDEF (Formatted Data)
                if (techs.contains("Ndef")) {
                    val ndef = Ndef.get(tag)
                    ndef?.use {
                        it.connect()
                        details["NDEF Type"] = it.type
                        details["Max Size"] = "${it.maxSize} bytes"
                        details["Writable"] = it.isWritable.toString()
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