package dev.animeshvarma.coeus.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.animeshvarma.coeus.model.NfcCardData
import dev.animeshvarma.coeus.ui.components.bouncyClick
import dev.animeshvarma.coeus.ui.theme.AnimationConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Contactless,
                        contentDescription = "Scanning",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                PulsingDotsText(
                    baseText = "Waiting for tag",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Hold device near NFC tag",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (data != null) {
            // RESULT STATE
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.CenterEnd) {
                    IconButton(onClick = onReset, modifier = Modifier.bouncyClick(onReset)) {
                        Icon(Icons.Default.Refresh, "Scan Again")
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // UID
                    item { AnimatedCard(0) { ResultCard("UID", data.uid, Icons.Default.Fingerprint, isCode = true) } }

                    // Tech List
                    item { AnimatedCard(1) { ResultCard("Technologies", data.techList.joinToString("\n"), Icons.Default.Nfc) } }

                    // Details (Mapped)
                    val entries = data.details.entries.toList()
                    itemsIndexed(entries) { index, entry ->
                        // Offset index by 2 because of the first two hardcoded cards
                        AnimatedCard(index + 2) {
                            ResultCard(entry.key, entry.value, Icons.Default.Info, isCode = true)
                        }
                    }
                }
            }
        }
    }
}

// [NEW] Text Animation Composable
@Composable
fun PulsingDotsText(
    baseText: String,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color
) {
    var dots by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            dots = ""
            delay(400)
            dots = "."
            delay(400)
            dots = ".."
            delay(400)
            dots = "..."
            delay(400)
        }
    }

    // Using a Box to prevent layout jitter when text grows
    Box(contentAlignment = Alignment.CenterStart) {
        // Invisible text to reserve max width space
        Text(text = "$baseText...", style = style, color = androidx.compose.ui.graphics.Color.Transparent)
        // Visible animated text
        Text(text = "$baseText$dots", style = style, color = color)
    }
}

@Composable
fun AnimatedCard(index: Int, content: @Composable () -> Unit) {
    val scale = remember { Animatable(0.9f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val delayMs = (index * 30L).coerceAtMost(300L)

        delay(delayMs)

        launch {
            scale.animateTo(
                1f,
                spring(dampingRatio = 0.7f, stiffness = AnimationConfig.STIFFNESS)
            )
        }
        launch {
            alpha.animateTo(1f, spring(stiffness = 1000f))
        }
    }

    Box(modifier = Modifier.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        this.alpha = alpha.value
    }) {
        content()
    }
}

@Composable
fun ResultCard(title: String, content: String, icon: ImageVector? = null, isCode: Boolean = false) {
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = if(isCode) MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}