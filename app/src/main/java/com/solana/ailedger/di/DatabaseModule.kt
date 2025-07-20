package com.solana.ailedger.di

import android.content.Context
import androidx.room.Room
import com.solana.ailedger.data.database.AILedgerDatabase
import com.solana.ailedger.data.database.LedgerDao
import com.solana.ailedger.data.database.SolanaWalletDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAILedgerDatabase(
        @ApplicationContext context: Context
    ): AILedgerDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AILedgerDatabase::class.java,
            "ai_ledger_database"
        ).build()
    }
    
    @Provides
    fun provideLedgerDao(database: AILedgerDatabase): LedgerDao {
        return database.ledgerDao()
    }
    
    @Provides
    fun provideSolanaWalletDao(database: AILedgerDatabase): SolanaWalletDao {
        return database.solanaWalletDao()
    }
    
    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }
}