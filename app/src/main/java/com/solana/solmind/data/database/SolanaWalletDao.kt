package com.solana.solmind.data.database

import androidx.room.*
import com.solana.solmind.data.model.SolanaWallet
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface SolanaWalletDao {
    
    @Query("SELECT * FROM solana_wallets ORDER BY createdAt DESC")
    fun getAllWallets(): Flow<List<SolanaWallet>>
    
    @Query("SELECT * FROM solana_wallets WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveWallets(): Flow<List<SolanaWallet>>
    
    @Query("SELECT * FROM solana_wallets WHERE address = :address")
    suspend fun getWalletByAddress(address: String): SolanaWallet?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: SolanaWallet)
    
    @Update
    suspend fun updateWallet(wallet: SolanaWallet)
    
    @Query("UPDATE solana_wallets SET lastSyncedAt = :syncTime WHERE address = :address")
    suspend fun updateLastSyncTime(address: String, syncTime: Date)
    
    @Query("UPDATE solana_wallets SET isActive = :isActive WHERE address = :address")
    suspend fun updateWalletStatus(address: String, isActive: Boolean)
    
    @Delete
    suspend fun deleteWallet(wallet: SolanaWallet)
    
    @Query("DELETE FROM solana_wallets WHERE address = :address")
    suspend fun deleteWalletByAddress(address: String)
}