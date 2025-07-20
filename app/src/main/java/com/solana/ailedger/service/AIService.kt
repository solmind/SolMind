package com.solana.ailedger.service

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.solana.ailedger.data.model.TransactionCategory
import com.solana.ailedger.data.model.TransactionType
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class AIService @Inject constructor(
    private val context: Context
) {
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    suspend fun extractTextFromImage(bitmap: Bitmap): String {
        return suspendCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener {
                    continuation.resume("")
                }
        }
    }
    
    fun categorizeTransaction(text: String): CategoryPrediction {
        val normalizedText = text.lowercase().trim()
        
        // Food & Dining keywords
        val foodKeywords = listOf(
            "restaurant", "cafe", "coffee", "pizza", "burger", "food", "dining", 
            "mcdonalds", "starbucks", "subway", "kfc", "dominos", "uber eats", 
            "doordash", "grubhub", "delivery", "takeout", "lunch", "dinner", "breakfast"
        )
        
        // Transportation keywords
        val transportKeywords = listOf(
            "gas", "fuel", "uber", "lyft", "taxi", "bus", "train", "metro", "parking", 
            "toll", "car", "vehicle", "transport", "airline", "flight", "airport"
        )
        
        // Shopping keywords
        val shoppingKeywords = listOf(
            "amazon", "walmart", "target", "costco", "shopping", "store", "mall", 
            "purchase", "buy", "clothes", "clothing", "shoes", "electronics", "grocery"
        )
        
        // Entertainment keywords
        val entertainmentKeywords = listOf(
            "movie", "cinema", "theater", "netflix", "spotify", "game", "gaming", 
            "entertainment", "concert", "show", "ticket", "event", "music", "streaming"
        )
        
        // Utilities keywords
        val utilitiesKeywords = listOf(
            "electric", "electricity", "water", "gas bill", "internet", "phone", 
            "utility", "bill", "payment", "service", "subscription", "monthly"
        )
        
        // Healthcare keywords
        val healthcareKeywords = listOf(
            "doctor", "hospital", "pharmacy", "medicine", "medical", "health", 
            "clinic", "dentist", "insurance", "prescription", "treatment"
        )
        
        // Income keywords
        val incomeKeywords = listOf(
            "salary", "wage", "income", "payment received", "deposit", "payroll", 
            "freelance", "consulting", "bonus", "commission", "refund", "cashback"
        )
        
        // Check for income first
        if (incomeKeywords.any { normalizedText.contains(it) }) {
            return CategoryPrediction(TransactionCategory.SALARY, TransactionType.INCOME, 0.8f)
        }
        
        // Check expense categories
        val categoryMatches = mapOf(
            TransactionCategory.FOOD_DINING to foodKeywords,
            TransactionCategory.TRANSPORTATION to transportKeywords,
            TransactionCategory.SHOPPING to shoppingKeywords,
            TransactionCategory.ENTERTAINMENT to entertainmentKeywords,
            TransactionCategory.UTILITIES to utilitiesKeywords,
            TransactionCategory.HEALTHCARE to healthcareKeywords
        )
        
        for ((category, keywords) in categoryMatches) {
            val matchCount = keywords.count { normalizedText.contains(it) }
            if (matchCount > 0) {
                val confidence = (matchCount.toFloat() / keywords.size).coerceAtMost(0.9f)
                return CategoryPrediction(category, TransactionType.EXPENSE, confidence)
            }
        }
        
        // Default to OTHER with low confidence
        return CategoryPrediction(TransactionCategory.OTHER, TransactionType.EXPENSE, 0.3f)
    }
    
    fun extractAmountFromText(text: String): Double? {
        // Regex patterns for different currency formats
        val patterns = listOf(
            "\$([0-9,]+\.?[0-9]*)".toRegex(), // $123.45
            "([0-9,]+\.?[0-9]*)\s*\$".toRegex(), // 123.45 $
            "([0-9,]+\.?[0-9]*)".toRegex() // 123.45
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                return amountStr.toDoubleOrNull()
            }
        }
        
        return null
    }
    
    fun extractDateFromText(text: String): String? {
        // Simple date extraction - can be enhanced
        val datePatterns = listOf(
            "\d{1,2}/\d{1,2}/\d{4}".toRegex(), // MM/dd/yyyy
            "\d{1,2}-\d{1,2}-\d{4}".toRegex(), // MM-dd-yyyy
            "\d{4}-\d{1,2}-\d{1,2}".toRegex()  // yyyy-MM-dd
        )
        
        for (pattern in datePatterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.value
            }
        }
        
        return null
    }
}

data class CategoryPrediction(
    val category: TransactionCategory,
    val type: TransactionType,
    val confidence: Float
)