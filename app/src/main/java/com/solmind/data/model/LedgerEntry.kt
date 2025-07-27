package com.solmind.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "ledger_entries")
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val category: TransactionCategory,
    val type: TransactionType,
    val date: Date,
    val accountMode: AccountMode = AccountMode.OFFCHAIN,
    val imageUri: String? = null,
    val solanaTransactionHash: String? = null,
    val solanaAddress: String? = null,
    val isAutoDetected: Boolean = false,
    val confidence: Float = 0f, // AI confidence score
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class AccountMode {
    ONCHAIN,
    OFFCHAIN;
    
    fun getDisplayName(): String {
        return when (this) {
            ONCHAIN -> "On-Chain"
            OFFCHAIN -> "Off-Chain"
        }
    }
}

enum class TransactionCategory {
    // Traditional off-chain categories
    FOOD_DINING,
    TRANSPORTATION,
    SHOPPING,
    ENTERTAINMENT,
    UTILITIES,
    HEALTHCARE,
    EDUCATION,
    TRAVEL,
    INVESTMENT,
    SALARY,
    FREELANCE,
    BUSINESS,
    GIFTS,
    OTHER,
    
    // Blockchain-specific categories
    DEFI_SWAP,
    DEFI_LENDING,
    DEFI_STAKING,
    NFT_PURCHASE,
    NFT_SALE,
    TOKEN_TRANSFER,
    BRIDGE,
    GAMING,
    MINTING,
    TRADING;
    
    fun getDisplayName(): String {
        return when (this) {
            FOOD_DINING -> "Food & Dining"
            TRANSPORTATION -> "Transportation"
            SHOPPING -> "Shopping"
            ENTERTAINMENT -> "Entertainment"
            UTILITIES -> "Utilities"
            HEALTHCARE -> "Healthcare"
            EDUCATION -> "Education"
            TRAVEL -> "Travel"
            INVESTMENT -> "Investment"
            SALARY -> "Salary"
            FREELANCE -> "Freelance"
            BUSINESS -> "Business"
            GIFTS -> "Gifts"
            OTHER -> "Other"
            DEFI_SWAP -> "DeFi Swap"
            DEFI_LENDING -> "DeFi Lending"
            DEFI_STAKING -> "DeFi Staking"
            NFT_PURCHASE -> "NFT Purchase"
            NFT_SALE -> "NFT Sale"
            TOKEN_TRANSFER -> "Token Transfer"
            BRIDGE -> "Cross-chain Bridge"
            GAMING -> "Gaming"
            MINTING -> "Minting"
            TRADING -> "Trading"
        }
    }
    
    fun getEmoji(): String {
        return when (this) {
            FOOD_DINING -> "🍽️"
            TRANSPORTATION -> "🚗"
            SHOPPING -> "🛍️"
            ENTERTAINMENT -> "🎬"
            UTILITIES -> "⚡"
            HEALTHCARE -> "🏥"
            EDUCATION -> "📚"
            TRAVEL -> "✈️"
            INVESTMENT -> "📈"
            SALARY -> "💰"
            FREELANCE -> "💼"
            BUSINESS -> "🏢"
            GIFTS -> "🎁"
            OTHER -> "📝"
            DEFI_SWAP -> "🔄"
            DEFI_LENDING -> "🏦"
            DEFI_STAKING -> "🔒"
            NFT_PURCHASE -> "🖼️"
            NFT_SALE -> "💎"
            TOKEN_TRANSFER -> "📤"
            BRIDGE -> "🌉"
            GAMING -> "🎮"
            MINTING -> "⚒️"
            TRADING -> "📊"
        }
    }
}