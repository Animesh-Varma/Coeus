package dev.animeshvarma.coeus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.animeshvarma.coeus.CoeusViewModel
import dev.animeshvarma.coeus.model.GeneralTab
import dev.animeshvarma.coeus.model.UiState
import dev.animeshvarma.coeus.ui.components.UnderConstruction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralScreen(viewModel: CoeusViewModel, uiState: UiState) {
    Column {
        // TABS
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth(0.9f)) {
                GeneralTab.values().forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = uiState.generalTab == tab,
                        onClick = { viewModel.onTabSelected(tab) },
                        shape = SegmentedButtonDefaults.itemShape(index, GeneralTab.values().size)
                    ) {
                        Text(tab.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // CONTENT
        when (uiState.generalTab) {
            GeneralTab.READ -> ReadScreen(
                isScanning = uiState.isScanning,
                data = uiState.lastScannedTag,
                onReset = { viewModel.clearScan() }
            )
            // Other tabs are under construction for MVP
            GeneralTab.WRITE -> UnderConstruction()
            GeneralTab.OTHER -> UnderConstruction()
        }
    }
}