package com.ritesh.cashiro.presentation.transactions

import androidx.lifecycle.ViewModel
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.export.CsvExporter
import com.ritesh.cashiro.data.export.ExportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val csvExporter: CsvExporter
) : ViewModel() {
    
    fun exportTransactions(
        transactions: List<TransactionEntity>,
        fileName: String? = null
    ): Flow<ExportResult> {
        return csvExporter.exportTransactions(transactions, fileName)
    }
}