package com.ritesh.cashiro.presentation.ui.features.settings.rules

import android.content.Context
import android.net.Uri
import com.ritesh.cashiro.data.rules.RuleSharingCodec
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.domain.model.rule.TransactionRule
import com.ritesh.cashiro.domain.repository.RuleRepository
import com.ritesh.cashiro.domain.service.RuleTemplateService
import com.ritesh.cashiro.domain.usecase.ApplyRulesToPastTransactionsUseCase
import com.ritesh.cashiro.domain.usecase.BatchApplyResult
import com.ritesh.cashiro.domain.usecase.GetCategoriesUseCase
import com.ritesh.cashiro.domain.usecase.InitializeRuleTemplatesUseCase
import com.ritesh.cashiro.data.repository.SubcategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RulesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ruleRepository: RuleRepository,
    private val ruleTemplateService: RuleTemplateService,
    private val initializeRuleTemplatesUseCase: InitializeRuleTemplatesUseCase,
    private val applyRulesToPastTransactionsUseCase: ApplyRulesToPastTransactionsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val subcategoryRepository: SubcategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()

    val rules: StateFlow<List<TransactionRule>> = ruleRepository.getAllRules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _sharingMessage = MutableStateFlow<String?>(null)
    val sharingMessage = _sharingMessage.asStateFlow()
    private val _isSharing = MutableStateFlow(false)
    val isSharing = _isSharing.asStateFlow()

    fun clearSharingMessage() { _sharingMessage.value = null }

    fun reportNothingToExport() {
        _sharingMessage.value = "You don't have any custom rules to export yet."
    }

    fun exportRules(uri: Uri) {
        if (_isSharing.value) return
        _isSharing.value = true
        viewModelScope.launch {
            try {
                val exportable = RuleSharingCodec.exportable(ruleRepository.getAllRules().first())
                if (exportable.isEmpty()) {
                    reportNothingToExport()
                    return@launch
                }
                withContext(Dispatchers.IO) {
                    val text = RuleSharingCodec.encode(exportable)
                    context.contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write(text.toByteArray(Charsets.UTF_8))
                    } ?: error("Cannot open export destination")
                }
                _sharingMessage.value = "Exported ${exportable.size} rule(s)."
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _sharingMessage.value = "Couldn't export rules. Please choose another destination."
            } finally {
                _isSharing.value = false
            }
        }
    }

    fun importRules(uri: Uri) {
        if (_isSharing.value) return
        _isSharing.value = true
        viewModelScope.launch {
            try {
                val decoded = withContext(Dispatchers.IO) {
                    val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                        // Read one extra byte to detect oversized files without allocating them whole.
                        val bytes = ByteArray((RuleSharingCodec.MAX_FILE_BYTES + 1).toInt())
                        var count = 0
                        while (count < bytes.size) {
                            val read = stream.read(bytes, count, bytes.size - count)
                            if (read < 0) break
                            if (read == 0) {
                                val next = stream.read()
                                if (next < 0) break
                                bytes[count++] = next.toByte()
                            } else count += read
                        }
                        require(count <= RuleSharingCodec.MAX_FILE_BYTES) { "Rules file is too large" }
                        String(bytes, 0, count, Charsets.UTF_8)
                    } ?: error("Cannot open rules file")
                    RuleSharingCodec.decode(text)
                }
                val existingNames = ruleRepository.getAllRules().first()
                    .map { it.name.trim().lowercase() }.toSet()
                val fresh = decoded.rules.filterNot { it.name.lowercase() in existingNames }
                // Room's collection insert is atomic: a failed insert leaves no partial rule set.
                if (fresh.isNotEmpty()) ruleRepository.insertRules(fresh)
                val duplicates = decoded.duplicatedInFile + decoded.rules.size - fresh.size
                _sharingMessage.value = buildString {
                    append("Imported ${fresh.size} rule(s).")
                    if (duplicates > 0) append(" Skipped $duplicates duplicate name(s).")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _sharingMessage.value = "Couldn't import rules. Choose a supported rules JSON file under 1 MB with valid conditions and actions."
            } finally {
                _isSharing.value = false
            }
        }
    }

    // Categories for selection sheet
    val categories = getCategoriesUseCase.execute()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All subcategories map for selection sheet
    val allSubcategories = subcategoryRepository.subcategoriesMap

    init {
        initializeRules()
    }

    private fun initializeRules() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Initialize default rule templates if none exist
                initializeRuleTemplatesUseCase()
            } catch (e: Exception) {
                // Log error but don't crash
                e.printStackTrace()
            } finally {
            _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleRule(ruleId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                ruleRepository.setRuleActive(ruleId, isActive)
            } catch (e: Exception) {
                // Log error
                e.printStackTrace()
            }
        }
    }

    fun createRule(rule: TransactionRule) {
        viewModelScope.launch {
            try {
                ruleRepository.insertRule(rule)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            try {
                ruleRepository.deleteRule(ruleId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateRule(rule: TransactionRule) {
        viewModelScope.launch {
            try {
                ruleRepository.updateRule(rule)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getRuleApplicationCount(ruleId: String): Flow<Int> = flow {
        emit(ruleRepository.getRuleApplicationCount(ruleId))
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Force reset to default templates
                initializeRuleTemplatesUseCase(forceReset = true)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
            _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun applyRuleToPastTransactions(
        rule: TransactionRule,
        applyToUncategorizedOnly: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            _uiState.update { it.copy(batchApplyProgress = 0 to 0) }
            _uiState.update { it.copy(batchApplyResult = null) }

            try {
                val result = if (applyToUncategorizedOnly) {
                    applyRulesToPastTransactionsUseCase.applyRuleToUncategorizedTransactions(
                        rule = rule,
                        onProgress = { processed, total ->
                            _uiState.update { it.copy(batchApplyProgress = processed to total) }
                        }
                    )
                } else {
                    applyRulesToPastTransactionsUseCase.applyRuleToAllTransactions(
                        rule = rule,
                        onProgress = { processed, total ->
                            _uiState.update { it.copy(batchApplyProgress = processed to total) }
                        }
                    )
                }
                _uiState.update { it.copy(batchApplyResult = result) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(batchApplyResult = BatchApplyResult(
                    totalProcessed = 0,
                    totalUpdated = 0,
                    errors = listOf("Error: ${e.message}")
                )) }
            } finally {
            _uiState.update { it.copy(isLoading = false) }
                _uiState.update { it.copy(batchApplyProgress = null) }
            }
        }
    }

    fun applyAllRulesToPastTransactions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            _uiState.update { it.copy(batchApplyProgress = 0 to 0) }
            _uiState.update { it.copy(batchApplyResult = null) }

            try {
                val result = applyRulesToPastTransactionsUseCase.applyAllActiveRulesToTransactions(
                    onProgress = { processed, total ->
                        _uiState.update { it.copy(batchApplyProgress = processed to total) }
                    }
                )
                _uiState.update { it.copy(batchApplyResult = result) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(batchApplyResult = BatchApplyResult(
                    totalProcessed = 0,
                    totalUpdated = 0,
                    errors = listOf("Error: ${e.message}")
                )) }
            } finally {
            _uiState.update { it.copy(isLoading = false) }
                _uiState.update { it.copy(batchApplyProgress = null) }
            }
        }
    }

    fun clearBatchApplyResult() {
        _uiState.update { it.copy(batchApplyResult = null) }
    }
}