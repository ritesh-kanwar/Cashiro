package com.ritesh.cashiro.presentation.ui.features.settings.rules

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ritesh.cashiro.domain.model.rule.ActionType
import com.ritesh.cashiro.domain.model.rule.ConditionOperator
import com.ritesh.cashiro.domain.model.rule.RuleAction
import com.ritesh.cashiro.domain.model.rule.RuleCondition
import com.ritesh.cashiro.domain.model.rule.TransactionField
import com.ritesh.cashiro.domain.model.rule.TransactionRule
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.presentation.ui.components.CategorySelectionSheet
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.presentation.ui.icons.Bag
import com.ritesh.cashiro.presentation.ui.icons.Box2
import com.ritesh.cashiro.presentation.ui.icons.DocumentText2
import com.ritesh.cashiro.presentation.ui.icons.Edit2
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreateRuleScreen(
    onNavigateBack: () -> Unit,
    onSaveRule: (TransactionRule) -> Unit,
    existingRule: TransactionRule? = null,
    rulesViewModel: RulesViewModel = hiltViewModel()
) {
    var ruleName by remember { mutableStateOf(existingRule?.name ?: "") }
    var description by remember { mutableStateOf(existingRule?.description ?: "") }

    // Initialize conditions state from existing rule or use a default one
    var conditions by remember {
        mutableStateOf(existingRule?.conditions ?: listOf(
            RuleCondition(TransactionField.AMOUNT, ConditionOperator.LESS_THAN, "")
        ))
    }
    // Track expanded state for each condition's field dropdown
    var fieldDropdownsExpanded by remember { mutableStateOf(List(conditions.size) { false }) }

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

    // Sync state when existingRule changes (for editing)
    LaunchedEffect(existingRule) {
        existingRule?.let { rule ->
            ruleName = rule.name
            description = rule.description ?: ""
            conditions = rule.conditions
            fieldDropdownsExpanded = List(rule.conditions.size) { false }
            val action = rule.actions.firstOrNull()
            if (action != null) {
                actionType = action.actionType
                actionField = action.field
                actionValue = action.value
            }
        }
    }

    var showCategorySheet by remember { mutableStateOf(false) }
    val categories by rulesViewModel.categories.collectAsState()
    val allSubcategories by rulesViewModel.allSubcategories.collectAsState(initial = emptyMap())

    // Common presets for quick setup
    val commonPresets = listOf(
        "Block OTPs" to {
            ruleName = "Block OTP Messages"
            conditions = listOf(
                RuleCondition(TransactionField.SMS_TEXT, ConditionOperator.CONTAINS, "OTP")
            )
            actionType = ActionType.BLOCK
            actionField = TransactionField.CATEGORY
            actionValue = ""
            fieldDropdownsExpanded = List(conditions.size) { false }
        },
        "Block Small Amounts" to {
            ruleName = "Block Small Transactions"
            conditions = listOf(
                RuleCondition(TransactionField.AMOUNT, ConditionOperator.LESS_THAN, "10")
            )
            actionType = ActionType.BLOCK
            actionField = TransactionField.CATEGORY
            actionValue = ""
            fieldDropdownsExpanded = List(conditions.size) { false }
        },
        "Small amounts → Food" to {
            ruleName = "Small Food Payments"
            conditions = listOf(
                RuleCondition(TransactionField.AMOUNT, ConditionOperator.LESS_THAN, "200")
            )
            actionType = ActionType.SET
            actionField = TransactionField.CATEGORY
            actionValue = "Food & Drinks"
            fieldDropdownsExpanded = List(conditions.size) { false }
        },
        "Standardize Merchant" to {
            ruleName = "Standardize Merchant Name"
            conditions = listOf(
                RuleCondition(TransactionField.MERCHANT, ConditionOperator.CONTAINS, "AMZN")
            )
            actionType = ActionType.SET
            actionField = TransactionField.MERCHANT
            actionValue = "Amazon"
            fieldDropdownsExpanded = List(conditions.size) { false }
        },
        "Mark as Income" to {
            ruleName = "Mark Credits as Income"
            conditions = listOf(
                RuleCondition(TransactionField.SMS_TEXT, ConditionOperator.CONTAINS, "credited")
            )
            actionType = ActionType.SET
            actionField = TransactionField.TYPE
            actionValue = "income"
            fieldDropdownsExpanded = List(conditions.size) { false }
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
                    .imePadding()
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
                // Quick templates
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Text(
                            text = "Quick Templates",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .overScrollVertical(false),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
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
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = AssistChipDefaults.assistChipBorder(
                                            enabled = true,
                                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                                            borderWidth = 1.dp
                                        ),
                                        shape = MaterialTheme.shapes.medium
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
                                    Iconax.Edit2,
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
                                    Iconax.DocumentText2,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }

                // Conditions section
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Conditions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // Add condition button
                            if (conditions.size < 5) {
                                Button(
                                    onClick = {
                                        conditions = conditions + RuleCondition(TransactionField.AMOUNT, ConditionOperator.LESS_THAN, "")
                                        fieldDropdownsExpanded = fieldDropdownsExpanded + false
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.height(32.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text("Add", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        conditions.forEachIndexed { index, condition ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(Dimensions.Padding.content),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        Text(
                                            text = "Condition ${index + 1}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        
                                        if (conditions.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    conditions = conditions.toMutableList().apply { removeAt(index) }
                                                    fieldDropdownsExpanded = fieldDropdownsExpanded.toMutableList().apply { removeAt(index) }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Iconax.Bag,
                                                    contentDescription = "Remove Condition",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Field selector
                                    ExposedDropdownMenuBox(
                                        expanded = fieldDropdownsExpanded.getOrElse(index) { false },
                                        onExpandedChange = { expanded ->
                                            fieldDropdownsExpanded = fieldDropdownsExpanded.toMutableList().apply {
                                                this[index] = expanded
                                            }
                                        }
                                    ) {
                                        TextField(
                                            value = when (condition.field) {
                                                TransactionField.AMOUNT -> "Amount"
                                                TransactionField.MERCHANT -> "Merchant"
                                                TransactionField.CATEGORY -> "Category"
                                                TransactionField.SMS_TEXT -> "SMS Text"
                                                TransactionField.TYPE -> "Transaction Type"
                                                TransactionField.SUBCATEGORY -> "Subcategory"
                                                TransactionField.BANK_NAME -> "Bank Name"
                                                else -> "Field"
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
                                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                                            ),
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = fieldDropdownsExpanded.getOrElse(index) { false }
                                                )
                                            },
                                            shape = MaterialTheme.shapes.largeIncreased,
                                            modifier = Modifier.fillMaxWidth()
                                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = fieldDropdownsExpanded.getOrElse(index) { false },
                                            onDismissRequest = { 
                                                fieldDropdownsExpanded = fieldDropdownsExpanded.toMutableList().apply {
                                                    this[index] = false
                                                }
                                            },
                                            shape = MaterialTheme.shapes.large
                                        ) {
                                            val menuItems = listOf(
                                                TransactionField.AMOUNT to "Amount",
                                                TransactionField.TYPE to "Transaction Type",
                                                TransactionField.CATEGORY to "Category",
                                                TransactionField.SUBCATEGORY to "Subcategory",
                                                TransactionField.MERCHANT to "Merchant",
                                                TransactionField.SMS_TEXT to "SMS Text",
                                                TransactionField.BANK_NAME to "Bank Name"
                                            )
                                            menuItems.forEachIndexed { i, (field, label) ->
                                                val isFirstItem = i == 0
                                                val isLastItem = i == menuItems.lastIndex
                                                val isMiddleItem = !isFirstItem && !isLastItem

                                                DropdownMenuItem(
                                                    text = { Text(label) },
                                                    onClick = {
                                                        conditions = conditions.toMutableList().apply {
                                                            val defaultOperator = if (field == TransactionField.AMOUNT) ConditionOperator.LESS_THAN else ConditionOperator.CONTAINS
                                                            val defaultValue = if (field == TransactionField.TYPE) "EXPENSE" else ""
                                                            this[index] = this[index].copy(
                                                                field = field,
                                                                operator = defaultOperator,
                                                                value = defaultValue
                                                            )
                                                        }
                                                        fieldDropdownsExpanded = fieldDropdownsExpanded.toMutableList().apply {
                                                            this[index] = false
                                                        }
                                                    },
                                                )

                                                if (isMiddleItem || (isFirstItem && menuItems.size > 2)) {
                                                    HorizontalDivider(
                                                        thickness = 1.5.dp,
                                                        color = MaterialTheme.colorScheme.surface
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Operator selector
                                    val operators = when (condition.field) {
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
                                                selected = condition.operator == op,
                                                onClick = { 
                                                    conditions = conditions.toMutableList().apply {
                                                        this[index] = this[index].copy(operator = op)
                                                    }
                                                },
                                                label = { Text(label) }
                                            )
                                        }
                                    }

                                    // Value input
                                    when (condition.field) {
                                        TransactionField.TYPE -> {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                listOf("INCOME", "EXPENSE", "CREDIT", "TRANSFER", "INVESTMENT").forEach { type ->
                                                    FilterChip(
                                                        selected = condition.value == type,
                                                        onClick = { 
                                                            conditions = conditions.toMutableList().apply {
                                                                this[index] = this[index].copy(value = type)
                                                            }
                                                        },
                                                        label = {
                                                            Text(
                                                                text = type.lowercase().let { if (it.isNotEmpty()) it.take(1).uppercase() + it.drop(1) else it },
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        else -> {
                                            TextField(
                                                value = condition.value,
                                                onValueChange = { newValue ->
                                                    conditions = conditions.toMutableList().apply {
                                                        this[index] = this[index].copy(value = newValue)
                                                    }
                                                },
                                                label = { Text("Value") },
                                                placeholder = {
                                                    Text(
                                                        when (condition.field) {
                                                            TransactionField.AMOUNT -> "e.g., 200"
                                                            TransactionField.MERCHANT -> "e.g., Swiggy"
                                                            TransactionField.SMS_TEXT -> "e.g., salary"
                                                            TransactionField.SUBCATEGORY -> "e.g., Rent"
                                                            TransactionField.CATEGORY -> "e.g., Food"
                                                            TransactionField.BANK_NAME -> "e.g., HDFC Bank"
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
                                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                                                ),
                                                keyboardOptions = if (condition.field == TransactionField.AMOUNT) {
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
                                    Icons.Rounded.AutoAwesome,
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
                                    shape = MaterialTheme.shapes.large
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

                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                actionType = type
                                                actionTypeDropdownExpanded = false
                                                if (type == ActionType.BLOCK) {
                                                    actionValue = "" // Clear value for BLOCK action
                                                }
                                            },
                                        )
                                        // Add a Spacer for middle items
                                        if (isMiddleItem || (isFirstItem && menuItems.size > 2) ) {
                                            HorizontalDivider(
                                                thickness = 1.5.dp,
                                                color = MaterialTheme.colorScheme.surface
                                            )
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
                                            Icons.Rounded.Block,
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
                                            TransactionField.SUBCATEGORY -> "Set Subcategory"
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
                                        shape = MaterialTheme.shapes.large
                                    ) {
                                        val menuItems = listOf(
                                            TransactionField.CATEGORY to "Set Category",
                                            TransactionField.SUBCATEGORY to "Set Subcategory",
                                            TransactionField.MERCHANT to "Set Merchant Name",
                                            TransactionField.TYPE to "Set Transaction Type",
                                            // NARRATION removed as requested
                                        )
                                        menuItems.forEachIndexed { index, (field, label) ->
                                            val isFirstItem = index == 0
                                            val isLastItem = index == menuItems.lastIndex
                                            val isMiddleItem = !isFirstItem && !isLastItem

                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = {
                                                    actionField = field
                                                    actionFieldDropdownExpanded = false
                                                    actionValue =
                                                        "" // Reset value when changing field
                                                },
                                            )
                                            // Add a Spacer for middle items
                                            if (isMiddleItem || (isFirstItem && menuItems.size > 2) ) {
                                                HorizontalDivider(
                                                    thickness = 1.5.dp,
                                                    color = MaterialTheme.colorScheme.surface
                                                )
                                            }
                                        }
                                    }
                                }

                                // Dynamic value input based on selected action field
                                when (actionField) {
                                    TransactionField.CATEGORY -> {
                                        // Category Selection
                                        ListItem(
                                            headlineContent = {
                                                Text(
                                                    if (actionValue.isBlank()) "Choose Category" else actionValue,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = if (actionValue.isBlank()) 
                                                        MaterialTheme.colorScheme.onSurfaceVariant 
                                                    else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            leadingContent = {
                                                Icon(
                                                    Iconax.Box2,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            trailingContent = {
                                                Icon(
                                                    Icons.Rounded.ChevronRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            colors = ListItemDefaults.colors(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(MaterialTheme.shapes.largeIncreased)
                                                .clickable { showCategorySheet = true }
                                        )
                                    }

                                    TransactionField.SUBCATEGORY -> {
                                        // Category Selection (reused for subcategory field too)
                                        ListItem(
                                            headlineContent = {
                                                Text(
                                                    if (actionValue.isBlank()) "Choose Subcategory" else actionValue,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = if (actionValue.isBlank()) 
                                                        MaterialTheme.colorScheme.onSurfaceVariant 
                                                    else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            leadingContent = {
                                                Icon(
                                                    Iconax.Box2,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            trailingContent = {
                                                Icon(
                                                    Icons.Rounded.ChevronRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            colors = ListItemDefaults.colors(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(MaterialTheme.shapes.largeIncreased)
                                                .clickable { showCategorySheet = true }
                                        )

                                        if (actionValue.isNotBlank()) {
                                            Text(
                                                text = "Selected Subcategory: $actionValue",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(start = Spacing.xs)
                                            )
                                        }
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
                                                            text = type.lowercase()
                                                                .let { if (it.isNotEmpty()) it.take(1).uppercase() + it.drop(1) else it },
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
                                                FilterChip(
                                                    selected = false,
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
                    val allConditionsValid = conditions.all { it.value.isNotBlank() }
                    val showPreview = ruleName.isNotBlank() && allConditionsValid &&
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
                                        conditions.forEachIndexed { index, condition ->
                                            if (index > 0) append(" AND ")
                                            append(
                                                when (condition.field) {
                                                    TransactionField.AMOUNT -> "amount"
                                                    TransactionField.TYPE -> "type"
                                                    TransactionField.CATEGORY -> "category"
                                                    TransactionField.SUBCATEGORY -> "subcategory"
                                                    TransactionField.MERCHANT -> "merchant"
                                                    TransactionField.SMS_TEXT -> "SMS text"
                                                    TransactionField.BANK_NAME -> "bank"
                                                    else -> "field"
                                                }
                                            )
                                            append(" ")
                                            append(
                                                when (condition.operator) {
                                                    ConditionOperator.LESS_THAN -> "is less than"
                                                    ConditionOperator.GREATER_THAN -> "is greater than"
                                                    ConditionOperator.EQUALS -> "equals"
                                                    ConditionOperator.CONTAINS -> "contains"
                                                    ConditionOperator.STARTS_WITH -> "starts with"
                                                    else -> "matches"
                                                }
                                            )
                                            append(" ")
                                            append(condition.value)
                                        }
                                        append(", ")
                                        append(
                                            when (actionField) {
                                                TransactionField.CATEGORY -> "set category to "
                                                TransactionField.MERCHANT -> "set merchant to "
                                                TransactionField.TYPE -> "set type to "
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
                        val allConditionsValid = conditions.all { it.value.isNotBlank() }
                        val isValid = ruleName.isNotBlank() && allConditionsValid &&
                                (actionType == ActionType.BLOCK || actionValue.isNotBlank())

                        if (isValid) {
                            val rule = TransactionRule(
                                id = existingRule?.id ?: UUID.randomUUID().toString(),
                                name = ruleName,
                                description = description.takeIf { it.isNotBlank() },
                                priority = existingRule?.priority ?: 100,
                                conditions = conditions,
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
                    enabled = ruleName.isNotBlank() && conditions.all { it.value.isNotBlank() } &&
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

    if (showCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            CategorySelectionSheet(
                categories = categories,
                subcategoriesMap = allSubcategories,
                onSelectionComplete = { category, subcategory ->
                    if (subcategory != null) {
                        actionField = TransactionField.SUBCATEGORY
                        actionValue = subcategory.name
                    } else {
                        actionField = TransactionField.CATEGORY
                        actionValue = category.name
                    }
                    showCategorySheet = false
                },
                onDismiss = { showCategorySheet = false }
            )
        }
    }
}