package dev.animeshvarma.coeus.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.animeshvarma.coeus.CoeusViewModel
import dev.animeshvarma.coeus.model.GeneralTab
import dev.animeshvarma.coeus.model.UiState
import dev.animeshvarma.coeus.ui.components.CoeusSegmentedControl
import dev.animeshvarma.coeus.ui.components.UnderConstruction
import dev.animeshvarma.coeus.ui.theme.AnimationConfig
import dev.animeshvarma.coeus.ui.screens.ReadScreen

@Composable
fun HomeScreen(viewModel: CoeusViewModel, uiState: UiState) {
    Column {
        // TABS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            CoeusSegmentedControl(
                items = GeneralTab.values().map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                selectedIndex = uiState.generalTab.ordinal,
                onItemSelection = { index ->
                    viewModel.onTabSelected(GeneralTab.values()[index])
                },
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // CONTENT SWITCHER
        Box(modifier = Modifier.fillMaxSize()) {
            val slideSpring = spring<IntOffset>(
                stiffness = AnimationConfig.STIFFNESS,
                dampingRatio = AnimationConfig.DAMPING
            )

            AnimatedContent(
                targetState = uiState.generalTab,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        slideInHorizontally(animationSpec = slideSpring) { it } + fadeIn() togetherWith
                                slideOutHorizontally(animationSpec = slideSpring) { -it } + fadeOut()
                    } else {
                        slideInHorizontally(animationSpec = slideSpring) { -it } + fadeIn() togetherWith
                                slideOutHorizontally(animationSpec = slideSpring) { it } + fadeOut()
                    }
                },
                label = "GeneralTabTransition"
            ) { tab ->
                when (tab) {
                    GeneralTab.READ -> ReadScreen(
                        isScanning = uiState.isScanning,
                        data = uiState.lastScannedTag,
                        onReset = { viewModel.clearScan() }
                    )
                    GeneralTab.WRITE -> UnderConstruction()
                    GeneralTab.OTHER -> UnderConstruction()
                }
            }
        }
    }
}