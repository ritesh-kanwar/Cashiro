package com.ritesh.cashiro.ui.screens.rules

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.ritesh.cashiro.domain.model.rule.*
import com.ritesh.cashiro.ui.components.CustomTitleTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.presentation.categories.NavigationContent
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.ritesh.cashiro.ui.theme.Dimensions
import com.ritesh.cashiro.ui.theme.Spacing
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.effects.rememberOverscrollFlingBehavior
import dev.chrisbanes.haze.hazeSource
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreateRuleScreen(
    onNavigateBack: () -> Unit,
    onSaveRule: (TransactionRule) -> Unit,
    existingRule: TransactionRule? = null
) {
    var ruleName by remember { mutableStateOf(existingRule?.name ?: "") }
    var description by remember { mutableStateOf(existingRule?.description ?: "") }

    // Initialize condition state from existing rule or use defaults
    var selectedField by remember {
        mutableStateOf(existingRule?.conditions?.firstOrNull()?.field ?: TransactionField.AMOUNT)
    }
    var fieldDropdownExpanded by remember { mutableStateOf(false) }
    var selectedOperator by remember {
        mutableStateOf(existingRule?.conditions?.firstOrNull()?.operator ?: ConditionOperator.LESS_THAN)
    }
    var conditionValue by remember {
        mutableStateOf(existingRule?.conditions?.firstOrNull()?.value ?: "")
    }

    // Initialize action state from existing rule or use defaults
    var actionType by remember {
        mutableStateOf(existingRule?.actions?.firstOrNull()?.actionType ?: ActionType.SET)
    }
    var actionField by remember {
        mutableStateOf(existingRule?.actions?.firstOrNull()?.field ?: TransactionField.CATEGORY)
    }
    var actionFieldDropdownExpanded by remember { mutableStateOf(false) }
    var actionTypeDropdownExpanded by remember { mutableStateOf(false) }
    var actionValue by remember {
        mutableStateOf(existingRule?.actions?.firstOrNull()?.value ?: "")
    }

    // Common presets for quick setup
    val commonPresets = listOf(
        "Block OTPs" to {
            ruleName = "Block OTP Messages"
            selectedField = TransactionField.SMS_TEXT
            selectedOperator = ConditionOperator.CONTAINS
            conditionValue = "OTP"
            actionType = ActionType.BLOCK
            actionField = TransactionField.CATEGORY
            actionValue = ""
        },
        "Block Small Amounts" to {
            ruleName = "Block Small Transactions"
            selectedField = TransactionField.AMOUNT
            selectedOperator = ConditionOperator.LESS_THAN
            conditionValue = "10"
            actionType = ActionType.BLOCK
            actionField = TransactionField.CATEGORY
            actionValue = ""
        },
        "Small amounts → Food" to {
            ruleName = "Small Food Payments"
            selectedField = TransactionField.AMOUNT
            selectedOperator = ConditionOperator.LESS_THAN
            conditionValue = "200"
            actionType = ActionType.SET
            actionField = TransactionField.CATEGORY
            actionValue = "Food & Dining"
        },
        "Standardize Merchant" to {
            ruleName = "Standardize Merchant Name"
            selectedField = TransactionField.MERCHANT
            selectedOperator = ConditionOperator.CONTAINS
            conditionValue = "AMZN"
            actionType = ActionType.SET
            actionField = TransactionField.MERCHANT
            actionValue = "Amazon"
        },
        "Mark as Income" to {
            ruleName = "Mark Credits as Income"
            selectedField = TransactionField.SMS_TEXT
            selectedOperator = ConditionOperator.CONTAINS
            conditionValue = "credited"
            actionType = ActionType.SET
            actionField = TransactionField.TYPE
            actionValue = "income"
        }
    )

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()

    val lazyListState = rememberLazyListState()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = if (existingRule != null) "Edit Rule" else "Create Rule",
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                onBackClick = onNavigateBack,
                navigationContent = { NavigationContent(onNavigateBack) },
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize()
                    .hazeSource(state = hazeState)
                    .imePadding() // Push content up when keyboard appears
                    .overScrollVertical(),
                state = lazyListState,
                contentPadding = PaddingValues(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                    bottom = 0.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
            ) {
                // Quick presets
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimensions.Padding.content),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Text(
                                text = "Quick Templates",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                commonPresets.forEach { (label, action) ->
                                    AssistChip(
                                        onClick = action,
                                        label = {
                                            Text(
                                                label,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.background,
                                            labelColor = MaterialTheme.colorScheme.secondary.copy(
                                                0.8f
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                // Rule name and description
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(1.5.dp)
                    ) {
                        TextField(
                            value = ruleName,
                            onValueChange = { ruleName = it },
                            label = { Text("Rule Name", fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("e.g., Food expenses under ₹200") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(
                                topStart = Dimensions.Radius.md,
                                topEnd = Dimensions.Radius.md,
                                bottomStart = Dimensions.Radius.xs,
                                bottomEnd = Dimensions.Radius.xs
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    0.7f
                                )
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DriveFileRenameOutline,
                                    contentDescription = null
                                )
                            }
                        )
                        TextField(
                            value = description,
                            onValueChange = { description = it },
                            label = {
                                Text(
                                    "Description (Optional)",
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            placeholder = { Text("\"What does this rule do?") },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(
                                topStart = Dimensions.Radius.xs,
                                topEnd = Dimensions.Radius.xs,
                                bottomStart = Dimensions.Radius.md,
                                bottomEnd = Dimensions.Radius.md
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    0.7f
                                )
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }

                // Condition section
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimensions.Padding.content),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "When",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Field selector
                            ExposedDropdownMenuBox(
                                expanded = fieldDropdownExpanded,
                                onExpandedChange = {
                                    fieldDropdownExpanded = !fieldDropdownExpanded
                                }
                            ) {
                                TextField(
                                    value = when (selectedField) {
                                        TransactionField.AMOUNT -> "Amount"
                                        TransactionField.MERCHANT -> "Merchant"
                                        TransactionField.CATEGORY -> "Category"
                                        TransactionField.SMS_TEXT -> "SMS Text"
                                        TransactionField.TYPE -> "Transaction Type"
                                        else -> "Amount"
                                    },
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Field") },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            0.7f
                                        )
                                    ),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = fieldDropdownExpanded
                                        )
                                    },
                                    shape = MaterialTheme.shapes.largeIncreased,
                                    modifier = Modifier.fillMaxWidth()
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = fieldDropdownExpanded,
                                    onDismissRequest = { fieldDropdownExpanded = false },
                                    containerColor = Color.Transparent,
                                    shadowElevation = 0.dp
                                ) {

                                    val menuItems = listOf(
                                        TransactionField.AMOUNT to "Amount",
                                        TransactionField.TYPE to "Transaction Type",
                                        TransactionField.CATEGORY to "Category",
                                        TransactionField.MERCHANT to "Merchant",
                                        TransactionField.SMS_TEXT to "SMS Text",
                                        TransactionField.BANK_NAME to "Bank Name"
                                    )
                                    menuItems.forEachIndexed { index, (field, label) ->
                                        val isFirstItem = index == 0
                                        val isLastItem = index == menuItems.lastIndex
                                        val isMiddleItem = !isFirstItem && !isLastItem

                                        val shape = when {
                                            isFirstItem -> RoundedCornerShape(
                                                topStart = Dimensions.Radius.md,
                                                topEnd = Dimensions.Radius.md,
                                                bottomStart = Dimensions.Radius.xs,
                                                bottomEnd = Dimensions.Radius.xs
                                            )
                                            isLastItem -> RoundedCornerShape(
                                                topStart = Dimensions.Radius.xs,
                                                topEnd = Dimensions.Radius.xs,
                                                bottomStart = Dimensions.Radius.md,
                                                bottomEnd = Dimensions.Radius.md
                                            )
                                            else -> RoundedCornerShape(Dimensions.Radius.xs) // Middle items
                                        }

                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                selectedField = field
                                                fieldDropdownExpanded = false
                                            },
                                            modifier = Modifier
                                                .background(color= MaterialTheme.colorScheme.surfaceContainer,shape = shape)
                                        )

                                        // Add a Spacer for middle items
                                        if (isMiddleItem || (isFirstItem && menuItems.size > 2) ) {
                                            Spacer(modifier = Modifier.height(1.5.dp))
                                        }
                                    }
                                }
                            }

                            // Operator selector
                            val operators = when (selectedField) {
                                TransactionField.AMOUNT -> listOf(
                                    ConditionOperator.LESS_THAN to "<",
                                    ConditionOperator.GREATER_THAN to ">",
                                    ConditionOperator.EQUALS to "="
                                )

                                else -> listOf(
                                    ConditionOperator.CONTAINS to "contains",
                                    ConditionOperator.EQUALS to "equals",
                                    ConditionOperator.STARTS_WITH to "starts with"
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                operators.forEach { (op, label) ->
                                    FilterChip(
                                        selected = selectedOperator == op,
                                        onClick = { selectedOperator = op },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            // Value input
                            when (selectedField) {
                                TransactionField.TYPE -> {
                                    // Transaction type chips for TYPE field
                                    Text(
                                        text = "Select transaction type:",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf(
                                            "INCOME",
                                            "EXPENSE",
                                            "CREDIT",
                                            "TRANSFER",
                                            "INVESTMENT"
                                        ).forEach { type ->
                                            FilterChip(
                                                selected = conditionValue == type,
                                                onClick = { conditionValue = type },
                                                label = {
                                                    Text(
                                                        type.lowercase()
                                                            .replaceFirstChar { it.uppercase() },
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    // Regular text input for other fields
                                    TextField(
                                        value = conditionValue,
                                        onValueChange = { conditionValue = it },
                                        label = { Text("Value") },
                                        placeholder = {
                                            Text(
                                                when (selectedField) {
                                                    TransactionField.AMOUNT -> "e.g., 200"
                                                    TransactionField.MERCHANT -> "e.g., Swiggy"
                                                    TransactionField.SMS_TEXT -> "e.g., salary"
                                                    else -> "Enter value"
                                                }
                                            )
                                        },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                0.7f
                                            )
                                        ),
                                        keyboardOptions = if (selectedField == TransactionField.AMOUNT) {
                                            KeyboardOptions(keyboardType = KeyboardType.Number)
                                        } else {
                                            KeyboardOptions.Default
                                        },
                                        shape = MaterialTheme.shapes.largeIncreased,
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }

                // Action section
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimensions.Padding.content),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Then",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Action type selector
                            ExposedDropdownMenuBox(
                                expanded = actionTypeDropdownExpanded,
                                onExpandedChange = {
                                    actionTypeDropdownExpanded = !actionTypeDropdownExpanded
                                }
                            ) {
                                TextField(
                                    value = when (actionType) {
                                        ActionType.BLOCK -> "Block Transaction"
                                        ActionType.SET -> "Set Field"
                                        ActionType.APPEND -> "Append to Field"
                                        ActionType.PREPEND -> "Prepend to Field"
                                        ActionType.CLEAR -> "Clear Field"
                                        ActionType.ADD_TAG -> "Add Tag"
                                        ActionType.REMOVE_TAG -> "Remove Tag"
                                    },
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Action Type") },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            0.7f
                                        )
                                    ),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = actionTypeDropdownExpanded
                                        )
                                    },
                                    shape = MaterialTheme.shapes.largeIncreased,
                                    modifier = Modifier.fillMaxWidth()
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = actionTypeDropdownExpanded,
                                    onDismissRequest = { actionTypeDropdownExpanded = false },
                                    containerColor = Color.Transparent,
                                    shadowElevation = 0.dp
                                ) {
                                    val menuItems = listOf(
                                        ActionType.BLOCK to "Block Transaction",
                                        ActionType.SET to "Set Field",
                                        ActionType.CLEAR to "Clear Field"
                                    )
                                    menuItems.forEachIndexed { index, (type, label) ->
                                        val isFirstItem = index == 0
                                        val isLastItem = index == menuItems.lastIndex
                                        val isMiddleItem = !isFirstItem && !isLastItem

                                        val shape = when {
                                            isFirstItem -> RoundedCornerShape(
                                                topStart = Dimensions.Radius.md,
                                                topEnd = Dimensions.Radius.md,
                                                bottomStart = Dimensions.Radius.xs,
                                                bottomEnd = Dimensions.Radius.xs
                                            )
                                            isLastItem -> RoundedCornerShape(
                                                topStart = Dimensions.Radius.xs,
                                                topEnd = Dimensions.Radius.xs,
                                                bottomStart = Dimensions.Radius.md,
                                                bottomEnd = Dimensions.Radius.md
                                            )
                                            else -> RoundedCornerShape(Dimensions.Radius.xs) // Middle items
                                        }
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                actionType = type
                                                actionTypeDropdownExpanded = false
                                                if (type == ActionType.BLOCK) {
                                                    actionValue = "" // Clear value for BLOCK action
                                                }
                                            },
                                            modifier = Modifier
                                                .background(color= MaterialTheme.colorScheme.surfaceContainer,shape = shape)
                                        )
                                        // Add a Spacer for middle items
                                        if (isMiddleItem || (isFirstItem && menuItems.size > 2) ) {
                                            Spacer(modifier = Modifier.height(1.5.dp))
                                        }
                                    }
                                }
                            }

                            // Show message for BLOCK action or field selector for others
                            if (actionType == ActionType.BLOCK) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = Spacing.xs),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(Spacing.md),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        Icon(
                                            Icons.Default.Block,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "Transactions matching this rule will be blocked and not saved",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            } else {
                                // Action field selector for non-BLOCK actions
                                ExposedDropdownMenuBox(
                                    expanded = actionFieldDropdownExpanded,
                                    onExpandedChange = {
                                        actionFieldDropdownExpanded = !actionFieldDropdownExpanded
                                    }
                                ) {
                                    TextField(
                                        value = when (actionField) {
                                            TransactionField.CATEGORY -> "Set Category"
                                            TransactionField.MERCHANT -> "Set Merchant Name"
                                            TransactionField.TYPE -> "Set Transaction Type"
                                            TransactionField.NARRATION -> "Set Description"
                                            else -> "Set Field"
                                        },
                                        onValueChange = { },
                                        readOnly = true,
                                        label = { Text("Action") },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = actionFieldDropdownExpanded
                                            )
                                        },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                0.7f
                                            )
                                        ),
                                        shape = MaterialTheme.shapes.largeIncreased,
                                        modifier = Modifier.fillMaxWidth()
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = actionFieldDropdownExpanded,
                                        onDismissRequest = { actionFieldDropdownExpanded = false },
                                        containerColor = Color.Transparent,
                                        shadowElevation = 0.dp
                                    ) {
                                        val menuItems = listOf(
                                            TransactionField.CATEGORY to "Set Category",
                                            TransactionField.MERCHANT to "Set Merchant Name",
                                            TransactionField.TYPE to "Set Transaction Type",
                                            TransactionField.NARRATION to "Set Description"
                                        )
                                        menuItems.forEachIndexed { index, (field, label) ->
                                            val isFirstItem = index == 0
                                            val isLastItem = index == menuItems.lastIndex
                                            val isMiddleItem = !isFirstItem && !isLastItem

                                            val shape = when {
                                                isFirstItem -> RoundedCornerShape(
                                                    topStart = Dimensions.Radius.md,
                                                    topEnd = Dimensions.Radius.md,
                                                    bottomStart = Dimensions.Radius.xs,
                                                    bottomEnd = Dimensions.Radius.xs
                                                )
                                                isLastItem -> RoundedCornerShape(
                                                    topStart = Dimensions.Radius.xs,
                                                    topEnd = Dimensions.Radius.xs,
                                                    bottomStart = Dimensions.Radius.md,
                                                    bottomEnd = Dimensions.Radius.md
                                                )
                                                else -> RoundedCornerShape(Dimensions.Radius.xs) // Middle items
                                            }
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = {
                                                    actionField = field
                                                    actionFieldDropdownExpanded = false
                                                    actionValue =
                                                        "" // Reset value when changing field
                                                },
                                                modifier = Modifier
                                                    .background(color= MaterialTheme.colorScheme.surfaceContainer,shape = shape)
                                            )
                                            // Add a Spacer for middle items
                                            if (isMiddleItem || (isFirstItem && menuItems.size > 2) ) {
                                                Spacer(modifier = Modifier.height(1.5.dp))
                                            }
                                        }
                                    }
                                }

                                // Dynamic value input based on selected action field
                                when (actionField) {
                                    TransactionField.CATEGORY -> {
                                        // Category chips and input
                                        val commonCategories = listOf(
                                            "Food & Dining", "Transportation", "Shopping",
                                            "Bills & Utilities", "Entertainment", "Healthcare",
                                            "Investments", "Others"
                                        )

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            commonCategories.forEach { category ->
                                                FilterChip(
                                                    selected = actionValue == category,
                                                    onClick = { actionValue = category },
                                                    label = {
                                                        Text(
                                                            category,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                )
                                            }
                                        }

                                        TextField(
                                            value = actionValue,
                                            onValueChange = { actionValue = it },
                                            label = { Text("Category Name") },
                                            placeholder = { Text("e.g., Rent") },
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    0.7f
                                                )
                                            ),
                                            shape = MaterialTheme.shapes.largeIncreased,
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }

                                    TransactionField.TYPE -> {
                                        // Transaction type chips
                                        Text(
                                            text = "Select transaction type:",
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            listOf(
                                                "INCOME",
                                                "EXPENSE",
                                                "CREDIT",
                                                "TRANSFER",
                                                "INVESTMENT"
                                            ).forEach { type ->
                                                FilterChip(
                                                    selected = actionValue == type,
                                                    onClick = { actionValue = type },
                                                    label = {
                                                        Text(
                                                            type.lowercase()
                                                                .replaceFirstChar { it.uppercase() },
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    TransactionField.MERCHANT -> {
                                        // Merchant name input with common suggestions
                                        val commonMerchants = listOf(
                                            "Amazon", "Swiggy", "Zomato", "Uber",
                                            "Netflix", "Google", "Flipkart"
                                        )

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            commonMerchants.forEach { merchant ->
                                                AssistChip(
                                                    onClick = { actionValue = merchant },
                                                    label = {
                                                        Text(
                                                            merchant,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                )
                                            }
                                        }

                                        TextField(
                                            value = actionValue,
                                            onValueChange = { actionValue = it },
                                            label = { Text("Merchant Name") },
                                            placeholder = { Text("e.g., Amazon") },
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    0.7f
                                                )
                                            ),

                                            shape = MaterialTheme.shapes.largeIncreased,
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }

                                    TransactionField.NARRATION -> {
                                        // Description/Narration input
                                        TextField(
                                            value = actionValue,
                                            onValueChange = { actionValue = it },
                                            label = { Text("Description") },
                                            placeholder = { Text("e.g., Monthly subscription payment") },
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    0.7f
                                                )
                                            ),

                                            shape = MaterialTheme.shapes.largeIncreased,
                                            modifier = Modifier.fillMaxWidth(),
                                            minLines = 2,
                                            maxLines = 3
                                        )
                                    }

                                    else -> {
                                        // Generic text input for other fields
                                        TextField(
                                            value = actionValue,
                                            onValueChange = { actionValue = it },
                                            label = { Text("Value") },
                                            placeholder = { Text("Enter value") },
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    0.7f
                                                )
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = MaterialTheme.shapes.largeIncreased,
                                            singleLine = true
                                        )
                                    }
                                }
                            } // End of if-else for BLOCK action
                        }
                    }
                }

                // Preview
                item {
                    val showPreview = ruleName.isNotBlank() && conditionValue.isNotBlank() &&
                            (actionType == ActionType.BLOCK || actionValue.isNotBlank())

                    if (showPreview) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(Dimensions.Padding.content),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                Text(
                                    text = "Rule Preview",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = buildString {
                                        append("When ")
                                        append(
                                            when (selectedField) {
                                                TransactionField.AMOUNT -> "amount"
                                                TransactionField.TYPE -> "type"
                                                TransactionField.CATEGORY -> "category"
                                                TransactionField.MERCHANT -> "merchant"
                                                TransactionField.SMS_TEXT -> "SMS text"
                                                TransactionField.BANK_NAME -> "bank"
                                                else -> "field"
                                            }
                                        )
                                        append(" ")
                                        append(
                                            when (selectedOperator) {
                                                ConditionOperator.LESS_THAN -> "is less than"
                                                ConditionOperator.GREATER_THAN -> "is greater than"
                                                ConditionOperator.EQUALS -> "equals"
                                                ConditionOperator.CONTAINS -> "contains"
                                                ConditionOperator.STARTS_WITH -> "starts with"
                                                else -> "matches"
                                            }
                                        )
                                        append(" ")
                                        append(conditionValue)
                                        append(", ")
                                        append(
                                            when (actionField) {
                                                TransactionField.CATEGORY -> "set category to "
                                                TransactionField.MERCHANT -> "set merchant to "
                                                TransactionField.TYPE -> "set type to "
                                                TransactionField.NARRATION -> "set description to "
                                                else -> "set field to "
                                            }
                                        )
                                        append(actionValue)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
                item{
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
            //Save or Add Rule
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush
                            .verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                    )
                    .padding(Spacing.md)
            ) {
                Button(
                    onClick = { // For BLOCK action, we don't need an action value
                        val isValid = ruleName.isNotBlank() && conditionValue.isNotBlank() &&
                                (actionType == ActionType.BLOCK || actionValue.isNotBlank())

                        if (isValid) {
                            val rule = TransactionRule(
                                id = existingRule?.id ?: UUID.randomUUID().toString(),
                                name = ruleName,
                                description = description.takeIf { it.isNotBlank() },
                                priority = existingRule?.priority ?: 100,
                                conditions = listOf(
                                    RuleCondition(
                                        field = selectedField,
                                        operator = selectedOperator,
                                        value = conditionValue
                                    )
                                ),
                                actions = listOf(
                                    RuleAction(
                                        field = actionField,
                                        actionType = actionType,
                                        value = if (actionType == ActionType.BLOCK) "" else actionValue
                                    )
                                ),
                                isActive = existingRule?.isActive ?: true,
                                isSystemTemplate = existingRule?.isSystemTemplate ?: false,
                                createdAt = existingRule?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            onSaveRule(rule)
                            // Navigation is handled in PennyWiseNavHost after saving
                        } },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shapes = ButtonDefaults.shapes(),
                    enabled = ruleName.isNotBlank() && conditionValue.isNotBlank() &&
                            (actionType == ActionType.BLOCK || actionValue.isNotBlank())
                ) {
                    Text(
                        text = "Save",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}