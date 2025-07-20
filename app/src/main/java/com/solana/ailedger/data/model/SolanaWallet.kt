package com.solana.ailedger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "solana_wallets")
data class SolanaWallet(
    @PrimaryKey
    val address: String,
    val name: String,
    val isActive: Boolean = true,
    val lastSyncedAt: Date? = null,
    val createdAt: Date = Date()
)

data class SolanaTransaction(
    val signature: String,
    val blockTime: Long?,
    val slot: Long,
    val amount: Double,
    val fee: Double,
    val fromAddress: String?,
    val toAddress: String?,
    val type: SolanaTransactionType,
    val programId: String?,
    val memo: String?
)

enum class SolanaTransactionType {
    TRANSFER,
    TOKEN_TRANSFER,
    SWAP,
    STAKE,
    UNSTAKE,
    NFT_MINT,
    NFT_TRANSFER,
    PROGRAM_INTERACTION,
    UNKNOWN
}

data class TokenInfo(
    val mint: String,
    val symbol: String,
    val name: String,
    val decimals: Int,
    val logoUri: String?
)