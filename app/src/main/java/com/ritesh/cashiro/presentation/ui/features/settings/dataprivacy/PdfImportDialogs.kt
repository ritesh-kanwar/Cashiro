package com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.ui.components.LoadingCircle
import com.ritesh.cashiro.presentation.ui.icons.HierarchySquare3
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.theme.LocalBlurEffects
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

// Shows while the PDF is being analyzed (processing indicator).
@OptIn(ExperimentalHazeApi::class)
@Composable
fun PdfProcessingDialog(
    isVisible: Boolean,
    error: String?,
    onDismissError: () -> Unit,
    blurEffects: Boolean = LocalBlurEffects.current,
    hazeState: HazeState = remember { HazeState() }
) {
    if (!isVisible && error == null) return

    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    Dialog(onDismissRequest = { if (error != null) onDismissError() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (blurEffects) Modifier.hazeEffect(
                        state = hazeState,
                        block = fun HazeEffectScope.() {
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                tint = HazeDefaults.tint(containerColor),
                                blurRadius = 20.dp,
                                noiseFactor = -1f
                            )
                            blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                        }
                    ) else Modifier
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (blurEffects) containerColor.copy(0.5f) else containerColor
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (error != null) {
                    Icon(
                        Icons.Rounded.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "PDF Import Error",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Button(
                        onClick = onDismissError,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Close") }
                } else {
                    LoadingCircle(modifier = Modifier.size(48.dp))
                    Text(
                        text = "Analyzing PDF...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Extracting transactions and bank accounts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfAccountDecisionCard(
    match: PdfAccountMatch,
    availableAccounts: List<AccountBalanceEntity>,
    currentDecision: AccountImportDecision,
    selectedMapping: AccountBalanceEntity?,
    onDecisionChanged: (AccountImportDecision) -> Unit,
    onMappingChanged: (AccountBalanceEntity?) -> Unit
) {
    var showAccountPicker by remember { mutableStateOf(false) }

    val cardColor = when (currentDecision) {
        AccountImportDecision.MERGE_WITH_EXISTING -> MaterialTheme.colorScheme.primaryContainer.copy(0.25f)
        AccountImportDecision.CREATE_NEW -> MaterialTheme.colorScheme.tertiaryContainer.copy(0.25f)
    }
    val subCardColor = when (currentDecision) {
        AccountImportDecision.MERGE_WITH_EXISTING -> MaterialTheme.colorScheme.primaryContainer
        AccountImportDecision.CREATE_NEW -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val subCardTextColor = when (currentDecision) {
        AccountImportDecision.MERGE_WITH_EXISTING -> MaterialTheme.colorScheme.onPrimaryContainer
        AccountImportDecision.CREATE_NEW -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val cardTextColor = when (currentDecision) {
        AccountImportDecision.MERGE_WITH_EXISTING -> MaterialTheme.colorScheme.primary
        AccountImportDecision.CREATE_NEW -> MaterialTheme.colorScheme.tertiary
    }


    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Account identity
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Icon(
                    Icons.Rounded.AccountBalance,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Column {
                    Text(
                        text = "Account ....${match.last4}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (selectedMapping != null) {
                        Text(
                            text = "Linked to: ${selectedMapping.bankName} (${selectedMapping.accountLast4})",
                            style = MaterialTheme.typography.labelSmall,
                            color = cardTextColor,
                        )
                    } else if (match.existingAccount != null) {
                        Text(
                            text = "Matches: ${match.existingAccount.bankName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = cardTextColor,
                        )
                    } else {
                        Text(
                            text = "No existing account found",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                        )
                    }
                }
            }

            // Decision options
            DecisionOption(
                selected = currentDecision == AccountImportDecision.MERGE_WITH_EXISTING,
                label = if (selectedMapping != null) "Merge with ${selectedMapping.bankName}" else "Link to existing account",
                description = if (selectedMapping != null) "Transactions will be added to this account" else "Select an account to link",
                icon = Iconax.HierarchySquare3,
                cardColor = subCardColor,
                cardTextColor =  subCardTextColor,
                onClick = { 
                    onDecisionChanged(AccountImportDecision.MERGE_WITH_EXISTING)
                    showAccountPicker = true
                }
            )
            
            DecisionOption(
                selected = currentDecision == AccountImportDecision.CREATE_NEW,
                label = "Create new bank \"${match.bankNameInPdf}\"",
                description = "Add as a separate account",
                icon = Icons.Rounded.Add,
                cardColor = subCardColor,
                cardTextColor =  subCardTextColor,
                onClick = { 
                    onDecisionChanged(AccountImportDecision.CREATE_NEW)
                    onMappingChanged(null)
                }
            )
        }
    }

    if (showAccountPicker) {
        ModalBottomSheet(
            onDismissRequest = { showAccountPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            com.ritesh.cashiro.presentation.ui.components.AccountSelectionSheet(
                accounts = availableAccounts,
                selectedAccount = selectedMapping,
                onAccountSelected = {
                    onMappingChanged(it)
                    showAccountPicker = false
                },
                showNoneOption = false
            )
        }
    }
}

@Composable
private fun DecisionOption(
    selected: Boolean,
    label: String,
    description: String,
    icon: ImageVector,
    cardColor: Color,
    cardTextColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) cardColor
                else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) cardTextColor else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) cardTextColor.copy(0.7f) else MaterialTheme.colorScheme.onSurface.copy(0.7f),
                )
            }
        }
    }
}
