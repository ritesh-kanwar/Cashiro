package com.ritesh.cashiro.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.data.repository.CategoryRepository
import com.ritesh.cashiro.data.repository.SubcategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class CategoriesViewModel
@Inject
constructor(
        private val categoryRepository: CategoryRepository,
        private val subcategoryRepository: SubcategoryRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    // Categories list
    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.categories

    // Subcategories map (category ID -> list of subcategories)
    val subcategories: StateFlow<Map<Long, List<SubcategoryEntity>>> = 
        subcategoryRepository.subcategoriesMap

    init {
        // No longer need to launch separate collections for each category
        // or initialize defaults here as it's done in DatabaseModule
    }

    // Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Filtered categories
    val filteredCategories: StateFlow<List<CategoryEntity>> = combine(
        categories,
        subcategories,
        _searchQuery
    ) { categories, subcategoriesMap, query ->
        if (query.isBlank()) {
            categories
        } else {
            categories.filter { category ->
                val categoryMatches = category.name.contains(query, ignoreCase = true)
                val subcategoriesMatch = subcategoriesMap[category.id]?.any {
                    it.name.contains(query, ignoreCase = true)
                } == true
                categoryMatches || subcategoriesMatch
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dialog states
    private val _showAddEditDialog = MutableStateFlow(false)
    val showAddEditDialog: StateFlow<Boolean> = _showAddEditDialog.asStateFlow()

    private val _editingCategory = MutableStateFlow<CategoryEntity?>(null)
    val editingCategory: StateFlow<CategoryEntity?> = _editingCategory.asStateFlow()

    // Snackbar message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Subcategory Dialog states
    private val _showSubcategoryDialog = MutableStateFlow(false)
    val showSubcategoryDialog: StateFlow<Boolean> = _showSubcategoryDialog.asStateFlow()

    private val _editingSubcategory = MutableStateFlow<SubcategoryEntity?>(null)
    val editingSubcategory: StateFlow<SubcategoryEntity?> = _editingSubcategory.asStateFlow()

    private val _targetCategoryId = MutableStateFlow<Long?>(null)
    val targetCategoryId: StateFlow<Long?> = _targetCategoryId.asStateFlow()

    fun showAddDialog() {
        _editingCategory.value = null
        _showAddEditDialog.value = true
    }

    fun showEditDialog(category: CategoryEntity) {
        // Allow editing system categories now (they can be edited but not deleted)
        _editingCategory.value = category
        _showAddEditDialog.value = true
    }

    fun hideDialog() {
        _showAddEditDialog.value = false
        _editingCategory.value = null
    }

    fun showAddSubcategoryDialog(categoryId: Long) {
        _targetCategoryId.value = categoryId
        _editingSubcategory.value = null
        _showSubcategoryDialog.value = true
    }

    fun showEditSubcategoryDialog(subcategory: SubcategoryEntity) {
        _targetCategoryId.value = subcategory.categoryId
        _editingSubcategory.value = subcategory
        _showSubcategoryDialog.value = true
    }

    fun hideSubcategoryDialog() {
        _showSubcategoryDialog.value = false
        _editingSubcategory.value = null
        _targetCategoryId.value = null
    }

    fun saveCategory(name: String, description: String, color: String, iconResId: Int, isIncome: Boolean) {
        viewModelScope.launch {
            try {
                val editingCat = _editingCategory.value

                if (editingCat != null) {
                    // Update existing category
                    categoryRepository.updateCategory(
                            editingCat.copy(
                                name = name,
                                description = description,
                                color = color,
                                iconResId = iconResId,
                                isIncome = isIncome
                            )
                    )
                    _snackbarMessage.value = "Category updated successfully"
                } else {
                    // Check if category already exists
                    if (categoryRepository.categoryExists(name)) {
                        _snackbarMessage.value = "Category '$name' already exists"
                        return@launch
                    }

                    // Create new category
                    categoryRepository.createCategory(
                            name = name,
                            description = description,
                            color = color,
                            iconResId = iconResId,
                            isIncome = isIncome
                    )
                    _snackbarMessage.value = "Category created successfully"
                }

                hideDialog()
            } catch (e: Exception) {
                _snackbarMessage.value = "Error saving category: ${e.message}"
            }
        }
    }

    fun resetCategory(categoryId: Long) {
        viewModelScope.launch {
            try {
                categoryRepository.resetCategoryToDefault(categoryId)
                _snackbarMessage.value = "Category reset to default"
            } catch (e: Exception) {
                _snackbarMessage.value = "Error resetting category: ${e.message}"
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        if (category.isSystem) {
            _snackbarMessage.value = "System categories cannot be deleted"
            return
        }

        viewModelScope.launch {
            try {
                val deleted = categoryRepository.deleteCategory(category.id)
                if (deleted) {
                    _snackbarMessage.value = "Category deleted successfully"
                } else {
                    _snackbarMessage.value = "Cannot delete this category"
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "Error deleting category: ${e.message}"
            }
        }
    }

    fun saveSubcategory(name: String, iconResId: Int, color: String) {
        val categoryId = _targetCategoryId.value ?: return
        val editingSub = _editingSubcategory.value

        viewModelScope.launch {
            try {
                if (editingSub != null) {
                    subcategoryRepository.updateSubcategory(
                        editingSub.copy(name = name, iconResId = iconResId, color = color)
                    )
                    _snackbarMessage.value = "Subcategory updated"
                } else {
                    subcategoryRepository.createSubcategory(categoryId, name, iconResId, color)
                    _snackbarMessage.value = "Subcategory added"
                }
                hideSubcategoryDialog()
            } catch (e: Exception) {
                _snackbarMessage.value = "Error saving subcategory: ${e.message}"
            }
        }
    }

    fun resetSubcategory(subcategoryId: Long) {
        viewModelScope.launch {
            try {
                subcategoryRepository.resetSubcategoryToDefault(subcategoryId)
                _snackbarMessage.value = "Subcategory reset to default"
            } catch (e: Exception) {
                _snackbarMessage.value = "Error resetting subcategory: ${e.message}"
            }
        }
    }

    fun deleteSubcategory(subcategory: SubcategoryEntity) {
        if (subcategory.isSystem) {
            _snackbarMessage.value = "System subcategories cannot be deleted"
            return
        }

        viewModelScope.launch {
            try {
                val deleted = subcategoryRepository.deleteSubcategory(subcategory)
                if (deleted) {
                    _snackbarMessage.value = "Subcategory deleted"
                } else {
                    _snackbarMessage.value = "Cannot delete this subcategory"
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "Error deleting subcategory: ${e.message}"
            }
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }
}

data class CategoriesUiState(val isLoading: Boolean = false, val errorMessage: String? = null)
