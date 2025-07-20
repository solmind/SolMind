package com.solana.ailedger.data.model

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

enum class TransactionCategory {
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
    OTHER;
    
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
        }
    }
}