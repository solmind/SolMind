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
            val prompt = "You are given a piece of text describing a ledger change: [$content], please choose appropriate category of: spend or income;amount;the category among one of [FOOD_DINING,TRANSPORTATION,SHOPPING,ENTERTAINMENT,UTILITIES,HEALTHCARE,EDUCATION,TRAVEL,INVESTMENT,SALARY,FREELANCE,BUSINESS,GIFTS,OTHER], output with 3 pieces split by ;"
            
            // Get model path
            val modelPath = modelManager.getModelPath(selectedModel.id)
            
            // Simulate model inference using the selected model
            // In production, this would load the actual PyTorch model from modelPath
            val modelResponse = simulateModelResponse(content, selectedModel)
            
            // Parse the response
            parseModelResponse(modelResponse)
        }
    }
    
    private fun simulateModelResponse(content: String, model: LanguageModel): String {
        val normalizedContent = content.lowercase().trim()
        
        // Simulate intelligent parsing based on content
        return when {
            // Income patterns
            normalizedContent.contains("salary") || normalizedContent.contains("paycheck") ||
            normalizedContent.contains("income") || normalizedContent.contains("deposit") -> {
                val amount = extractAmount(normalizedContent) ?: "1000.00"
                "income;$amount;SALARY"
            }
            
            normalizedContent.contains("freelance") || normalizedContent.contains("consulting") -> {
                val amount = extractAmount(normalizedContent) ?: "500.00"
                "income;$amount;FREELANCE"
            }
            
            // Food & Dining
            normalizedContent.contains("restaurant") || normalizedContent.contains("food") ||
            normalizedContent.contains("coffee") || normalizedContent.contains("lunch") ||
            normalizedContent.contains("dinner") || normalizedContent.contains("pizza") -> {
                val amount = extractAmount(normalizedContent) ?: "25.00"
                "spend;$amount;FOOD_DINING"
            }
            
            // Transportation
            normalizedContent.contains("gas") || normalizedContent.contains("fuel") ||
            normalizedContent.contains("uber") || normalizedContent.contains("taxi") ||
            normalizedContent.contains("bus") || normalizedContent.contains("train") -> {
                val amount = extractAmount(normalizedContent) ?: "15.00"
                "spend;$amount;TRANSPORTATION"
            }
            
            // Shopping
            normalizedContent.contains("shopping") || normalizedContent.contains("store") ||
            normalizedContent.contains("amazon") || normalizedContent.contains("walmart") ||
            normalizedContent.contains("clothes") || normalizedContent.contains("purchase") -> {
                val amount = extractAmount(normalizedContent) ?: "50.00"
                "spend;$amount;SHOPPING"
            }
            
            // Entertainment
            normalizedContent.contains("movie") || normalizedContent.contains("cinema") ||
            normalizedContent.contains("netflix") || normalizedContent.contains("spotify") ||
            normalizedContent.contains("game") || normalizedContent.contains("concert") -> {
                val amount = extractAmount(normalizedContent) ?: "12.99"
                "spend;$amount;ENTERTAINMENT"
            }
            
            // Utilities
            normalizedContent.contains("electric") || normalizedContent.contains("water") ||
            normalizedContent.contains("internet") || normalizedContent.contains("phone") ||
            normalizedContent.contains("utility") || normalizedContent.contains("bill") -> {
                val amount = extractAmount(normalizedContent) ?: "75.00"
                "spend;$amount;UTILITIES"
            }
            
            // Healthcare
            normalizedContent.contains("doctor") || normalizedContent.contains("hospital") ||
            normalizedContent.contains("pharmacy") || normalizedContent.contains("medical") ||
            normalizedContent.contains("dentist") || normalizedContent.contains("medicine") -> {
                val amount = extractAmount(normalizedContent) ?: "100.00"
                "spend;$amount;HEALTHCARE"
            }
            
            // Education
            normalizedContent.contains("school") || normalizedContent.contains("tuition") ||
            normalizedContent.contains("course") || normalizedContent.contains("education") ||
            normalizedContent.contains("book") || normalizedContent.contains("university") -> {
                val amount = extractAmount(normalizedContent) ?: "200.00"
                "spend;$amount;EDUCATION"
            }
            
            // Travel
            normalizedContent.contains("hotel") || normalizedContent.contains("flight") ||
            normalizedContent.contains("travel") || normalizedContent.contains("vacation") ||
            normalizedContent.contains("airline") || normalizedContent.contains("booking") -> {
                val amount = extractAmount(normalizedContent) ?: "300.00"
                "spend;$amount;TRAVEL"
            }
            
            // Investment
            normalizedContent.contains("stock") || normalizedContent.contains("investment") ||
            normalizedContent.contains("crypto") || normalizedContent.contains("bitcoin") ||
            normalizedContent.contains("portfolio") || normalizedContent.contains("trading") -> {
                val amount = extractAmount(normalizedContent) ?: "500.00"
                "spend;$amount;INVESTMENT"
            }
            
            // Business
            normalizedContent.contains("business") || normalizedContent.contains("office") ||
            normalizedContent.contains("supplies") || normalizedContent.contains("equipment") ||
            normalizedContent.contains("software") || normalizedContent.contains("subscription") -> {
                val amount = extractAmount(normalizedContent) ?: "99.00"
                "spend;$amount;BUSINESS"
            }
            
            // Gifts
            normalizedContent.contains("gift") || normalizedContent.contains("present") ||
            normalizedContent.contains("birthday") || normalizedContent.contains("anniversary") ||
            normalizedContent.contains("donation") || normalizedContent.contains("charity") -> {
                val amount = extractAmount(normalizedContent) ?: "30.00"
                "spend;$amount;GIFTS"
            }
            
            // Default to OTHER
            else -> {
                val amount = extractAmount(normalizedContent) ?: "20.00"
                "spend;$amount;OTHER"
            }
        }
    }
    
    private fun extractAmount(text: String): String? {
        // Enhanced amount extraction patterns
        val patterns = listOf(
            """\$([0-9,]+\.?[0-9]*)""".toRegex(), // $123.45
            """([0-9,]+\.?[0-9]*)\s*\$""".toRegex(), // 123.45 $
            """([0-9,]+\.?[0-9]*)""".toRegex() // 123.45
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
    
    private fun parseModelResponse(response: String): TransactionParseResult {
        val parts = response.split(";")
        
        if (parts.size != 3) {
            return TransactionParseResult(
                type = TransactionType.EXPENSE,
                amount = 0.0,
                category = TransactionCategory.OTHER,
                confidence = 0.3f
            )
        }
        
        val typeStr = parts[0].trim().lowercase()
        val amountStr = parts[1].trim()
        val categoryStr = parts[2].trim().uppercase()
        
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
            amount > 0 && category != TransactionCategory.OTHER -> 0.9f
            amount > 0 -> 0.7f
            category != TransactionCategory.OTHER -> 0.6f
            else -> 0.3f
        }
        
        return TransactionParseResult(
            type = type,
            amount = amount,
            category = category,
            confidence = confidence
        )
    }
}

data class TransactionParseResult(
    val type: TransactionType,
    val amount: Double,
    val category: TransactionCategory,
    val confidence: Float
)