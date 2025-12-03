package dev.animeshvarma.coeus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.animeshvarma.coeus.CoeusViewModel
import dev.animeshvarma.coeus.model.AppScreen
import dev.animeshvarma.coeus.ui.components.CoeusDrawerContent
import dev.animeshvarma.coeus.ui.components.UnderConstruction
import dev.animeshvarma.coeus.ui.screens.GeneralScreen
import kotlinx.coroutines.launch

@Composable
fun CoeusApp(
    modifier: Modifier = Modifier,
    viewModel: CoeusViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            CoeusDrawerContent(uiState.currentScreen) {
                viewModel.onScreenSelected(it)
                scope.launch { drawerState.close() }
            }
        }
    ) {
        // [FIX] Apply modifier here to handle system bars
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // HEADER
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.Default.Menu, "Menu")
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.currentScreen.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.width(32.dp).height(2.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
            }

            // CONTENT
            if (uiState.currentScreen == AppScreen.GENERAL) {
                GeneralScreen(viewModel, uiState)
            } else {
                UnderConstruction()
            }
        }
    }
}