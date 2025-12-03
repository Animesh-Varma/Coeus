package dev.animeshvarma.coeus.model

enum class AppScreen(val title: String) {
    HOME("Home"),
    ENCRYPTED("Encrypted"),
    RELAY("Relay"),
    COMMAND("Command"),
    DONATE("Donate"),

    // [FIX] Added missing DOCS entry
    DOCS("Docs/Release Notes"),

    CONFIG("Config"),
    SETTINGS("Settings")
}

enum class GeneralTab {
    READ, WRITE, OTHER
}

data class NfcCardData(
    val uid: String,
    val techList: List<String>,
    val details: Map<String, String>,
    val timestamp: Long = System.currentTimeMillis()
)

data class UiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val generalTab: GeneralTab = GeneralTab.READ,
    val isScanning: Boolean = true,
    val lastScannedTag: NfcCardData? = null,
    val error: String? = null
)