package com.solana.solmind.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import android.content.Context
import com.solana.solmind.data.model.LedgerEntry
import com.solana.solmind.data.model.SolanaWallet
import com.solana.solmind.data.model.TransactionCategory
import com.solana.solmind.data.model.TransactionType
import java.util.Date

@Database(
    entities = [LedgerEntry::class, SolanaWallet::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AILedgerDatabase : RoomDatabase() {
    
    abstract fun ledgerDao(): LedgerDao
    abstract fun solanaWalletDao(): SolanaWalletDao
    
    companion object {
        @Volatile
        private var INSTANCE: AILedgerDatabase? = null
        
        fun getDatabase(context: Context): AILedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AILedgerDatabase::class.java,
                    "ai_ledger_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String {
        return type.name
    }
    
    @TypeConverter
    fun toTransactionType(type: String): TransactionType {
        return TransactionType.valueOf(type)
    }
    
    @TypeConverter
    fun fromTransactionCategory(category: TransactionCategory): String {
        return category.name
    }
    
    @TypeConverter
    fun toTransactionCategory(category: String): TransactionCategory {
        return TransactionCategory.valueOf(category)
    }
}