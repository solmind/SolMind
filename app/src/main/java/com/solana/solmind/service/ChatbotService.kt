package com.solana.solmind.service

import com.solana.solmind.service.PyTorchInference

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatbotService @Inject constructor(
    private val context: Context,
    private val modelManager: ModelManager,
    private val pyTorchInference: PyTorchInference
) {
    private val maxSequenceLength = 512
    private val vocabularySize = 32128 // FLAN-T5 vocabulary size
    
    // Enhanced tokenizer with more vocabulary
    private val vocabulary = mapOf(
        "<pad>" to 0, "<unk>" to 1, "<s>" to 2, "</s>" to 3,
        "hello" to 4, "hi" to 5, "how" to 6, "are" to 7, "you" to 8,
        "good" to 9, "fine" to 10, "thanks" to 11, "thank" to 12, "help" to 13,
        "what" to 14, "is" to 15, "this" to 16, "that" to 17, "can" to 18,
        "i" to 19, "me" to 20, "my" to 21, "your" to 22, "the" to 23,
        "transaction" to 24, "money" to 25, "amount" to 26, "date" to 27, "category" to 28,
        "expense" to 29, "income" to 30, "budget" to 31, "spending" to 32, "save" to 33,
        "bank" to 34, "account" to 35, "balance" to 36, "payment" to 37, "transfer" to 38,
        "food" to 39, "restaurant" to 40, "grocery" to 41, "gas" to 42, "fuel" to 43,
        "shopping" to 44, "entertainment" to 45, "travel" to 46, "hotel" to 47, "flight" to 48,
        "coffee" to 49, "lunch" to 50, "dinner" to 51, "breakfast" to 52, "snack" to 53,
        "uber" to 54, "taxi" to 55, "bus" to 56, "train" to 57, "parking" to 58,
        "amazon" to 59, "store" to 60, "market" to 61, "pharmacy" to 62, "doctor" to 63
    )
    
    // Reverse vocabulary for decoding
    private val reverseVocabulary = vocabulary.entries.associate { it.value to it.key }
    
    suspend fun generateResponse(userMessage: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "ChatbotService: Generating response for message: '$userMessage'")
                val selectedModel = modelManager.selectedModel.value
                
                if (selectedModel == null) {
                    Log.w(TAG, "ChatbotService: No model selected, using fallback response")
                    return@withContext generateFallbackResponse(userMessage)
                }
                
                Log.i(TAG, "ChatbotService: Selected model: ${selectedModel.name} (${selectedModel.id})")
                
                // Load model if not already loaded
                if (!pyTorchInference.isModelLoaded() || pyTorchInference.getCurrentModelId() != selectedModel.id) {
                    val modelPath = modelManager.getModelPath(selectedModel.id)
                    if (modelPath == null) {
                        Log.e(TAG, "ChatbotService: Model ${selectedModel.id} not downloaded or path not found, using fallback")
                        return@withContext generateFallbackResponse(userMessage)
                    }
                    Log.i(TAG, "ChatbotService: Loading model from path: $modelPath")
                    val loaded = pyTorchInference.loadModel(modelPath, selectedModel.id)
                    if (!loaded) {
                        Log.e(TAG, "ChatbotService: Failed to load model ${selectedModel.id}, using fallback")
                        return@withContext generateFallbackResponse(userMessage)
                    }
                    Log.i(TAG, "ChatbotService: Successfully loaded model ${selectedModel.id}")
                } else {
                    Log.d(TAG, "ChatbotService: Model ${selectedModel.id} already loaded")
                }
                
                // Generate response using the PyTorch inference service
                val response = generateModelResponse(userMessage)
                Log.i(TAG, "ChatbotService: Generated response: '$response'")
                response
                
            } catch (e: Exception) {
                Log.e(TAG, "ChatbotService: Exception during response generation", e)
                generateFallbackResponse(userMessage)
            }
        }
    }
    
    private suspend fun tokenizeText(text: String): IntArray {
        // Simple tokenization - split by spaces and map to vocabulary
        val words = text.lowercase().split("\\s+".toRegex())
        val tokens = mutableListOf<Int>()
        
        // Add start token
        tokens.add(vocabulary["<s>"] ?: 2)
        
        // Convert words to tokens
        for (word in words) {
            val token = vocabulary[word] ?: vocabulary["<unk>"] ?: 1
            tokens.add(token)
        }
        
        // Add end token
        tokens.add(vocabulary["</s>"] ?: 3)
        
        // Pad or truncate to maxSequenceLength
        val result = IntArray(maxSequenceLength)
        val copyLength = minOf(tokens.size, maxSequenceLength)
        
        for (i in 0 until copyLength) {
            result[i] = tokens[i]
        }
        
        // Fill remaining with padding tokens
        for (i in copyLength until maxSequenceLength) {
            result[i] = vocabulary["<pad>"] ?: 0
        }
        
        return result
    }
    
    private fun detokenizeText(tokens: IntArray): String {
        val words = mutableListOf<String>()
        
        for (token in tokens) {
            val word = reverseVocabulary[token]
            if (word != null && word != "<pad>" && word != "<s>" && word != "</s>") {
                words.add(word)
            }
        }
        
        return words.joinToString(" ")
    }
    
    private suspend fun generateModelResponse(userMessage: String): String {
        return try {
            if (!pyTorchInference.isModelLoaded()) {
                Log.w(TAG, "ChatbotService: Model not loaded for inference")
                return generateFallbackResponse(userMessage)
            }
            
            Log.d(TAG, "ChatbotService: Starting model inference")
            
            // Use PyTorchInference for inference
             val startTime = System.currentTimeMillis()
             val response = pyTorchInference.runInference(userMessage)
             val inferenceTime = System.currentTimeMillis() - startTime
             
             if (response != null) {
                 Log.d(TAG, "ChatbotService: Inference completed in ${inferenceTime}ms")
                 return response
             } else {
                 Log.w(TAG, "ChatbotService: Inference returned null, using fallback")
                 return generateFallbackResponse(userMessage)
             }

                
        } catch (e: Exception) {
            Log.e(TAG, "ChatbotService: Exception during model inference", e)
            generateFallbackResponse(userMessage)
        }
    }
    
    private fun cleanResponse(response: String): String {
        return response
            .replace("<pad>", "")
            .replace("</s>", "")
            .replace("<unk>", "")
            .trim()
            .takeIf { it.isNotBlank() } ?: "I'm here to help with your financial questions!"
    }
    
    private fun generateFallbackResponse(userMessage: String): String {
        Log.d(TAG, "ChatbotService: Generating fallback response for: '$userMessage'")
        val normalizedMessage = userMessage.lowercase().trim()
        
        return when {
            // Greeting patterns
            normalizedMessage.contains("hello") || normalizedMessage.contains("hi") ||
            normalizedMessage.contains("hey") -> {
                "Hello! I'm your SolMind financial assistant. How can I help you manage your finances today?"
            }
            
            // Transaction help
            normalizedMessage.contains("transaction") || normalizedMessage.contains("expense") ||
            normalizedMessage.contains("income") -> {
                "I can help you add and categorize transactions! Just describe what you spent or earned, and I'll help you record it. For example: 'I bought coffee for $4.50' or 'I received my salary of $3000'."
            }
            
            // Budget questions
            normalizedMessage.contains("budget") || normalizedMessage.contains("spending") -> {
                "Great question about budgeting! I can help you track your expenses by category. You can view your spending patterns in the dashboard and set up categories for better organization."
            }
            
            // Category questions
            normalizedMessage.contains("category") || normalizedMessage.contains("categories") -> {
                "I can categorize your transactions into: Food & Dining, Transportation, Shopping, Entertainment, Utilities, Healthcare, Education, Travel, Investment, Business, and more. Just describe your transaction and I'll suggest the right category!"
            }
            
            // Help with amounts
            normalizedMessage.contains("amount") || normalizedMessage.contains("money") ||
            normalizedMessage.contains("cost") || normalizedMessage.contains("price") -> {
                "I can extract amounts from your descriptions! Just tell me about your transaction like 'I paid $25 for groceries' and I'll automatically detect the amount and categorize it."
            }
            
            // AI model questions
            normalizedMessage.contains("model") || normalizedMessage.contains("ai") -> {
                "I'm powered by local AI models that run directly on your device for privacy. You can select different models in Settings > AI Settings > Language Model. Currently available models include FLAN-T5, DialoGPT, and DistilBERT."
            }
            
            // Privacy questions
            normalizedMessage.contains("privacy") || normalizedMessage.contains("data") -> {
                "Your privacy is important! All AI processing happens locally on your device. Your financial data never leaves your phone, and all models run offline for maximum security."
            }
            
            // General help
            normalizedMessage.contains("help") || normalizedMessage.contains("how") -> {
                "I'm here to help! I can:\n\n" +
                "💰 Add transactions from text descriptions\n" +
                "📊 Categorize expenses automatically\n" +
                "📱 Process receipt images\n" +
                "📈 Help track your spending\n\n" +
                "Just describe your transaction and I'll take care of the rest!"
            }
            
            // Thank you
            normalizedMessage.contains("thank") -> {
                "You're welcome! I'm always here to help with your financial management. Feel free to ask me anything!"
            }
            
            // Goodbye
            normalizedMessage.contains("bye") || normalizedMessage.contains("goodbye") -> {
                "Goodbye! Remember, I'm here whenever you need help managing your finances. Have a great day!"
            }
            
            // Default response
            else -> {
                "I'm your SolMind financial assistant! I can help you add transactions, categorize expenses, and manage your finances. Try describing a transaction like 'I bought lunch for $12' or ask me about budgeting tips!"
            }
        }
    }
    
    fun clearModel() {
        Log.i(TAG, "ChatbotService: Clearing model resources")
        pyTorchInference.closeModel()
    }
    
    companion object {
        private const val TAG = "ChatbotService"
    }
}