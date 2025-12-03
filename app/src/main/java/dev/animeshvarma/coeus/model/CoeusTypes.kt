package dev.animeshvarma.coeus.model

// --- Navigation Enums ---
enum class AppScreen(val title: String) {
    GENERAL("General"),
    ENCRYPTED("Encrypted"),
    RELAY("Relay"),
    COMMAND("Command"),
    DONATE("Donate"),
    CONFIG("Config"),
    SETTINGS("Settings")
}

enum class GeneralTab {
    READ, WRITE, OTHER
}

// --- Data Models ---
data class NfcCardData(
    val uid: String,
    val techList: List<String>,
    val details: Map<String, String>,
    val timestamp: Long = System.currentTimeMillis()
)

// --- State Management ---
data class UiState(
    val currentScreen: AppScreen = AppScreen.GENERAL,
    val generalTab: GeneralTab = GeneralTab.READ,

    // NFC State
    val isScanning: Boolean = true,
    val lastScannedTag: NfcCardData? = null,
    val error: String? = null,

    // Logs/Debug
    val logs: List<String> = emptyList(),
    val showLogsDialog: Boolean = false
)