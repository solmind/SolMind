package com.solmind.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.solmind.data.model.LedgerEntry
import com.solmind.data.model.SolanaWallet
import com.solmind.data.model.TransactionCategory
import com.solmind.data.model.TransactionType
import com.solmind.data.model.AccountMode
import java.util.Date

@Database(
    entities = [LedgerEntry::class, SolanaWallet::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AILedgerDatabase : RoomDatabase() {
    
    abstract fun ledgerDao(): LedgerDao
    abstract fun solanaWalletDao(): SolanaWalletDao
    
    companion object {
        @Volatile
        private var INSTANCE: AILedgerDatabase? = null
        
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE ledger_entries ADD COLUMN accountMode TEXT NOT NULL DEFAULT 'OFFCHAIN'")
            }
        }
        
        fun getDatabase(context: Context): AILedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AILedgerDatabase::class.java,
                    "ai_ledger_database"
                ).addMigrations(MIGRATION_1_2).build()
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
    
    @TypeConverter
    fun fromAccountMode(accountMode: AccountMode): String {
        return accountMode.name
    }
    
    @TypeConverter
    fun toAccountMode(accountMode: String): AccountMode {
        return AccountMode.valueOf(accountMode)
    }
}