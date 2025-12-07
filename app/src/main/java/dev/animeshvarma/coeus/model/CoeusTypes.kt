package dev.animeshvarma.coeus.model

enum class AppScreen(val title: String) {
    HOME("Home"),
    ENCRYPTED("Encrypted"),
    RELAY("Relay"),
    COMMAND("Command"),
    DONATE("Donate"),
    DOCS("Docs/Release Notes"),
    CONFIG("Config"),
    SETTINGS("Settings")
}

enum class GeneralTab {
    READ, WRITE, OTHER
}

// [FIX] New class to hold detailed record info
data class NdefRecordInfo(
    val label: String,
    val parsedContent: String,
    val rawBytes: ByteArray
) {
    // Auto-generated equals/hashCode for ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NdefRecordInfo
        return rawBytes.contentEquals(other.rawBytes)
    }
    override fun hashCode(): Int = rawBytes.contentHashCode()
}

data class NfcCardData(
    val uid: String,
    val techList: List<String>,
    val details: Map<String, String>,
    val ndefRecords: List<NdefRecordInfo> = emptyList(), // [FIX] Added list for records
    val timestamp: Long = System.currentTimeMillis()
)

data class UiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val generalTab: GeneralTab = GeneralTab.READ,
    val isScanning: Boolean = true,
    val lastScannedTag: NfcCardData? = null,
    val error: String? = null
)