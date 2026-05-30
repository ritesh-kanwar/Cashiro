package com.ritesh.cashiro.presentation.ui.features.settings.importstatement

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.statement.ImportStatementUseCase
import com.ritesh.cashiro.data.statement.StatementImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ImportStatementUiState {
    data object Idle : ImportStatementUiState()
    data class Loading(val progress: Float = 0f) : ImportStatementUiState()
    data class Success(val result: StatementImportResult.Success) : ImportStatementUiState()
    data class Error(val message: String) : ImportStatementUiState()
}

@HiltViewModel
class ImportStatementViewModel @Inject constructor(
    private val importStatementUseCase: ImportStatementUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportStatementUiState>(ImportStatementUiState.Idle)
    val uiState: StateFlow<ImportStatementUiState> = _uiState.asStateFlow()

    fun importStatement(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _uiState.value = ImportStatementUiState.Loading(0f)
        viewModelScope.launch {
            when (val result = importStatementUseCase.import(uris) { progress ->
                _uiState.value = ImportStatementUiState.Loading(progress)
            }) {
                is StatementImportResult.Success -> {
                    _uiState.value = ImportStatementUiState.Success(result)
                }
                is StatementImportResult.Error -> {
                    _uiState.value = ImportStatementUiState.Error(result.message)
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = ImportStatementUiState.Idle
    }
}
