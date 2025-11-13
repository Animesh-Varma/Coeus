package dev.animeshvarma.coeus.presentation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dev.animeshvarma.coeus.R
import dev.animeshvarma.coeus.data.NfcManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * MainActivity implementing NFC foreground dispatch system
 * This activity demonstrates proper NFC tag reading with foreground dispatch mechanism
 */
class MainActivity : AppCompatActivity() {
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var outputTextView: TextView
    private lateinit var clearButton: Button
    private lateinit var vibrator: Vibrator
    private lateinit var nfcManager: NfcManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create UI elements programmatically (simple approach)
        setContentView(android.R.layout.activity_list_item)

        // Create a vertical layout
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        // Create TextView for output
        outputTextView = TextView(this).apply {
            text = getString(R.string.waiting_for_nfc_tag)
            textSize = 16f
            setSingleLine(false)
            setLines(10)
            gravity = android.view.Gravity.CENTER
        }

        // Create Clear button
        clearButton = Button(this).apply {
            text = getString(R.string.clear)
            setOnClickListener {
                outputTextView.text = getString(R.string.waiting_for_nfc_tag)
            }
        }

        // Add views to layout
        layout.addView(outputTextView)
        layout.addView(clearButton)

        // Set the layout as content view
        setContentView(layout)

        // Initialize the NfcManager singleton
        nfcManager = NfcManager.getInstance(this)

        // Initialize NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Initialize vibrator for haptic feedback
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Create pending intent for foreground dispatch
        // Using immutable flag for Android API 31+ compatibility
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Check if NFC is available
        if (nfcAdapter == null) {
            Toast.makeText(this, getString(R.string.nfc_not_supported), Toast.LENGTH_LONG).show()
            return
        }
    }

    /**
     * Enable NFC foreground dispatch when activity is in foreground
     * This allows the activity to receive NFC intents even when another app has focus
     */
    override fun onResume() {
        super.onResume()

        // Check NFC status before enabling foreground dispatch
        when (nfcManager.getNfcStatus()) {
            dev.animeshvarma.coeus.data.NfcAvailabilityStatus.AVAILABLE -> {
                // NFC is available and enabled, proceed with foreground dispatch setup
                enableForegroundDispatch()
            }
            dev.animeshvarma.coeus.data.NfcAvailabilityStatus.DISABLED -> {
                // NFC is available but disabled, show user a message
                updateOutput(getString(R.string.nfc_disabled_message))
                Toast.makeText(this, getString(R.string.please_enable_nfc), Toast.LENGTH_LONG).show()
            }
            dev.animeshvarma.coeus.data.NfcAvailabilityStatus.NOT_SUPPORTED -> {
                // NFC is not supported on this device
                updateOutput(getString(R.string.nfc_not_supported_message))
                Toast.makeText(this, getString(R.string.nfc_not_supported), Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Enable foreground dispatch with appropriate intent filters
     * Sets up filters for all three NFC actions to intercept ALL tag types
     * ACTION_NDEF_DISCOVERED (highest priority), ACTION_TECH_DISCOVERED (medium), ACTION_TAG_DISCOVERED (lowest)
     */
    private fun enableForegroundDispatch() {
        try {
            // Create intent filters for ALL three NFC actions (highest to lowest priority)
            val intentFilters = arrayOf(
                android.content.IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                android.content.IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                android.content.IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            )

            // Specify ALL supported technologies we want to handle
            val techLists = arrayOf(
                arrayOf(
                    "android.nfc.tech.IsoDep",
                    "android.nfc.tech.NfcA",
                    "android.nfc.tech.NfcB",
                    "android.nfc.tech.MifareClassic",
                    "android.nfc.tech.Ndef",
                    "android.nfc.tech.NdefFormatable",
                    "android.nfc.tech.NfcV"
                )
            )

            // Enable foreground dispatch with all filters and tech lists
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists)
        } catch (e: Exception) {
            Timber.e(e, "Error enabling foreground dispatch: ${e.message}")
            Toast.makeText(this, getString(R.string.error_setting_up_nfc, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Disable NFC foreground dispatch when activity is paused
     * Properly manages the lifecycle to avoid conflicts with other apps
     */
    override fun onPause() {
        super.onPause()
        try {
            // Disable foreground dispatch to prevent conflicts
            nfcAdapter.disableForegroundDispatch(this)
        } catch (e: Exception) {
            Timber.e(e, "Error disabling foreground dispatch: ${e.message}")
        }
    }

    /**
     * Called when a new NFC intent is received
     * This is where we handle the NFC tag
     * Process the tag using Kotlin coroutines for async operations
     * Logs which action triggered the intent (NDEF/TECH/TAG)
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Log which action triggered this intent
        when (intent.action) {
            NfcAdapter.ACTION_NDEF_DISCOVERED -> Timber.d("ACTION_NDEF_DISCOVERED triggered")
            NfcAdapter.ACTION_TECH_DISCOVERED -> Timber.d("ACTION_TECH_DISCOVERED triggered")
            NfcAdapter.ACTION_TAG_DISCOVERED -> Timber.d("ACTION_TAG_DISCOVERED triggered")
            else -> Timber.d("Unknown NFC action: ${intent.action}")
        }

        // Launch coroutine to handle NFC tag processing
        CoroutineScope(Dispatchers.Main).launch {
            processNfcTagAsync(intent)
        }
    }

    /**
     * Process NFC tag asynchronously using coroutines
     * Handles the intent, extracts the Tag object, and processes it
     */
    private suspend fun processNfcTagAsync(intent: Intent) = withContext(Dispatchers.IO) {
        try {
            // Provide haptic feedback when tag is detected
            withContext(Dispatchers.Main) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Check if the vibrator has amplitude control for better feedback
                    if (vibrator.hasVibrator()) {
                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                } else {
                    @Suppress("DEPRECATION")
                    if (vibrator.hasVibrator()) {
                        vibrator.vibrate(100)
                    }
                }
            }

            // Extract the tag object from the intent
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as? Tag
            }

            // Null safety check for the tag
            if (tag == null) {
                withContext(Dispatchers.Main) {
                    updateOutput(getString(R.string.no_tag_found))
                }
                return@withContext
            }

            // Process the NFC tag
            val result = processTag(tag)

            // Update UI with the results on the main thread
            withContext(Dispatchers.Main) {
                updateOutput(result)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing NFC tag: ${e.message}")

            // Update UI with error message on the main thread
            withContext(Dispatchers.Main) {
                updateOutput(getString(R.string.error_processing_nfc_tag, e.message))
            }
        }
    }

    /**
     * Process the NFC tag and extract all relevant information
     * @param tag The NFC Tag object to process
     * @return A formatted string with the tag information
     */
    private fun processTag(tag: Tag): String {
        val output = StringBuilder()
        output.append("NFC Tag Detected!\n\n")

        // Extract and format the tag UID
        val uid = tag.id
        val uidFormatted = formatUid(uid)
        output.append("UID: $uidFormatted\n\n")

        // Get and display the tag technologies
        val techList = tag.techList
        output.append("Tag Technologies:\n")
        techList.forEach { tech ->
            output.append("- $tech\n")
        }
        output.append("\n")

        // Process NDEF data if available (this handles formatted tags like vCards)
        if (techList.contains("android.nfc.tech.Ndef")) {
            try {
                val ndef = android.nfc.tech.Ndef.get(tag)
                ndef?.let {
                    it.connect()
                    val ndefMessage = it.cachedNdefMessage
                    if (ndefMessage != null) {
                        output.append("NDEF Content:\n")
                        val records = ndefMessage.records
                        records.forEachIndexed { index, record ->
                            val payload = String(record.payload)
                            output.append("Record $index: Type=${String(record.type)}, Payload=$payload\n")
                        }
                    } else {
                        output.append("No NDEF message found\n")
                    }
                    it.close()
                }
            } catch (e: Exception) {
               Timber.e(e, "Error reading NDEF: ${e.message}")
                output.append("Error reading NDEF: ${e.message}\n")
            }
        }

        // Process IsoDep technology (for direct communication)
        if (techList.contains("android.nfc.tech.IsoDep")) {
            try {
                val tagConnectionManager = dev.animeshvarma.coeus.data.TagConnectionManager(tag)

                // First connect to the tag
                val connectResult = runBlocking(Dispatchers.IO) {
                    tagConnectionManager.connect()
                }

                if (connectResult is dev.animeshvarma.coeus.data.Result.Success) {
                    // Get detailed tag info including ATS
                    val tagInfoResult = runBlocking(Dispatchers.IO) {
                        tagConnectionManager.getTagInfo()
                    }

                    when (tagInfoResult) {
                        is dev.animeshvarma.coeus.data.Result.Success -> {
                            val tagInfo = tagInfoResult.data
                            output.append("Tag Details:\n")
                            output.append("Type: ${tagInfo.tagType}\n")
                            output.append("Historical Bytes: ${tagInfo.historicalBytes ?: "N/A"}\n")
                            output.append("ATS (Answer To Select): ${tagInfo.ats ?: "N/A"}\n\n")
                        }
                        is dev.animeshvarma.coeus.data.Result.Error -> {
                            Timber.e("Error getting tag info: ${tagInfoResult.message}")
                            output.append("Tag Details Error: ${tagInfoResult.message}\n\n")
                        }
                    }

                    // Send a SELECT command (00 A4 04 00) to select the root application
                    val selectCommand = byteArrayOf(
                        0x00.toByte(),  // CLA
                        0xA4.toByte(),  // INS: SELECT
                        0x04.toByte(),  // P1: select by name
                        0x00.toByte()   // P2: first or only occurrence
                    )

                    output.append("IsoDep Communication:\n")
                    output.append("Sending SELECT command: ${formatBytes(selectCommand)}\n")

                    val transceiveResult = runBlocking(Dispatchers.IO) {
                        tagConnectionManager.transceive(selectCommand)
                    }

                    when (transceiveResult) {
                        is dev.animeshvarma.coeus.data.Result.Success -> {
                            output.append("Response: ${formatBytes(transceiveResult.data)}\n")
                        }
                        is dev.animeshvarma.coeus.data.Result.Error -> {
                            output.append("Transceive Error: ${transceiveResult.message}\n")
                        }
                    }
                } else {
                    // If connection failed, provide error message
                    val errorMessage = when (connectResult) {
                        is dev.animeshvarma.coeus.data.Result.Error -> connectResult.message
                        else -> "Unknown connection error"
                    }
                    output.append("Failed to connect to tag: $errorMessage\n")
                }

                tagConnectionManager.close()
            } catch (e: Exception) {
                Timber.e(e, "Error with IsoDep communication: ${e.message}")
                output.append("Error with IsoDep communication: ${e.message}\n")
            }
        }

        return output.toString()
    }

    /**
     * Format the UID as a hex string with colons between bytes (e.g., "04:A1:B2:C3")
     */
    private fun formatUid(uid: ByteArray): String {
        return uid.joinToString(separator = ":") { String.format("%02X", it) }
    }

    /**
     * Format a byte array as a hex string
     */
    private fun formatBytes(bytes: ByteArray?): String {
        if (bytes == null) return "null"
        return bytes.joinToString(separator = " ") { String.format("%02X", it) }
    }

    /**
     * Update the output TextView on the UI thread
     */
    private fun updateOutput(text: String) {
        runOnUiThread {
            outputTextView.text = text
        }
    }
}