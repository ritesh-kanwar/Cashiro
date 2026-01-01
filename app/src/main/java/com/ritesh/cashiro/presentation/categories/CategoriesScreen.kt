package com.ritesh.cashiro.presentation.categories

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.ui.components.CashiroCard
import com.ritesh.cashiro.ui.components.CategoryChip
import com.ritesh.cashiro.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.ui.components.SearchBarBox
import com.ritesh.cashiro.ui.components.SectionHeader
import com.ritesh.cashiro.ui.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.ui.theme.Dimensions
import com.ritesh.cashiro.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.animation.scaleIn
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CategoriesScreen(onNavigateBack: () -> Unit, viewModel: CategoriesViewModel = hiltViewModel()) {
    val categories by viewModel.filteredCategories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    
    // Use local TextFieldValue state for SearchBarBox to handle cursor position
    var searchInput by remember { mutableStateOf(TextFieldValue(text = searchQuery)) }

    // Sync input with ViewModel state (in case it changes externally)
    LaunchedEffect(searchQuery) {
        if (searchQuery != searchInput.text) {
            searchInput = searchInput.copy(text = searchQuery)
        }
    }

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
    var showFloatingLabel by remember { mutableStateOf(true) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }
    val labels = listOf("Search Fruits", "Search Shopping", "Search Fitness", "Search Sports")
    var currentLabelIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            currentLabelIndex = (currentLabelIndex + 1) % labels.size
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }.collect { firstVisibleItem ->
            // Show the label only when the list is scrolled to the top
            showFloatingLabel = firstVisibleItem == 0
        }
    }
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
                navigationContent = { NavigationContent(onNavigateBack) },
                actionContent = {
                    ActionContent(
                        showMenu = showFilterMenu,
                        onActionClick = { showFilterMenu = true },
                        onDismissMenu = { showFilterMenu = false },
                        onFilterSelected = { filter ->
                            selectedFilter = filter
                            showFilterMenu = false
                        }
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                expanded = showFloatingLabel,
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Category") },
                text = { Text(text = "Add Category") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = if (showFloatingLabel) MaterialTheme.shapes.extraLargeIncreased else MaterialTheme.shapes.large
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = {
                    Snackbar(
                        snackbarData = it,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.large,
                    )
                }
            )
        },
    ) { paddingValues ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .hazeSource(state = hazeState),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top =Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                bottom = 0.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item {
                SearchBarBox(
                    searchQuery = searchInput,
                    onSearchQueryChange = {
                        searchInput = it
                        viewModel.updateSearchQuery(it.text)
                    },
                    label = {
                        AnimatedContent(
                            targetState = labels[currentLabelIndex],
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(400, delayMillis = 100)) +
                                        slideInVertically(
                                            initialOffsetY = { it },
                                            animationSpec = tween(400, delayMillis = 100)
                                        ))
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(400)) +
                                                slideOutVertically(
                                                    targetOffsetY = { -it },
                                                    animationSpec = tween(400)
                                                )
                                    )
                            },
                            label = "SearchBarLabelAnimation"
                        ) { labelText ->
                            Text(
                                text = labelText,
                                fontSize = 14.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.inverseSurface.copy(0.5f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    leadingIcon = { },
                    trailingIcon = if (searchInput.text.isNotEmpty()) {
                        {
                            IconButton(onClick = {
                                searchInput = TextFieldValue("")
                                viewModel.updateSearchQuery("")
                            }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    } else {
                        {}
                    }
                )
            }

            // Recalculate grouped categories for the displayed list
            val dispExpenseCategories = categories.filter { !it.isIncome }
            val dispIncomeCategories = categories.filter { it.isIncome }


            when (selectedFilter) {
                "All" -> {
                    // Show all categories without headers
                    items(
                        items = categories, // Use the latest categories state directly
                        key = { "all-${it.id}" }) { category ->
                        AnimatedContent(
                            targetState = category,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                        scaleIn(
                                            initialScale = 0.92f,
                                            animationSpec = tween(220, delayMillis = 90)
                                        ) togetherWith fadeOut(animationSpec = tween(90))
                            },
                            label = "CategoryItemAnimation"
                        ) { animatedCategory ->
                            SwipeableCategoryItem(
                                category = animatedCategory,
                                subcategories = subcategories[animatedCategory.id] ?: emptyList(),
                                onEdit = { viewModel.showEditDialog(animatedCategory) },
                                onDelete = { viewModel.deleteCategory(animatedCategory) },
                                onAddSubcategory = {
                                    viewModel.showAddSubcategoryDialog(animatedCategory.id)
                                },
                                onEditSubcategory = { viewModel.showEditSubcategoryDialog(it) },
                            )
                        }
                    }
                }

                "Expense" -> {
                    // Expense Categories Section
                    if (dispExpenseCategories.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Expense Categories",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        items(
                            items = dispExpenseCategories,
                            key = { "expense-${it.id}" }) { category ->
                            SwipeableCategoryItem(
                                category = category,
                                subcategories = subcategories[category.id]
                                    ?: emptyList(),
                                onEdit = { viewModel.showEditDialog(category) },
                                onDelete = { viewModel.deleteCategory(category) },
                                onAddSubcategory = {
                                    viewModel.showAddSubcategoryDialog(category.id)
                                },
                                onEditSubcategory = {
                                    viewModel.showEditSubcategoryDialog(it)
                                },
                            )
                        }
                    }
                }

                "Income" -> {
                    // Income Categories Section
                    if (dispIncomeCategories.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Income Categories",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        items(
                            items = dispIncomeCategories,
                            key = { "income-${it.id}" }) { category ->
                            SwipeableCategoryItem(
                                category = category,
                                subcategories = subcategories[category.id]
                                    ?: emptyList(),
                                onEdit = { viewModel.showEditDialog(category) },
                                onDelete = { viewModel.deleteCategory(category) },
                                onAddSubcategory = {
                                    viewModel.showAddSubcategoryDialog(category.id)
                                },
                                onEditSubcategory = {
                                    viewModel.showEditSubcategoryDialog(it)
                                },
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
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
        val currentCategory =
            editingSubcategory?.categoryId?.let { catId -> categories.find { it.id == catId } }

        ModalBottomSheet(
            onDismissRequest = { viewModel.hideSubcategoryDialog() },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            EditSubcategorySheet(
                subcategory = editingSubcategory,
                categoryColor = currentCategory?.color ?: "#757575",
                categoryIconResId = currentCategory?.iconResId
                    ?: com.ritesh.cashiro.R.drawable.type_food_dining,
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
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {SwipeToDismissBoxValue.EndToStart -> {
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
        backgroundContent = { if (!category.isSystem) {
            val color by
            animateColorAsState(
                when (dismissState.dismissDirection) {SwipeToDismissBoxValue.EndToStart ->
                    MaterialTheme.colorScheme.error
                    else -> Color.Transparent },
                label = "background color"
            )
            Box(modifier = Modifier
                .fillMaxSize()
                .background(
                    color = color,
                    shape = MaterialTheme.shapes.large
                )
                .padding(horizontal = Dimensions.Padding.content),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }
        } },
        content = {
            CategoryItem(
                category = category,
                subcategories = subcategories,
                onClick = onEdit,
                onAddSubcategory = onAddSubcategory,
                onEditSubcategory = onEditSubcategory,
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !category.isSystem
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CategoryItem(
    category: CategoryEntity, subcategories: List<SubcategoryEntity>,
    onClick: (() -> Unit)?,
    onAddSubcategory: () -> Unit,
    onEditSubcategory: (SubcategoryEntity) -> Unit,
) {
    val showAddButton = subcategories.isEmpty()

    CashiroCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.content),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category with Icon
                CategoryChip(
                    category = category,
                    onClick = onClick,
                    showText = true,
                    modifier = Modifier.weight(1f)
                )

                // Subcategory Add/Toggle
                BlurredAnimatedVisibility(showAddButton) {
                    IconButton(
                        onClick = onAddSubcategory,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = MaterialTheme.shapes.largeIncreased
                    ) {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = "Add Subcategory",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavigationContent(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .animateContentSize()
            .padding(start = 16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onNavigateBack,
                ),
    ) {
        IconButton(
            onClick = onNavigateBack,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onBackground
            ),
            shapes =  IconButtonDefaults.shapes()
        ) {
            Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "Back Button",
                    modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActionContent(
        showMenu: Boolean,
        onActionClick: () -> Unit,
        onDismissMenu: () -> Unit,
        onFilterSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onActionClick
            )
    ) {
        IconButton(
            onClick = onActionClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onBackground
            ),
            shapes =  IconButtonDefaults.shapes()
        ) {
            Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = "More options",
                    modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onDismissMenu,
            shape = MaterialTheme.shapes.large,
            containerColor = Color.Transparent,
            shadowElevation = 0.dp,
            modifier = Modifier.padding(8.dp)
        ) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = { onFilterSelected("All") },
                modifier = Modifier
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        )
                    )
            )
            Spacer(modifier = Modifier.height(1.5.dp))
            DropdownMenuItem(
                text = { Text("Expense") },
                onClick = { onFilterSelected("Expense") },
                modifier = Modifier
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        )
                    )
            )
            Spacer(modifier = Modifier.height(1.5.dp))
            DropdownMenuItem(
                text = { Text("Income") },
                onClick = { onFilterSelected("Income") },
                modifier = Modifier
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
            )
        }
    }
}
