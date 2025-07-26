package com.solana.solmind.repository

import com.solana.solmind.data.database.LedgerDao
import com.solana.solmind.data.database.SolanaWalletDao
import com.solana.solmind.data.model.LedgerEntry
import com.solana.solmind.data.model.SolanaWallet
import com.solana.solmind.data.model.TransactionCategory
import com.solana.solmind.data.model.TransactionType
import com.solana.solmind.data.model.AccountMode
import com.solana.solmind.service.AIService
import com.solana.solmind.service.SolanaService
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LedgerRepository @Inject constructor(
    private val ledgerDao: LedgerDao,
    private val solanaWalletDao: SolanaWalletDao,
    private val aiService: AIService,
    private val solanaService: SolanaService
) {
    
    // Ledger Entry operations
    fun getAllEntries(): Flow<List<LedgerEntry>> = ledgerDao.getAllEntries()
    
    fun getEntriesByAccountMode(accountMode: AccountMode): Flow<List<LedgerEntry>> = 
        ledgerDao.getEntriesByAccountMode(accountMode)
    
    fun getEntriesByType(type: TransactionType): Flow<List<LedgerEntry>> = 
        ledgerDao.getEntriesByType(type)
    
    fun getEntriesByTypeAndAccountMode(type: TransactionType, accountMode: AccountMode): Flow<List<LedgerEntry>> = 
        ledgerDao.getEntriesByTypeAndAccountMode(type, accountMode)
    
    fun getEntriesByCategory(category: TransactionCategory): Flow<List<LedgerEntry>> = 
        ledgerDao.getEntriesByCategory(category)
    
    fun getEntriesByCategoryAndAccountMode(category: TransactionCategory, accountMode: AccountMode): Flow<List<LedgerEntry>> = 
        ledgerDao.getEntriesByCategoryAndAccountMode(category, accountMode)
    
    fun getEntriesByDateRange(startDate: Date, endDate: Date): Flow<List<LedgerEntry>> = 
        ledgerDao.getEntriesByDateRange(startDate, endDate)
    
    fun getEntriesByDateRangeAndAccountMode(startDate: Date, endDate: Date, accountMode: AccountMode): Flow<List<LedgerEntry>> = 
        ledgerDao.getEntriesByDateRangeAndAccountMode(startDate, endDate, accountMode)
    
    suspend fun getEntryById(id: Long): LedgerEntry? = ledgerDao.getEntryById(id)
    
    suspend fun insertEntry(entry: LedgerEntry): Long = ledgerDao.insertEntry(entry)
    
    suspend fun updateEntry(entry: LedgerEntry) = ledgerDao.updateEntry(entry)
    
    suspend fun deleteEntry(entry: LedgerEntry) = ledgerDao.deleteEntry(entry)
    
    suspend fun deleteEntryById(id: Long) = ledgerDao.deleteEntryById(id)
    
    fun searchEntries(query: String): Flow<List<LedgerEntry>> = ledgerDao.searchEntries(query)
    
    suspend fun getTotalAmountByTypeAndDateRange(
        type: TransactionType,
        startDate: Date,
        endDate: Date
    ): Double = ledgerDao.getTotalAmountByTypeAndDateRange(type, startDate, endDate) ?: 0.0
    
    suspend fun getTotalAmountByTypeAndDateRangeAndAccountMode(
        type: TransactionType,
        startDate: Date,
        endDate: Date,
        accountMode: AccountMode
    ): Double = ledgerDao.getTotalAmountByTypeAndDateRangeAndAccountMode(type, startDate, endDate, accountMode) ?: 0.0
    
    // AI-powered entry creation using FLAN-T5-small model
    suspend fun createEntryFromText(text: String, amount: Double? = null, accountMode: AccountMode = AccountMode.OFFCHAIN): LedgerEntry {
        // Use the new LocalAIService with FLAN-T5-small model for intelligent parsing
        val parseResult = aiService.parseTransactionWithAI(text)
        
        // Use provided amount or extracted amount from AI, fallback to manual extraction
        val finalAmount = amount ?: parseResult.amount.takeIf { it > 0.0 } ?: aiService.extractAmountFromText(text) ?: 0.0
        
        return LedgerEntry(
            amount = finalAmount,
            description = parseResult.description.takeIf { it.isNotBlank() } ?: text,
            category = parseResult.category,
            type = parseResult.type,
            date = parseResult.date?.let { dateStr ->
                try {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dateStr)
                } catch (e: Exception) {
                    Date()
                }
            } ?: Date(),
            accountMode = accountMode,
            confidence = parseResult.confidence,
            isAutoDetected = true
        )
    }
    
    // Solana Wallet operations
    fun getAllWallets(): Flow<List<SolanaWallet>> = solanaWalletDao.getAllWallets()
    
    fun getActiveWallets(): Flow<List<SolanaWallet>> = solanaWalletDao.getActiveWallets()
    
    suspend fun getWalletByAddress(address: String): SolanaWallet? = 
        solanaWalletDao.getWalletByAddress(address)
    
    suspend fun addWallet(address: String, name: String): Boolean {
        return try {
            val isValid = solanaService.validateSolanaAddress(address)
            if (isValid) {
                val wallet = SolanaWallet(
                    address = address,
                    name = name,
                    isActive = true,
                    createdAt = Date()
                )
                solanaWalletDao.insertWallet(wallet)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun updateWallet(wallet: SolanaWallet) = solanaWalletDao.updateWallet(wallet)
    
    suspend fun deleteWallet(wallet: SolanaWallet) = solanaWalletDao.deleteWallet(wallet)
    
    suspend fun updateWalletSyncTime(address: String, syncTime: Date) = 
        solanaWalletDao.updateLastSyncTime(address, syncTime)
    
    // Sync Solana transactions
    suspend fun syncWalletTransactions(address: String): List<LedgerEntry> {
        try {
            val transactions = solanaService.getAccountTransactions(address, 50)
            val newEntries = mutableListOf<LedgerEntry>()
            
            for (transaction in transactions) {
                // Check if transaction already exists
                val existingEntry = ledgerDao.getEntryByTransactionHash(transaction.signature)
                if (existingEntry == null) {
                    // Create new ledger entry from Solana transaction with AI inference
                    val entry = createLedgerEntryFromSolanaTransaction(transaction, address)
                    val entryId = ledgerDao.insertEntry(entry)
                    newEntries.add(entry.copy(id = entryId))
                }
            }
            
            // Update wallet sync time
            solanaWalletDao.updateLastSyncTime(address, Date())
            
            return newEntries
        } catch (e: Exception) {
            throw e
        }
    }
    
    private suspend fun createLedgerEntryFromSolanaTransaction(
        transaction: com.solana.solmind.data.model.SolanaTransaction,
        userAddress: String
    ): LedgerEntry {
        val isIncoming = transaction.toAddress == userAddress
        val type = if (isIncoming) TransactionType.INCOME else TransactionType.EXPENSE
        
        // Create a descriptive text for AI analysis
        val transactionText = buildString {
            append("${transaction.amount} SOL ")
            append(if (isIncoming) "received from" else "sent to")
            append(" ${if (isIncoming) transaction.fromAddress else transaction.toAddress} ")
            append("via ${transaction.type.name.replace("_", " ").lowercase()} ")
            transaction.memo?.let { memo ->
                append("with memo: $memo")
            }
        }
        
        // Use AI service to infer category and description for on-chain transactions
        val aiResult = try {
            aiService.parseOnChainTransactionWithAI(transactionText, transaction.amount)
        } catch (e: Exception) {
            // Fallback to hardcoded mapping if AI fails
            null
        }
        
        // Use AI result or fallback to blockchain-specific category mapping
        val category = aiResult?.category ?: when (transaction.type) {
            com.solana.solmind.data.model.SolanaTransactionType.TRANSFER -> {
                TransactionCategory.TOKEN_TRANSFER
            }
            com.solana.solmind.data.model.SolanaTransactionType.TOKEN_TRANSFER -> {
                TransactionCategory.TOKEN_TRANSFER
            }
            com.solana.solmind.data.model.SolanaTransactionType.SWAP -> {
                TransactionCategory.DEFI_SWAP
            }
            com.solana.solmind.data.model.SolanaTransactionType.STAKE -> {
                TransactionCategory.DEFI_STAKING
            }
            com.solana.solmind.data.model.SolanaTransactionType.NFT_MINT -> {
                TransactionCategory.MINTING
            }
            com.solana.solmind.data.model.SolanaTransactionType.NFT_TRANSFER -> {
                if (isIncoming) TransactionCategory.NFT_SALE else TransactionCategory.NFT_PURCHASE
            }
            else -> TransactionCategory.OTHER
        }
        
        // Use AI-generated description or create a default one
        val description = aiResult?.description?.takeIf { it.isNotBlank() && it != "Transaction" } 
            ?: transaction.memo 
            ?: run {
                val typeStr = transaction.type.name.replace("_", " ").lowercase()
                    .replaceFirstChar { it.uppercase() }
                val directionStr = if (isIncoming) "Received" else "Sent"
                "$directionStr - $typeStr"
            }
        
        // Use AI confidence or default based on whether AI was used
        val confidence = aiResult?.confidence ?: 0.7f
        
        // Use the actual transaction amount, not the AI-parsed amount
        val finalAmount = transaction.amount
        
        return LedgerEntry(
            amount = finalAmount,
            description = description,
            category = category,
            type = type,
            date = if (transaction.blockTime != null) Date(transaction.blockTime * 1000) else Date(),
            accountMode = AccountMode.ONCHAIN,
            solanaTransactionHash = transaction.signature,
            solanaAddress = userAddress,
            isAutoDetected = true,
            confidence = confidence
        )
    }
}