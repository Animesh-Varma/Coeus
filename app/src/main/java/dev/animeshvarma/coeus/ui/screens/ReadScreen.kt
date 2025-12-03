package dev.animeshvarma.coeus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.animeshvarma.coeus.model.NfcCardData

@Composable
fun ReadScreen(
    isScanning: Boolean,
    data: NfcCardData?,
    onReset: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isScanning) {
            // WAITING STATE
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // You can add Pulse Animation here later if desired
                    Icon(
                        imageVector = Icons.Default.Contactless,
                        contentDescription = "Scanning",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Waiting for tag...", style = MaterialTheme.typography.titleMedium)
                Text("Hold device near NFC tag", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (data != null) {
            // RESULT STATE
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.CenterEnd) {
                    IconButton(onClick = onReset) { Icon(Icons.Default.Refresh, "Scan Again") }
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { ResultCard("UID", data.uid, Icons.Default.Nfc) }
                    item { ResultCard("Technologies", data.techList.joinToString(", "), Icons.Default.Contactless) }
                    data.details.forEach { (key, value) ->
                        item { ResultCard(key, value) }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultCard(title: String, content: String, icon: ImageVector? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}