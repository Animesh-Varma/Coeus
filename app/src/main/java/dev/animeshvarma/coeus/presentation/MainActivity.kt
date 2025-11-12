package dev.animeshvarma.coeus.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Arrays

/**
 * Simple NFC Tag Reading Demo
 * This activity demonstrates reading NFC tags with various technologies
 */
class MainActivity : AppCompatActivity() {
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var outputTextView: TextView
    private lateinit var clearButton: Button
    private lateinit var vibrator: Vibrator

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
            text = "Waiting for NFC tag..."
            textSize = 16f
            setSingleLine(false)
            setLines(10)
            gravity = android.view.Gravity.CENTER
        }
        
        // Create Clear button
        clearButton = Button(this).apply {
            text = "Clear"
            setOnClickListener {
                outputTextView.text = "Waiting for NFC tag..."
            }
        }
        
        // Add views to layout
        layout.addView(outputTextView)
        layout.addView(clearButton)
        
        // Set the layout as content view
        setContentView(layout)

        // Initialize NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        // Initialize vibrator for haptic feedback
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        
        // Check if NFC is available
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not supported on this device", Toast.LENGTH_LONG).show()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Enable NFC foreground dispatch to receive tag intents
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent, 
            android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        // Disable NFC foreground dispatch when activity is paused
        nfcAdapter.disableForegroundDispatch(this)
    }

    /**
     * Called when a new NFC intent is received
     * This is where we handle the NFC tag
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        
        try {
            // Provide haptic feedback when tag is detected
            if (vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(100)
                }
            }

            // Check if the intent contains NFC tag data
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag == null) {
                updateOutput("No tag found in intent")
                return
            }

            // Process the NFC tag
            processNfcTag(tag)

        } catch (e: Exception) {
            Log.e("NFC", "Error processing NFC tag: ${e.message}", e)
            updateOutput("Error processing NFC tag: ${e.message}")
        }
    }

    /**
     * Process the NFC tag and extract all relevant information
     */
    private fun processNfcTag(tag: Tag) {
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

        // Check and read NDEF data if available
        if (techList.contains("android.nfc.tech.Ndef")) {
            try {
                val ndef = Ndef.get(tag)
                ndef?.let { 
                    it.connect()
                    val ndefMessage = it.cachedNdefMessage
                    if (ndefMessage != null) {
                        output.append("NDEF Content:\n")
                        val records = ndefMessage.records
                        records.forEachIndexed { index, record ->
                            val payload = String(record.payload)
                            val type = String(record.type)
                            output.append("Record $index: Type=${record.type}, Payload=$payload\n")
                        }
                    } else {
                        output.append("No NDEF message found\n")
                    }
                    it.close()
                }
            } catch (e: Exception) {
                Log.e("NFC", "Error reading NDEF: ${e.message}", e)
                output.append("Error reading NDEF: ${e.message}\n")
            }
        }

        // Check and communicate with IsoDep if available
        if (techList.contains("android.nfc.tech.IsoDep")) {
            try {
                val isoDep = IsoDep.get(tag)
                isoDep?.let {
                    it.connect()
                    
                    // Send a SELECT command (00 A4 04 00) to select the root application
                    val selectCommand = byteArrayOf(
                        0x00.toByte(),  // CLA
                        0xA4.toByte(),  // INS: SELECT
                        0x04.toByte(),  // P1: select by name
                        0x00.toByte()   // P2: first or only occurrence
                    )
                    
                    output.append("\nIsoDep Communication:\n")
                    output.append("Sending SELECT command: ${formatBytes(selectCommand)}\n")
                    
                    val response = it.transceive(selectCommand)
                    output.append("Response: ${formatBytes(response)}\n")
                    
                    it.close()
                }
            } catch (e: Exception) {
                Log.e("NFC", "Error with IsoDep communication: ${e.message}", e)
                output.append("Error with IsoDep communication: ${e.message}\n")
            }
        }

        // Update the UI with the output
        updateOutput(output.toString())
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