package dev.animeshvarma.coeus.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable // Fixes rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.animeshvarma.coeus.model.NdefRecordInfo
import dev.animeshvarma.coeus.model.NfcCardData
import dev.animeshvarma.coeus.ui.components.bouncyClick
import dev.animeshvarma.coeus.ui.theme.AnimationConfig
import kotlinx.coroutines.delay // Fixes delay error
import kotlinx.coroutines.launch // Fixes launch error

@Composable
fun ReadScreen(
    isScanning: Boolean,
    data: NfcCardData?,
    onReset: () -> Unit
) {
    var selectedRecord by remember { mutableStateOf<NdefRecordInfo?>(null) }

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
                    item { AnimatedCard(0, "uid") { ResultCard("UID", data.uid, Icons.Default.Fingerprint, isCode = true) } }

                    // Tech List
                    item { AnimatedCard(1, "tech") { ResultCard("Technologies", data.techList.joinToString("\n"), Icons.Default.Nfc) } }

                    // Generic Details
                    val entries = data.details.entries.toList()
                    itemsIndexed(entries) { index, entry ->
                        AnimatedCard(index + 2, "detail_${entry.key}") {
                            ResultCard(entry.key, entry.value, Icons.Default.Info, isCode = true)
                        }
                    }

                    // NDEF Records (Clickable)
                    itemsIndexed(data.ndefRecords) { index, record ->
                        AnimatedCard(index + 2 + entries.size, "ndef_$index") {
                            ResultCard(
                                title = record.label,
                                content = record.parsedContent,
                                icon = Icons.Default.Description,
                                onClick = { selectedRecord = record }
                            )
                        }
                    }
                }
            }
        }
    }

    // Raw Data Dialog
    if (selectedRecord != null) {
        RawDataDialog(record = selectedRecord!!) {
            selectedRecord = null
        }
    }
}

@Composable
fun RawDataDialog(record: NdefRecordInfo, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Raw Data View",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Hex Dump
                Text("HEX:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = record.rawBytes.joinToString(" ") { "%02X".format(it) },
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ASCII Dump
                Text("ASCII:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                val ascii = record.rawBytes.map { if (it in 32..126) it.toChar() else '.' }.joinToString("")
                Text(
                    text = ascii,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun ResultCard(
    title: String,
    content: String,
    icon: ImageVector? = null,
    isCode: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().then(
            if(onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
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

@Composable
fun PulsingDotsText(
    baseText: String,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color
) {
    var dots by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            dots = ""; delay(400)
            dots = "."; delay(400)
            dots = ".."; delay(400)
            dots = "..."; delay(400)
        }
    }
    Box(contentAlignment = Alignment.CenterStart) {
        Text(text = "$baseText...", style = style, color = androidx.compose.ui.graphics.Color.Transparent)
        Text(text = "$baseText$dots", style = style, color = color)
    }
}

@Composable
fun AnimatedCard(index: Int, uniqueKey: String, content: @Composable () -> Unit) {
    var hasAnimated by rememberSaveable(uniqueKey) { mutableStateOf(false) }

    val scale = remember { Animatable(if (hasAnimated) 1f else 0.9f) }
    val alpha = remember { Animatable(if (hasAnimated) 1f else 0f) }

    LaunchedEffect(uniqueKey) {
        if (!hasAnimated) {
            // Ordered Cascade (Max 300ms delay)
            val delayMs = (index * 30L).coerceAtMost(300L)
            delay(delayMs)

            launch { scale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 800f)) }
            launch { alpha.animateTo(1f, spring(stiffness = 1000f)) }
            hasAnimated = true
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