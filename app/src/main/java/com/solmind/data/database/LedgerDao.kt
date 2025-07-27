package com.solmind.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.solmind.data.model.LedgerEntry
import com.solmind.data.model.TransactionCategory
import com.solmind.data.model.TransactionType
import com.solmind.data.model.AccountMode
import java.util.Date

@Dao
interface LedgerDao {
    
    @Query("SELECT * FROM ledger_entries ORDER BY date DESC, createdAt DESC")
    fun getAllEntries(): Flow<List<LedgerEntry>>
    
    @Query("SELECT * FROM ledger_entries WHERE accountMode = :accountMode ORDER BY date DESC, createdAt DESC")
    fun getEntriesByAccountMode(accountMode: AccountMode): Flow<List<LedgerEntry>>
    
    @Query("SELECT * FROM ledger_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): LedgerEntry?
    
    @Query("SELECT * FROM ledger_entries WHERE type = :type ORDER BY date DESC, createdAt DESC")
    fun getEntriesByType(type: TransactionType): Flow<List<LedgerEntry>>
    
    @Query("SELECT * FROM ledger_entries WHERE type = :type AND accountMode = :accountMode ORDER BY date DESC, createdAt DESC")
    fun getEntriesByTypeAndAccountMode(type: TransactionType, accountMode: AccountMode): Flow<List<LedgerEntry>>
    
    @Query("SELECT * FROM ledger_entries WHERE category = :category ORDER BY date DESC, createdAt DESC")
    fun getEntriesByCategory(category: TransactionCategory): Flow<List<LedgerEntry>>
    
    @Query("SELECT * FROM ledger_entries WHERE category = :category AND accountMode = :accountMode ORDER BY date DESC, createdAt DESC")
    fun getEntriesByCategoryAndAccountMode(category: TransactionCategory, accountMode: AccountMode): Flow<List<LedgerEntry>>
    
    @Query("SELECT * FROM ledger_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, createdAt DESC")
    fun getEntriesByDateRange(startDate: Date, endDate: Date): Flow<List<LedgerEntry>>
    
    @Query("SELECT * FROM ledger_entries WHERE date BETWEEN :startDate AND :endDate AND accountMode = :accountMode ORDER BY date DESC, createdAt DESC")
    fun getEntriesByDateRangeAndAccountMode(startDate: Date, endDate: Date, accountMode: AccountMode): Flow<List<LedgerEntry>>
    
    @Query("SELECT * FROM ledger_entries WHERE solanaTransactionHash = :hash")
    suspend fun getEntryByTransactionHash(hash: String): LedgerEntry?
    
    @Query("SELECT SUM(amount) FROM ledger_entries WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByTypeAndDateRange(type: TransactionType, startDate: Date, endDate: Date): Double?
    
    @Query("SELECT SUM(amount) FROM ledger_entries WHERE type = :type AND date BETWEEN :startDate AND :endDate AND accountMode = :accountMode")
    suspend fun getTotalAmountByTypeAndDateRangeAndAccountMode(type: TransactionType, startDate: Date, endDate: Date, accountMode: AccountMode): Double?
    
    @Query("SELECT category, SUM(amount) as total FROM ledger_entries WHERE type = :type AND date BETWEEN :startDate AND :endDate GROUP BY category")
    suspend fun getCategoryTotalsByTypeAndDateRange(type: TransactionType, startDate: Date, endDate: Date): List<CategoryTotal>
    
    @Query("SELECT category, SUM(amount) as total FROM ledger_entries WHERE type = :type AND date BETWEEN :startDate AND :endDate AND accountMode = :accountMode GROUP BY category")
    suspend fun getCategoryTotalsByTypeAndDateRangeAndAccountMode(type: TransactionType, startDate: Date, endDate: Date, accountMode: AccountMode): List<CategoryTotal>
    
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
    
    @Query("SELECT * FROM ledger_entries WHERE description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY date DESC, createdAt DESC")
    fun searchEntries(query: String): Flow<List<LedgerEntry>>
}

data class CategoryTotal(
    val category: TransactionCategory,
    val total: Double
)