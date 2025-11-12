package dev.animeshvarma.coeus.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.animeshvarma.coeus.domain.SendApduUseCase
import kotlinx.coroutines.launch

class MainViewModel(private val sendApduUseCase: SendApduUseCase) : ViewModel() {
    var apduCommand = mutableStateOf("")
        private set
    var apduResponse = mutableStateOf("")
        private set
    var isProcessing = mutableStateOf(false)
        private set
    var errorMessage = mutableStateOf("")
        private set

    fun sendApduCommand() {
        if (apduCommand.value.isEmpty()) return

        viewModelScope.launch {
            isProcessing.value = true
            errorMessage.value = ""
            
            sendApduUseCase(apduCommand.value)
                .onSuccess { response ->
                    apduResponse.value = response
                }
                .onFailure { error ->
                    errorMessage.value = error.message ?: "An error occurred"
                }
            
            isProcessing.value = false
        }
    }

    fun updateApduCommand(newCommand: String) {
        apduCommand.value = newCommand
    }
}