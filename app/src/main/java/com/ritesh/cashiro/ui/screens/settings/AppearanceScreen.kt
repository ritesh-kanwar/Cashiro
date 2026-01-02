package com.ritesh.cashiro.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MotionPhotosAuto
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.presentation.categories.NavigationContent
import com.ritesh.cashiro.ui.components.CashiroCard
import com.ritesh.cashiro.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.ui.components.PreferenceSwitch
import com.ritesh.cashiro.ui.components.SectionHeader
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.theme.Dimensions
import com.ritesh.cashiro.ui.theme.Spacing
import com.ritesh.cashiro.ui.viewmodel.ThemeViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceScreen(
    onNavigateBack: () -> Unit,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = "Appearance",
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                onBackClick = onNavigateBack,
                navigationContent = { NavigationContent(onNavigateBack) }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .overScrollVertical()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Dimensions.Padding.content + paddingValues.calculateTopPadding()
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                
                // Theme Mode Section
                SectionHeader(
                    title = "Theme Mode",
                    modifier = Modifier.padding(start = Spacing.md)
                )

                CashiroCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(Dimensions.Radius.lg))
                                    .background(
                                        color = if(themeUiState.isDarkTheme == null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(Dimensions.Radius.lg)
                                    )
                                    .clickable{
                                        themeViewModel.updateDarkTheme(null)
                                    },
                                contentAlignment = Alignment.Center
                            ){
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MotionPhotosAuto,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "System",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(Dimensions.Radius.lg))
                                    .background(
                                        color = if(themeUiState.isDarkTheme == false) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(Dimensions.Radius.lg)
                                    )
                                    .clickable{
                                        themeViewModel.updateDarkTheme(false)
                                    },
                                contentAlignment = Alignment.Center
                            ){
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.LightMode,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "Light",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(Dimensions.Radius.lg))
                                    .background(
                                        color = if(themeUiState.isDarkTheme == true) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(Dimensions.Radius.lg)
                                    )
                                    .clickable{
                                        themeViewModel.updateDarkTheme(true)
                                    },
                                contentAlignment = Alignment.Center
                            ){
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DarkMode,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "Dark",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }

                        if (themeUiState.isDarkTheme != false) {
                            PreferenceSwitch(
                                title = "Amoled Black",
                                subtitle = "Use pure black background for deeper contrast",
                                checked = themeUiState.isAmoledMode,
                                onCheckedChange = { themeViewModel.updateAmoledMode(it) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                color = if (themeUiState.isAmoledMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.DarkMode,
                                            contentDescription = null,
                                            tint = if (themeUiState.isAmoledMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                padding = PaddingValues(0.dp),
                                isSingle = true
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.xl))
            }
        }
    }
}
