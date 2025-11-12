package dev.animeshvarma.coeus.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainContent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Coeus - NFC APDU Tool",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        OutlinedTextField(
            value = viewModel.apduCommand.value,
            onValueChange = { viewModel.updateApduCommand(it) },
            label = { Text("APDU Command") },
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("Enter hex command (e.g., 00A404000A)") }
        )
        
        Button(
            onClick = { viewModel.sendApduCommand() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isProcessing.value
        ) {
            if (viewModel.isProcessing.value) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Send Command")
            }
        }
        
        if (viewModel.errorMessage.value.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Error: ${viewModel.errorMessage.value}",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        if (viewModel.apduResponse.value.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Response: ${viewModel.apduResponse.value}",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}