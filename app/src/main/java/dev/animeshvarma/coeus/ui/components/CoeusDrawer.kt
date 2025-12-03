package dev.animeshvarma.coeus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.animeshvarma.coeus.model.AppScreen

@Composable
fun CoeusDrawerContent(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Coeus", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // --- TOP SECTION ---
            DrawerItem("Home", Icons.Default.Home, currentScreen == AppScreen.HOME) { onScreenSelected(AppScreen.HOME) }
            DrawerItem("Encrypted", Icons.Default.EnhancedEncryption, currentScreen == AppScreen.ENCRYPTED) { onScreenSelected(AppScreen.ENCRYPTED) }
            DrawerItem("Relay", Icons.Default.Router, currentScreen == AppScreen.RELAY) { onScreenSelected(AppScreen.RELAY) }
            DrawerItem("Command", Icons.Default.Terminal, currentScreen == AppScreen.COMMAND) { onScreenSelected(AppScreen.COMMAND) }

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- BOTTOM SECTION ---
            DrawerItem("Donate", Icons.Default.VolunteerActivism, currentScreen == AppScreen.DONATE) { onScreenSelected(AppScreen.DONATE) }

            // [FIX] Added Docs/Release Notes
            DrawerItem("Docs/Release Notes", Icons.AutoMirrored.Filled.Article, currentScreen == AppScreen.DOCS) { onScreenSelected(AppScreen.DOCS) }

            DrawerItem("Config", Icons.Default.Tune, currentScreen == AppScreen.CONFIG) { onScreenSelected(AppScreen.CONFIG) }
            DrawerItem("Settings", Icons.Default.Settings, currentScreen == AppScreen.SETTINGS) { onScreenSelected(AppScreen.SETTINGS) }
        }
    }
}

// DrawerItem composable remains same...
@Composable
fun DrawerItem(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .bouncyClick(onClick = onClick),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = contentColor)
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = contentColor)
        }
    }
}