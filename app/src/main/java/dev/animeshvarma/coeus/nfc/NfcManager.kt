package dev.animeshvarma.coeus.nfc

import android.content.Context
import android.nfc.NfcAdapter
import timber.log.Timber

enum class NfcAvailabilityStatus {
    AVAILABLE,
    DISABLED,
    NOT_SUPPORTED
}

class NfcManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: NfcManager? = null

        fun getInstance(context: Context): NfcManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NfcManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getNfcAdapter(): NfcAdapter? {
        return try {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
            if (nfcAdapter == null) {
                Timber.w("NFC adapter is not available on this device")
            }
            nfcAdapter
        } catch (e: Exception) {
            Timber.e(e, "Error getting NFC adapter")
            null
        }
    }

    fun isNfcAvailable(): Boolean {
        return getNfcAdapter() != null
    }

    fun isNfcEnabled(): Boolean {
        return getNfcAdapter()?.isEnabled == true
    }

    fun getNfcStatus(): NfcAvailabilityStatus {
        return try {
            if (!isNfcAvailable()) {
                NfcAvailabilityStatus.NOT_SUPPORTED
            } else if (!isNfcEnabled()) {
                NfcAvailabilityStatus.DISABLED
            } else {
                NfcAvailabilityStatus.AVAILABLE
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting NFC status")
            NfcAvailabilityStatus.NOT_SUPPORTED
        }
    }
}