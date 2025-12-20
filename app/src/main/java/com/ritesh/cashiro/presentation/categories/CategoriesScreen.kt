package com.ritesh.cashiro.presentation.categories

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.ui.components.CategoryChip
import com.ritesh.cashiro.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.ui.components.PennyWiseCard
import com.ritesh.cashiro.ui.components.SectionHeader
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.ui.theme.Dimensions
import com.ritesh.cashiro.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onNavigateBack: () -> Unit, viewModel: CategoriesViewModel = hiltViewModel()) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val showAddEditDialog by viewModel.showAddEditDialog.collectAsStateWithLifecycle()
    val editingCategory by viewModel.editingCategory.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val subcategories by viewModel.subcategories.collectAsStateWithLifecycle()
    val showSubcategoryDialog by viewModel.showSubcategoryDialog.collectAsStateWithLifecycle()
    val editingSubcategory by viewModel.editingSubcategory.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()

    // Show snackbar messages
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearSnackbarMessage()
            }
        }
    }

    // Group categories by type
    val expenseCategories = categories.filter { !it.isIncome }
    val incomeCategories = categories.filter { it.isIncome }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = "Categories",
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                onBackClick = onNavigateBack,
            )
        },
        floatingActionButton = {
            // FAB positioned at bottom end
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) { Icon(Icons.Default.Add, contentDescription = "Add Category") }
        },
        snackbarHost = {
            // Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
            )
        }

    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().overScrollVertical().hazeSource(state = hazeState),
                flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
                contentPadding = PaddingValues(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                    bottom = 0.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Expense Categories Section
                if (expenseCategories.isNotEmpty()) {
                    item { SectionHeader(title = "Expense Categories") }

                    items(items = expenseCategories, key = { it.id }) { category ->
                        SwipeableCategoryItem(
                            category = category,
                            subcategories = subcategories[category.id] ?: emptyList(),
                            onEdit = { viewModel.showEditDialog(category) },
                            onDelete = { viewModel.deleteCategory(category) },
                            onAddSubcategory = { viewModel.showAddSubcategoryDialog(category.id) },
                            onEditSubcategory = { viewModel.showEditSubcategoryDialog(it) },
                            onDeleteSubcategory = { viewModel.deleteSubcategory(it) }
                        )
                    }
                }

                // Income Categories Section
                if (incomeCategories.isNotEmpty()) {
                    item { SectionHeader(title = "Income Categories") }

                    items(items = incomeCategories, key = { it.id }) { category ->
                        SwipeableCategoryItem(
                            category = category,
                            subcategories = subcategories[category.id] ?: emptyList(),
                            onEdit = { viewModel.showEditDialog(category) },
                            onDelete = { viewModel.deleteCategory(category) },
                            onAddSubcategory = { viewModel.showAddSubcategoryDialog(category.id) },
                            onEditSubcategory = { viewModel.showEditSubcategoryDialog(it) },
                            onDeleteSubcategory = { viewModel.deleteSubcategory(it) }
                        )
                    }
                }

                item{
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }



    // Add/Edit Bottom Sheet
    if (showAddEditDialog) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideDialog() },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            EditCategorySheet(
                category = editingCategory,
                onDismiss = { viewModel.hideDialog() },
                onSave = { name, color, iconResId, isIncome ->
                    viewModel.saveCategory(name, color, iconResId, isIncome)
                }
            )
        }
    }

    if (showSubcategoryDialog) {
        SubcategoryEditDialog(
                subcategory = editingSubcategory,
                onDismiss = { viewModel.hideSubcategoryDialog() },
                onSave = { viewModel.saveSubcategory(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableCategoryItem(
    category: CategoryEntity,
    subcategories: List<SubcategoryEntity>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddSubcategory: () -> Unit,
    onEditSubcategory: (SubcategoryEntity) -> Unit,
    onDeleteSubcategory: (SubcategoryEntity) -> Unit
) {
    val dismissState =
            rememberSwipeToDismissBoxState(
                    confirmValueChange = { dismissValue ->
                        when (dismissValue) {
                            SwipeToDismissBoxValue.EndToStart -> {
                                if (!category.isSystem) {
                                    onDelete()
                                    true
                                } else {
                                    false // Don't allow swipe for system categories
                                }
                            }
                            else -> false
                        }
                    }
            )

    SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                if (!category.isSystem) {
                    val color by
                            animateColorAsState(
                                    when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.EndToStart ->
                                                MaterialTheme.colorScheme.error
                                        else -> Color.Transparent
                                    },
                                    label = "background color"
                            )
                    Box(
                            modifier =
                                    Modifier.fillMaxSize()
                                            .background(color)
                                            .padding(horizontal = Dimensions.Padding.content),
                            contentAlignment = Alignment.CenterEnd
                    ) {
                        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                            Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            },
            content = {
                CategoryItem(
                        category = category,
                        subcategories = subcategories,
                        onClick = if (!category.isSystem) onEdit else null,
                        onAddSubcategory = onAddSubcategory,
                        onEditSubcategory = onEditSubcategory,
                        onDeleteSubcategory = onDeleteSubcategory
                )
            },
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = !category.isSystem
    )
}

@Composable
private fun CategoryItem(
        category: CategoryEntity,
        subcategories: List<SubcategoryEntity>,
        onClick: (() -> Unit)?,
        onAddSubcategory: () -> Unit,
        onEditSubcategory: (SubcategoryEntity) -> Unit,
        onDeleteSubcategory: (SubcategoryEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    PennyWiseCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.content),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                // Category with Icon
                CategoryChip(category = category, showText = true, modifier = Modifier.weight(1f))

                // Subcategory Add/Toggle
                IconButton(onClick = onAddSubcategory) {
                    Icon(
                            Icons.Default.AddCircleOutline,
                            contentDescription = "Add Subcategory",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                    )
                }

                if (subcategories.isNotEmpty()) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                                if (expanded) Icons.Default.ExpandLess
                                else Icons.Default.ExpandMore,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // System badge
                if (category.isSystem) {
                    Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(start = Spacing.sm)
                    ) {
                        Text(
                                text = "System",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier =
                                        Modifier.padding(
                                                horizontal = Spacing.sm,
                                                vertical = Spacing.xs
                                        )
                        )
                    }
                } else {
                    // Edit icon for non-system categories
                    Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp).padding(start = Spacing.sm)
                    )
                }
            }

            if (expanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 48.dp, bottom = 8.dp)) {
                    subcategories.forEach { sub ->
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(vertical = 4.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                    text = sub.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                    onClick = { onEditSubcategory(sub) },
                                    modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Subcategory",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                    onClick = { onDeleteSubcategory(sub) },
                                    modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Subcategory",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubcategoryEditDialog(
        subcategory: SubcategoryEntity?,
        onDismiss: () -> Unit,
        onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(subcategory?.name ?: "") }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (subcategory == null) "Add Subcategory" else "Edit Subcategory") },
            text = {
                OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Subcategory Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                        onClick = { if (name.isNotBlank()) onSave(name) },
                        enabled = name.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
