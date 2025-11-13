package dev.animeshvarma.coeus.data

import android.content.Context
import android.nfc.NfcAdapter
import timber.log.Timber

/**
 * Enum representing the possible NFC availability states
 */
enum class NfcAvailabilityStatus {
    /** NFC hardware is available and enabled */
    AVAILABLE,

    /** NFC hardware is available but disabled */
    DISABLED,

    /** NFC hardware is not supported on the device */
    NOT_SUPPORTED
}

/**
 * Singleton class responsible for managing NFC operations in the application.
 * Provides methods to check NFC availability, enable state, and get the NfcAdapter instance.
 *
 * This class follows clean architecture principles by separating concerns.
 *
 * @property context The application context used to access system services
 */
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

    /**
     * Gets the NfcAdapter instance for the application
     *
     * @return The NfcAdapter instance or null if the device doesn't support NFC
     */
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

    /**
     * Checks if the device has NFC hardware available
     *
     * @return true if NFC hardware is available, false otherwise
     */
    fun isNfcAvailable(): Boolean {
        return try {
            val nfcAdapter = getNfcAdapter()
            val hasNfc = nfcAdapter != null
            Timber.d("NFC availability: ${if (hasNfc) "available" else "not available"}")
            hasNfc
        } catch (e: Exception) {
            Timber.e(e, "Error checking NFC availability")
            false
        }
    }

    /**
     * Checks if NFC is currently enabled on the device
     *
     * @return true if NFC is available and enabled, false otherwise
     */
    fun isNfcEnabled(): Boolean {
        return try {
            val nfcAdapter = getNfcAdapter()
            val isEnabled = nfcAdapter?.isEnabled == true
            Timber.d("NFC enabled state: ${if (isEnabled) "enabled" else "disabled"}")
            isEnabled
        } catch (e: Exception) {
            Timber.e(e, "Error checking NFC enabled state")
            false
        }
    }

    /**
     * Provides the current NFC availability status as an enum
     *
     * @return NfcAvailabilityStatus enum indicating the current NFC state:
     *         - AVAILABLE: NFC hardware is available and enabled
     *         - DISABLED: NFC hardware is available but disabled
     *         - NOT_SUPPORTED: NFC hardware is not supported on the device
     */
    fun getNfcStatus(): NfcAvailabilityStatus {
        return try {
            if (!isNfcAvailable()) {
                Timber.i("NFC: Not supported")
                NfcAvailabilityStatus.NOT_SUPPORTED
            } else if (!isNfcEnabled()) {
                Timber.i("NFC: Available but disabled")
                NfcAvailabilityStatus.DISABLED
            } else {
                Timber.i("NFC: Available and enabled")
                NfcAvailabilityStatus.AVAILABLE
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting NFC status")
            NfcAvailabilityStatus.NOT_SUPPORTED // Default to not supported on error
        }
    }

    /**
     * Helper method to get a human-readable description of the NFC status
     *
     * @return A string describing the current NFC status
     */
    fun getNfcStatusDescription(): String {
        return when (getNfcStatus()) {
            NfcAvailabilityStatus.AVAILABLE -> "NFC is available and enabled"
            NfcAvailabilityStatus.DISABLED -> "NFC is available but disabled, please enable it in settings"
            NfcAvailabilityStatus.NOT_SUPPORTED -> "NFC is not supported on this device"
        }
    }
}