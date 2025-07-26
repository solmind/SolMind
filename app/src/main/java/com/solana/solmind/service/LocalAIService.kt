package com.solana.solmind.service

import android.content.Context
import com.solana.solmind.data.model.TransactionCategory
import com.solana.solmind.data.model.TransactionType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class LocalAIService @Inject constructor(
    private val context: Context,
    private val modelManager: ModelManager
) {
    
    // For now, we'll simulate the FLAN-T5-small model response
    // In a real implementation, you would load the actual model file
    suspend fun parseTransactionText(content: String): TransactionParseResult {
        return withContext(Dispatchers.IO) {
            val selectedModel = modelManager.selectedModel.value
                ?: throw IllegalStateException("No model selected. Please select a model first.")
            
            // Check if the selected model is downloaded
            if (!modelManager.isModelDownloaded(selectedModel.id)) {
                throw IllegalStateException("Selected model ${selectedModel.name} is not downloaded. Please download it first.")
            }
            
            // Create the prompt for the selected model
            val prompt = "You are given a piece of text describing a ledger change: [$content], please extract: transaction type (spend or income);amount;category from [FOOD_DINING,TRANSPORTATION,SHOPPING,ENTERTAINMENT,UTILITIES,HEALTHCARE,EDUCATION,TRAVEL,INVESTMENT,SALARY,FREELANCE,BUSINESS,GIFTS,OTHER];description (clean, descriptive summary);date (YYYY-MM-DD format, today if not specified). Output with 5 pieces split by ;"
            
            // Get model path
            val modelPath = modelManager.getModelPath(selectedModel.id)
            
            // Simulate model inference using the selected model
            // In production, this would load the actual PyTorch model from modelPath
            val modelResponse = simulateModelResponse(content, selectedModel)
            
            // Parse the response
            parseModelResponse(modelResponse)
        }
    }
    
    suspend fun parseOnChainTransactionWithAI(content: String, actualAmount: Double? = null): TransactionParseResult {
        return withContext(Dispatchers.IO) {
            val selectedModel = modelManager.selectedModel.value
                ?: throw IllegalStateException("No model selected. Please select a model first.")
            
            // Check if the selected model is downloaded
            if (!modelManager.isModelDownloaded(selectedModel.id)) {
                throw IllegalStateException("Selected model ${selectedModel.name} is not downloaded. Please download it first.")
            }
            
            // Create the prompt for on-chain transactions with blockchain-specific categories
            val prompt = "You are given a piece of text describing a blockchain transaction: [$content], please extract: transaction type (spend or income);amount;category from [DEFI_SWAP,DEFI_LENDING,DEFI_STAKING,NFT_PURCHASE,NFT_SALE,TOKEN_TRANSFER,BRIDGE,GAMING,MINTING,TRADING,INVESTMENT,OTHER];description (clean, descriptive summary);date (YYYY-MM-DD format, today if not specified). Output with 5 pieces split by ;"
            
            // Get model path
            val modelPath = modelManager.getModelPath(selectedModel.id)
            
            // Simulate model inference using the selected model for on-chain transactions
             val modelResponse = simulateOnChainModelResponse(content)
            
            // Parse the response and override amount if actualAmount is provided
            val parseResult = parseModelResponse(modelResponse)
            
            // Use actual amount if provided, otherwise use parsed amount
            if (actualAmount != null && actualAmount > 0.0) {
                parseResult.copy(amount = actualAmount)
            } else {
                parseResult
            }
        }
    }
    
    private fun simulateOnChainModelResponse(content: String): String {
        val normalizedContent = content.lowercase().trim()
        android.util.Log.d("LocalAIService", "Processing on-chain content: '$content' -> normalized: '$normalizedContent'")
        
        // Simulate intelligent parsing for blockchain transactions
        val result = when {
            // DeFi Swapping
            normalizedContent.contains("swap") || normalizedContent.contains("exchange") ||
            normalizedContent.contains("uniswap") || normalizedContent.contains("pancakeswap") -> {
                val amount = extractAmount(normalizedContent) ?: "100.00"
                val description = extractDescription(normalizedContent, "Token swap")
                val date = extractDate(normalizedContent)
                "spend;$amount;DEFI_SWAP;$description;$date"
            }
            
            // DeFi Lending
            normalizedContent.contains("lend") || normalizedContent.contains("borrow") ||
            normalizedContent.contains("compound") || normalizedContent.contains("aave") -> {
                val amount = extractAmount(normalizedContent) ?: "500.00"
                val description = extractDescription(normalizedContent, "DeFi lending")
                val date = extractDate(normalizedContent)
                "spend;$amount;DEFI_LENDING;$description;$date"
            }
            
            // DeFi Staking
            normalizedContent.contains("stake") || normalizedContent.contains("staking") ||
            normalizedContent.contains("validator") || normalizedContent.contains("delegate") -> {
                val amount = extractAmount(normalizedContent) ?: "1000.00"
                val description = extractDescription(normalizedContent, "Token staking")
                val date = extractDate(normalizedContent)
                "spend;$amount;DEFI_STAKING;$description;$date"
            }
            
            // NFT Purchase
            normalizedContent.contains("nft") && (normalizedContent.contains("buy") || normalizedContent.contains("purchase")) ||
            normalizedContent.contains("opensea") || normalizedContent.contains("magic eden") -> {
                val amount = extractAmount(normalizedContent) ?: "0.5"
                val description = extractDescription(normalizedContent, "NFT purchase")
                val date = extractDate(normalizedContent)
                "spend;$amount;NFT_PURCHASE;$description;$date"
            }
            
            // NFT Sale
            normalizedContent.contains("nft") && (normalizedContent.contains("sell") || normalizedContent.contains("sale")) -> {
                val amount = extractAmount(normalizedContent) ?: "0.3"
                val description = extractDescription(normalizedContent, "NFT sale")
                val date = extractDate(normalizedContent)
                "income;$amount;NFT_SALE;$description;$date"
            }
            
            // Token Transfer
            normalizedContent.contains("transfer") || normalizedContent.contains("send") ||
            normalizedContent.contains("receive") -> {
                val amount = extractAmount(normalizedContent) ?: "50.00"
                val description = extractDescription(normalizedContent, "Token transfer")
                val date = extractDate(normalizedContent)
                val type = if (normalizedContent.contains("receive")) "income" else "spend"
                "$type;$amount;TOKEN_TRANSFER;$description;$date"
            }
            
            // Bridge
            normalizedContent.contains("bridge") || normalizedContent.contains("cross-chain") -> {
                val amount = extractAmount(normalizedContent) ?: "25.00"
                val description = extractDescription(normalizedContent, "Cross-chain bridge")
                val date = extractDate(normalizedContent)
                "spend;$amount;BRIDGE;$description;$date"
            }
            
            // Gaming
            normalizedContent.contains("game") || normalizedContent.contains("gaming") ||
            normalizedContent.contains("play") || normalizedContent.contains("reward") -> {
                val amount = extractAmount(normalizedContent) ?: "10.00"
                val description = extractDescription(normalizedContent, "Gaming transaction")
                val date = extractDate(normalizedContent)
                val type = if (normalizedContent.contains("reward")) "income" else "spend"
                "$type;$amount;GAMING;$description;$date"
            }
            
            // Minting
            normalizedContent.contains("mint") || normalizedContent.contains("minting") -> {
                val amount = extractAmount(normalizedContent) ?: "0.1"
                val description = extractDescription(normalizedContent, "Token/NFT minting")
                val date = extractDate(normalizedContent)
                "spend;$amount;MINTING;$description;$date"
            }
            
            // Trading
            normalizedContent.contains("trade") || normalizedContent.contains("trading") ||
            normalizedContent.contains("buy") || normalizedContent.contains("sell") -> {
                val amount = extractAmount(normalizedContent) ?: "200.00"
                val description = extractDescription(normalizedContent, "Token trading")
                val date = extractDate(normalizedContent)
                "spend;$amount;TRADING;$description;$date"
            }
            
            // Investment
            normalizedContent.contains("invest") || normalizedContent.contains("investment") ||
            normalizedContent.contains("portfolio") -> {
                val amount = extractAmount(normalizedContent) ?: "1000.00"
                val description = extractDescription(normalizedContent, "Crypto investment")
                val date = extractDate(normalizedContent)
                "spend;$amount;INVESTMENT;$description;$date"
            }
            
            // Default to OTHER
            else -> {
                val amount = extractAmount(normalizedContent) ?: "20.00"
                val description = extractDescription(normalizedContent, "Other blockchain transaction")
                val date = extractDate(normalizedContent)
                "spend;$amount;OTHER;$description;$date"
            }
        }
        
        android.util.Log.d("LocalAIService", "On-chain model response: '$result'")
        return result
    }
    
    private fun simulateModelResponse(content: String, model: LanguageModel): String {
        val normalizedContent = content.lowercase().trim()
        android.util.Log.d("LocalAIService", "Processing content: '$content' -> normalized: '$normalizedContent'")
        
        // Simulate intelligent parsing based on content
        val result = when {
            // Income patterns
            normalizedContent.contains("salary") || normalizedContent.contains("paycheck") ||
            normalizedContent.contains("income") || normalizedContent.contains("deposit") -> {
                val amount = extractAmount(normalizedContent) ?: "1000.00"
                val description = extractDescription(normalizedContent, "Salary payment")
                val date = extractDate(normalizedContent)
                "income;$amount;SALARY;$description;$date"
            }
            
            normalizedContent.contains("freelance") || normalizedContent.contains("consulting") -> {
                val amount = extractAmount(normalizedContent) ?: "500.00"
                val description = extractDescription(normalizedContent, "Freelance work")
                val date = extractDate(normalizedContent)
                "income;$amount;FREELANCE;$description;$date"
            }
            
            // Food & Dining
            normalizedContent.contains("restaurant") || normalizedContent.contains("food") ||
            normalizedContent.contains("coffee") || normalizedContent.contains("lunch") ||
            normalizedContent.contains("dinner") || normalizedContent.contains("pizza") ||
            normalizedContent.contains("breakfast") -> {
                val amount = extractAmount(normalizedContent) ?: "25.00"
                val description = extractDescription(normalizedContent, "Food & dining")
                val date = extractDate(normalizedContent)
                android.util.Log.d("LocalAIService", "Matched FOOD_DINING category, amount: $amount")
                "spend;$amount;FOOD_DINING;$description;$date"
            }
            
            // Transportation
            normalizedContent.contains("gas") || normalizedContent.contains("fuel") ||
            normalizedContent.contains("uber") || normalizedContent.contains("taxi") ||
            normalizedContent.contains("bus") || normalizedContent.contains("train") -> {
                val amount = extractAmount(normalizedContent) ?: "15.00"
                val description = extractDescription(normalizedContent, "Transportation")
                val date = extractDate(normalizedContent)
                "spend;$amount;TRANSPORTATION;$description;$date"
            }
            
            // Shopping
            normalizedContent.contains("shopping") || normalizedContent.contains("store") ||
            normalizedContent.contains("amazon") || normalizedContent.contains("walmart") ||
            normalizedContent.contains("clothes") || normalizedContent.contains("purchase") -> {
                val amount = extractAmount(normalizedContent) ?: "50.00"
                val description = extractDescription(normalizedContent, "Shopping")
                val date = extractDate(normalizedContent)
                "spend;$amount;SHOPPING;$description;$date"
            }
            
            // Entertainment
            normalizedContent.contains("movie") || normalizedContent.contains("cinema") ||
            normalizedContent.contains("netflix") || normalizedContent.contains("spotify") ||
            normalizedContent.contains("game") || normalizedContent.contains("concert") -> {
                val amount = extractAmount(normalizedContent) ?: "12.99"
                val description = extractDescription(normalizedContent, "Entertainment")
                val date = extractDate(normalizedContent)
                "spend;$amount;ENTERTAINMENT;$description;$date"
            }
            
            // Utilities
            normalizedContent.contains("electric") || normalizedContent.contains("water") ||
            normalizedContent.contains("internet") || normalizedContent.contains("phone") ||
            normalizedContent.contains("utility") || normalizedContent.contains("bill") -> {
                val amount = extractAmount(normalizedContent) ?: "75.00"
                val description = extractDescription(normalizedContent, "Utility bill")
                val date = extractDate(normalizedContent)
                "spend;$amount;UTILITIES;$description;$date"
            }
            
            // Healthcare
            normalizedContent.contains("doctor") || normalizedContent.contains("hospital") ||
            normalizedContent.contains("pharmacy") || normalizedContent.contains("medical") ||
            normalizedContent.contains("dentist") || normalizedContent.contains("medicine") -> {
                val amount = extractAmount(normalizedContent) ?: "100.00"
                val description = extractDescription(normalizedContent, "Healthcare")
                val date = extractDate(normalizedContent)
                "spend;$amount;HEALTHCARE;$description;$date"
            }
            
            // Education
            normalizedContent.contains("school") || normalizedContent.contains("tuition") ||
            normalizedContent.contains("course") || normalizedContent.contains("education") ||
            normalizedContent.contains("book") || normalizedContent.contains("university") -> {
                val amount = extractAmount(normalizedContent) ?: "200.00"
                val description = extractDescription(normalizedContent, "Education")
                val date = extractDate(normalizedContent)
                "spend;$amount;EDUCATION;$description;$date"
            }
            
            // Travel
            normalizedContent.contains("hotel") || normalizedContent.contains("flight") ||
            normalizedContent.contains("travel") || normalizedContent.contains("vacation") ||
            normalizedContent.contains("airline") || normalizedContent.contains("booking") -> {
                val amount = extractAmount(normalizedContent) ?: "300.00"
                val description = extractDescription(normalizedContent, "Travel")
                val date = extractDate(normalizedContent)
                "spend;$amount;TRAVEL;$description;$date"
            }
            
            // Investment
            normalizedContent.contains("stock") || normalizedContent.contains("investment") ||
            normalizedContent.contains("crypto") || normalizedContent.contains("bitcoin") ||
            normalizedContent.contains("portfolio") || normalizedContent.contains("trading") -> {
                val amount = extractAmount(normalizedContent) ?: "500.00"
                val description = extractDescription(normalizedContent, "Investment")
                val date = extractDate(normalizedContent)
                "spend;$amount;INVESTMENT;$description;$date"
            }
            
            // Business
            normalizedContent.contains("business") || normalizedContent.contains("office") ||
            normalizedContent.contains("supplies") || normalizedContent.contains("equipment") ||
            normalizedContent.contains("software") || normalizedContent.contains("subscription") -> {
                val amount = extractAmount(normalizedContent) ?: "99.00"
                val description = extractDescription(normalizedContent, "Business expense")
                val date = extractDate(normalizedContent)
                "spend;$amount;BUSINESS;$description;$date"
            }
            
            // Gifts
            normalizedContent.contains("gift") || normalizedContent.contains("present") ||
            normalizedContent.contains("birthday") || normalizedContent.contains("anniversary") ||
            normalizedContent.contains("donation") || normalizedContent.contains("charity") -> {
                val amount = extractAmount(normalizedContent) ?: "30.00"
                val description = extractDescription(normalizedContent, "Gift")
                val date = extractDate(normalizedContent)
                "spend;$amount;GIFTS;$description;$date"
            }
            
            // Default to OTHER
            else -> {
                val amount = extractAmount(normalizedContent) ?: "20.00"
                val description = extractDescription(normalizedContent, "Other expense")
                val date = extractDate(normalizedContent)
                android.util.Log.d("LocalAIService", "No category match found, defaulting to OTHER, amount: $amount")
                "spend;$amount;OTHER;$description;$date"
            }
        }
        
        android.util.Log.d("LocalAIService", "Model response: '$result'")
        return result
    }
    
    private fun extractAmount(text: String): String? {
        // Enhanced amount extraction patterns
        val patterns = listOf(
            Regex("\\\$([0-9,]+\\.?[0-9]*)"), // $123.45
            Regex("([0-9,]+\\.?[0-9]*)\\s*\\\$"), // 123.45 $
            Regex("([0-9,]+\\.?[0-9]*)") // 123.45
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                return amountStr
            }
        }
        
        return null
    }
    
    private fun extractDescription(text: String, defaultDescription: String): String {
        // Clean up the text to create a meaningful description
        val cleanText = text.trim()
            .replace(Regex("\\\$[0-9,]+\\.?[0-9]*"), "") // Remove amounts
            .replace(Regex("[0-9,]+\\.?[0-9]*\\s*\\\$"), "") // Remove amounts
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
        
        return if (cleanText.isNotEmpty() && cleanText.length > 3) {
            cleanText.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            defaultDescription
        }
    }
    
    private fun extractDate(text: String): String {
        // Look for date patterns in the text
        val datePatterns = listOf(
            """(\d{4}-\d{2}-\d{2})""".toRegex(), // YYYY-MM-DD
            """(\d{1,2}/\d{1,2}/\d{4})""".toRegex(), // MM/DD/YYYY
            """(\d{1,2}-\d{1,2}-\d{4})""".toRegex() // MM-DD-YYYY
        )
        
        for (pattern in datePatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val dateStr = match.groupValues[1]
                // Convert to YYYY-MM-DD format if needed
                return when {
                    dateStr.contains("/") -> {
                        val parts = dateStr.split("/")
                        if (parts.size == 3) {
                            "${parts[2]}-${parts[0].padStart(2, '0')}-${parts[1].padStart(2, '0')}"
                        } else {
                            java.time.LocalDate.now().toString()
                        }
                    }
                    dateStr.contains("-") && dateStr.length == 10 -> dateStr
                    dateStr.contains("-") -> {
                        val parts = dateStr.split("-")
                        if (parts.size == 3 && parts[0].length == 2) {
                            "${parts[2]}-${parts[0].padStart(2, '0')}-${parts[1].padStart(2, '0')}"
                        } else {
                            java.time.LocalDate.now().toString()
                        }
                    }
                    else -> java.time.LocalDate.now().toString()
                }
            }
        }
        
        // Default to today's date
        return java.time.LocalDate.now().toString()
    }
    
    private fun parseModelResponse(response: String): TransactionParseResult {
        val parts = response.split(";")
        
        if (parts.size != 5) {
            return TransactionParseResult(
                type = TransactionType.EXPENSE,
                amount = 0.0,
                category = TransactionCategory.OTHER,
                description = "Unknown transaction",
                date = java.time.LocalDate.now().toString(),
                confidence = 0.3f
            )
        }
        
        val typeStr = parts[0].trim().lowercase()
        val amountStr = parts[1].trim()
        val categoryStr = parts[2].trim().uppercase()
        val description = parts[3].trim()
        val date = parts[4].trim()
        
        val type = when (typeStr) {
            "income" -> TransactionType.INCOME
            "spend" -> TransactionType.EXPENSE
            else -> TransactionType.EXPENSE
        }
        
        val amount = amountStr.toDoubleOrNull() ?: 0.0
        
        val category = try {
            TransactionCategory.valueOf(categoryStr)
        } catch (e: IllegalArgumentException) {
            TransactionCategory.OTHER
        }
        
        // Calculate confidence based on how well the parsing worked
        val confidence = when {
            amount > 0 && category != TransactionCategory.OTHER && description.isNotEmpty() -> 0.9f
            amount > 0 && category != TransactionCategory.OTHER -> 0.8f
            amount > 0 && description.isNotEmpty() -> 0.7f
            amount > 0 -> 0.6f
            category != TransactionCategory.OTHER -> 0.5f
            else -> 0.3f
        }
        
        return TransactionParseResult(
            type = type,
            amount = amount,
            category = category,
            description = description.ifEmpty { "Transaction" },
            date = date.ifEmpty { java.time.LocalDate.now().toString() },
            confidence = confidence
        )
    }
}

data class TransactionParseResult(
    val type: TransactionType,
    val amount: Double,
    val category: TransactionCategory,
    val description: String,
    val date: String?,
    val confidence: Float
)