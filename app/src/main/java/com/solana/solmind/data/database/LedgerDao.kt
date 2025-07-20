package com.solana.solmind.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.solana.solmind.data.model.LedgerEntry
import com.solana.solmind.data.model.TransactionCategory
import com.solana.solmind.data.model.TransactionType
import java.util.Date

@Dao
interface LedgerDao {
    
    @Query("SELECT * FROM ledger_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<LedgerEntry>>
    
    @Query("SELECT * FROM ledger_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): LedgerEntry?
    
    @Query("SELECT * FROM ledger_entries WHERE type = :type ORDER BY date DESC")
    fun getEntriesByType(type: TransactionType): Flow<List<LedgerEntry>>
    
    @Query("SELECT * FROM ledger_entries WHERE category = :category ORDER BY date DESC")
    fun getEntriesByCategory(category: TransactionCategory): Flow<List<LedgerEntry>>
    
    @Query("SELECT * FROM ledger_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getEntriesByDateRange(startDate: Date, endDate: Date): Flow<List<LedgerEntry>>
    
    @Query("SELECT * FROM ledger_entries WHERE solanaTransactionHash = :hash")
    suspend fun getEntryByTransactionHash(hash: String): LedgerEntry?
    
    @Query("SELECT SUM(amount) FROM ledger_entries WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByTypeAndDateRange(type: TransactionType, startDate: Date, endDate: Date): Double?
    
    @Query("SELECT category, SUM(amount) as total FROM ledger_entries WHERE type = :type AND date BETWEEN :startDate AND :endDate GROUP BY category")
    suspend fun getCategoryTotalsByTypeAndDateRange(type: TransactionType, startDate: Date, endDate: Date): List<CategoryTotal>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntry): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<LedgerEntry>)
    
    @Update
    suspend fun updateEntry(entry: LedgerEntry)
    
    @Delete
    suspend fun deleteEntry(entry: LedgerEntry)
    
    @Query("DELETE FROM ledger_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)
    
    @Query("SELECT * FROM ledger_entries WHERE description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchEntries(query: String): Flow<List<LedgerEntry>>
}

data class CategoryTotal(
    val category: TransactionCategory,
    val total: Double
)