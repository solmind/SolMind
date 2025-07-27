package com.solana.solmind.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solana.solmind.data.model.AccountMode
import com.solana.solmind.data.model.LedgerEntry
import com.solana.solmind.repository.LedgerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val ledgerRepository: LedgerRepository
) : ViewModel() {
    
    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> = _exportStatus.asStateFlow()
    
    fun exportTransactionsToCSV(context: Context, accountMode: AccountMode) {
        viewModelScope.launch {
            try {
                _exportStatus.value = ExportStatus.Loading
                
                // Get transactions based on account mode
                val transactions = if (accountMode == AccountMode.ONCHAIN || accountMode == AccountMode.OFFCHAIN) {
                    ledgerRepository.getEntriesByAccountMode(accountMode).first()
                } else {
                    ledgerRepository.getAllEntries().first()
                }
                
                // Create CSV file
                val csvFile = createCSVFile(context, transactions, accountMode)
                
                // Share the file
                shareCSVFile(context, csvFile)
                
                _exportStatus.value = ExportStatus.Success("Exported ${transactions.size} transactions")
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus.Error(e.message ?: "Export failed")
            }
        }
    }
    
    private fun createCSVFile(context: Context, transactions: List<LedgerEntry>, accountMode: AccountMode): File {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val fileName = "solmind_transactions_${accountMode.name.lowercase()}_$timestamp.csv"
        
        val file = File(context.getExternalFilesDir(null), fileName)
        
        FileWriter(file).use { writer ->
            // Write CSV header
            writer.append("Date,Description,Amount,Type,Category,Account Mode,Transaction Hash,Solana Address,Created At,Updated At\n")
            
            // Write transaction data
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            
            transactions.forEach { transaction ->
                writer.append("\"${dateFormatter.format(transaction.date)}\",")
                writer.append("\"${transaction.description.replace("\"", "\"\"")}\",")
                writer.append("${transaction.amount},")
                writer.append("${transaction.type.name},")
                writer.append("${transaction.category.name},")
                writer.append("${transaction.accountMode.name},")
                writer.append("\"${transaction.solanaTransactionHash ?: ""}\",")
                writer.append("\"${transaction.solanaAddress ?: ""}\",")
                writer.append("\"${dateFormatter.format(transaction.createdAt)}\",")
                writer.append("\"${dateFormatter.format(transaction.updatedAt)}\"\n")
            }
        }
        
        return file
    }
    
    private fun shareCSVFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SolMind Transaction Export")
            putExtra(Intent.EXTRA_TEXT, "Exported transaction data from SolMind")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooserIntent = Intent.createChooser(shareIntent, "Share CSV file")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
    
    fun clearExportStatus() {
        _exportStatus.value = ExportStatus.Idle
    }
}

sealed class ExportStatus {
    object Idle : ExportStatus()
    object Loading : ExportStatus()
    data class Success(val message: String) : ExportStatus()
    data class Error(val message: String) : ExportStatus()
}