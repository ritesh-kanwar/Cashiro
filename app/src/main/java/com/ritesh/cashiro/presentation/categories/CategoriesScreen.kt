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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.ui.components.CategoryChip
import com.ritesh.cashiro.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.ui.components.CashiroCard
import com.ritesh.cashiro.ui.components.SectionHeader
import com.ritesh.cashiro.ui.effects.BlurredAnimatedVisibility
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



    // Add/Edit Category Bottom Sheet
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
                onSave = { name, description, color, iconResId, isIncome ->
                    viewModel.saveCategory(name, description, color, iconResId, isIncome)
                },
                onReset = if (editingCategory?.isSystem == true) {
                    { categoryId -> viewModel.resetCategory(categoryId) }
                } else null
            )
        }
    }

    // Edit Subcategory Bottom Sheet
    if (showSubcategoryDialog) {
        val currentCategory = editingSubcategory?.categoryId?.let { catId ->
            categories.find { it.id == catId }
        }
        
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideSubcategoryDialog() },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            EditSubcategorySheet(
                subcategory = editingSubcategory,
                categoryColor = currentCategory?.color ?: "#757575",
                categoryIconResId = currentCategory?.iconResId ?: com.ritesh.cashiro.R.drawable.type_food_dining,
                onDismiss = { viewModel.hideSubcategoryDialog() },
                onSave = { name, iconResId, color ->
                    viewModel.saveSubcategory(name, iconResId, color)
                },
                onReset = if (editingSubcategory?.isSystem == true) {
                    { subcategoryId -> viewModel.resetSubcategory(subcategoryId) }
                } else null,
                onDelete = if (editingSubcategory != null) {
                    { subcategoryId ->
                        editingSubcategory?.let { viewModel.deleteSubcategory(it) }
                        viewModel.hideSubcategoryDialog()
                    }
                } else null
            )
        }
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
                                            .background(
                                                color= color,
                                                shape = MaterialTheme.shapes.large
                                            )
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
                        onClick = onEdit,
                        onAddSubcategory = onAddSubcategory,
                        onEditSubcategory = onEditSubcategory,
                        onDeleteSubcategory = onDeleteSubcategory
                )
            },
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = !category.isSystem
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    CashiroCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.content),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                // Category with Icon
                CategoryChip(category = category, onClick = onClick, showText = true, modifier = Modifier.weight(1f))

                // Subcategory Add/Toggle
                BlurredAnimatedVisibility(subcategories.isEmpty()) {
                    IconButton(
                        onClick = onAddSubcategory,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = MaterialTheme.shapes.largeIncreased
                    ) {
                        Icon(
                            Icons.Default.AddCircleOutline,
                            contentDescription = "Add Subcategory",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // System badge
                BlurredAnimatedVisibility(category.isSystem) {
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
                }
            }

            // Subcategory Row (horizontal chips)
            if (subcategories.isNotEmpty()) {
                SubcategoryRow(
                    subcategories = subcategories,
                    onSubcategoryClick = onEditSubcategory,
                    onAddClick = onAddSubcategory,
                    modifier = Modifier.padding(bottom = Spacing.xs)
                )
            } else {
                // Show add button if no subcategories
                Spacer(modifier = Modifier.height(Spacing.xs))
            }
        }
    }
}

