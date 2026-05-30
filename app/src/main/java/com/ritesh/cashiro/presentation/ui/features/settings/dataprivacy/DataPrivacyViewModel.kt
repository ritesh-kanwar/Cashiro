package com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.backup.BackupConfiguration
import com.ritesh.cashiro.data.backup.BackupExporter
import com.ritesh.cashiro.data.backup.BackupImporter
import com.ritesh.cashiro.data.backup.ExportResult
import com.ritesh.cashiro.data.backup.ImportResult
import com.ritesh.cashiro.data.backup.ImportStrategy

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class DataPrivacyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupExporter: BackupExporter,
    private val backupImporter: BackupImporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataPrivacyUiState())
    val uiState: StateFlow<DataPrivacyUiState> = _uiState.asStateFlow()

    fun exportBackup(config: BackupConfiguration) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(importExportMessage = "Creating backup...") }
                when (val result = backupExporter.exportBackup(config)) {
                    is ExportResult.Success -> {
                        _uiState.update { it.copy(
                            exportedBackupFile = result.file,
                            importExportMessage = "Backup created successfully! Choose where to save it."
                        ) }
                    }
                    is ExportResult.Error -> {
                        _uiState.update { it.copy(importExportMessage = "Export failed: ${result.message}") }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(importExportMessage = "Export error: ${e.message}") }
            }
        }
    }

    fun saveBackupToFile(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value.exportedBackupFile?.let { file ->
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    _uiState.update { it.copy(
                        importExportMessage = "Backup saved successfully!",
                        exportedBackupFile = null
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(importExportMessage = "Failed to save backup: ${e.message}") }
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(importExportMessage = "Importing backup...") }
                when (val result = backupImporter.importBackup(uri, ImportStrategy.MERGE)) {
                    is ImportResult.Success -> {
                        _uiState.update { it.copy(importExportMessage = "Import successful! Imported ${result.importedTransactions} transactions, ${result.importedCategories} categories.") }
                    }
                    is ImportResult.Error -> {
                        _uiState.update { it.copy(importExportMessage = "Import failed: ${result.message}") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(importExportMessage = "Import error: ${e.message}") }
            }
        }
    }
    
    fun shareBackup() {
        _uiState.value.exportedBackupFile?.let { file ->
            shareBackupFile(file)
        }
    }

    private fun shareBackupFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Cashiro Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Share Backup").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e("DataPrivacyViewModel", "Error sharing backup file", e)
        }
    }
    
    fun clearImportExportMessage() {
        _uiState.update { it.copy(importExportMessage = null, hasNewAccountsCreated = false) }
    }
    
    fun clearExportedFile() {
        _uiState.update { it.copy(exportedBackupFile = null) }
    }

    private fun extractUtr(text: String?): String? {
        if (text == null) return null
        // Matches UPI: 123 456... or UTR No. 123 456... etc (allowing spaces in digits)
        val utrRegex = Regex("""(?:UPI[:\s]*|UTR\s+No\.?[:\s]*|Ref\s+No\.?[:\s]*|ID[:\s]*)([\d\s]+)""", RegexOption.IGNORE_CASE)
        return utrRegex.find(text)?.groupValues?.get(1)?.replace(Regex("""\D"""), "")
    }
}
