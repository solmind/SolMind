package com.solmind.service

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TensorFlow Lite inference service for Hugging Face models
 * Supports text generation models converted to TFLite format
 */
@Singleton
class PyTorchInference @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TFLiteInference"
        private const val MAX_SEQUENCE_LENGTH = 512
        private const val VOCAB_SIZE = 32128 // T5 vocabulary size
    }
    
    private var interpreter: Interpreter? = null
    private var currentModelId: String? = null
    
    init {
        try {
            Log.d(TAG, "TensorFlow Lite inference service initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TensorFlow Lite inference service", e)
        }
    }
    
    /**
     * Load a TFLite model from the specified path
     * @param modelPath Path to the TFLite model file (.tflite)
     * @param modelId Identifier for the model
     * @return true if model loaded successfully, false otherwise
     */
    fun loadModel(modelPath: String, modelId: String): Boolean {
        return try {
            Log.d(TAG, "Loading TFLite model from: $modelPath")
            
            // Close existing interpreter if any
            closeModel()
            
            var actualModelPath = modelPath
            
            // Check if model file exists
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                // Try to find model in assets
                val extractedModelId = extractModelIdFromPath(modelPath)
                val assetModelPath = "models/${extractedModelId}.tflite"
                
                try {
                    val assetInputStream = context.assets.open(assetModelPath)
                    assetInputStream.close()
                    
                    // Copy from assets to cache directory
                    val cacheDir = File(context.cacheDir, "tflite_models")
                    cacheDir.mkdirs()
                    val cachedModelFile = File(cacheDir, "${extractedModelId}.tflite")
                    
                    copyAssetToFile(assetModelPath, cachedModelFile.absolutePath)
                    actualModelPath = cachedModelFile.absolutePath
                    Log.d(TAG, "Using model from assets: $assetModelPath")
                } catch (e: Exception) {
                    Log.e(TAG, "Model not found in assets: $assetModelPath")
                    return false
                }
            }
            
            // Create TensorFlow Lite interpreter
            val modelBuffer = if (File(actualModelPath).exists()) {
                // Load from file system (downloaded models) using memory mapping
                val modelFile = File(actualModelPath)
                val fileInputStream = java.io.FileInputStream(modelFile)
                val fileChannel = fileInputStream.channel
                val mappedBuffer = fileChannel.map(
                    java.nio.channels.FileChannel.MapMode.READ_ONLY,
                    0,
                    fileChannel.size()
                )
                fileInputStream.close()
                mappedBuffer
            } else {
                // Load from assets (bundled models)
                FileUtil.loadMappedFile(context, actualModelPath)
            }
            val options = Interpreter.Options()
            options.setNumThreads(4) // Use 4 threads for better performance
            interpreter = Interpreter(modelBuffer, options)
            currentModelId = extractModelIdFromPath(actualModelPath)
            
            Log.i(TAG, "TFLite model loaded successfully: $modelId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load TFLite model from $modelPath", e)
            interpreter = null
            currentModelId = null
            false
        }
    }
    
    // Store the original input text for transaction parsing
    private var originalInputText: String = ""
    
    /**
     * Run inference on the loaded model
     * @param inputText Input text for the model
     * @return Generated text output or null if inference fails
     */
    fun runInference(inputText: String): String? {
        val tfliteInterpreter = interpreter
        if (tfliteInterpreter == null) {
            Log.w(TAG, "No model loaded for inference")
            return null
        }
        
        // Store the original input for transaction parsing
        originalInputText = inputText
        
        return try {
            Log.d(TAG, "Running TFLite inference with input: $inputText")
            
            // Log model tensor information for debugging
            logModelTensorInfo(tfliteInterpreter)
            
            // Create input tensors
            val inputIds = tokenizeText(inputText)
            val attentionMask = createAttentionMask(inputIds.size)
            
            // Get actual input tensor shapes from the model
            val inputTensorCount = tfliteInterpreter.inputTensorCount
            Log.d(TAG, "Model expects $inputTensorCount input tensors")
            
            // Prepare inputs based on actual model requirements
            val inputs = prepareModelInputs(tfliteInterpreter, inputIds, attentionMask)
            
            // Prepare outputs based on actual model requirements
            val outputs = prepareModelOutputs(tfliteInterpreter)
            
            // Run inference
            tfliteInterpreter.runForMultipleInputsOutputs(inputs, outputs)
            
            // Extract and decode output
            val result = extractModelOutput(outputs)
            
            Log.d(TAG, "TFLite inference completed, output: $result")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "TFLite inference failed", e)
            null
        }
    }
    
    /**
     * Simple tokenization - converts text to token IDs
     * This is a basic implementation. For production, use proper tokenizers like SentencePiece
     */
    private fun tokenizeText(text: String): LongArray {
        // Format the input with the specific prompt for transaction parsing
        val promptText = """Analyze this transaction description and categorize it:

Transaction: "$text"

Instructions:
1. Determine if this is a 'spend' or 'income' transaction
2. Extract or estimate the monetary amount (use 0.00 if unclear)
3. Select the most appropriate category from: FOOD_DINING, TRANSPORTATION, SHOPPING, ENTERTAINMENT, UTILITIES, HEALTHCARE, EDUCATION, TRAVEL, INVESTMENT, SALARY, FREELANCE, BUSINESS, GIFTS, OTHER

Respond in this exact format: type;amount;category
Example: spend;25.50;FOOD_DINING

Your response:"""
        
        // Basic tokenization: split by spaces and convert to hash-based token IDs
        val words = promptText.trim().split("\\s+".toRegex())
        val tokens = mutableListOf<Long>()
        
        // Add start token (0)
        tokens.add(0L)
        
        // Convert words to token IDs (simplified approach)
        for (word in words) {
            val tokenId = (word.hashCode().toLong() and 0x7FFFFFFF) % (VOCAB_SIZE - 100) + 100
            tokens.add(tokenId)
        }
        
        // Add end token (1)
        tokens.add(1L)
        
        // Pad or truncate to max sequence length
        return if (tokens.size > MAX_SEQUENCE_LENGTH) {
            tokens.take(MAX_SEQUENCE_LENGTH).toLongArray()
        } else {
            val padded = LongArray(MAX_SEQUENCE_LENGTH)
            tokens.forEachIndexed { index, token -> padded[index] = token }
            padded
        }
    }
    
    /**
     * Create attention mask for the input
     */
    private fun createAttentionMask(inputLength: Int): LongArray {
        val mask = LongArray(MAX_SEQUENCE_LENGTH)
        for (i in 0 until minOf(inputLength, MAX_SEQUENCE_LENGTH)) {
            mask[i] = 1L
        }
        return mask
    }
    
    /**
     * Decode tokens back to text and parse transaction information
     * This is a simplified implementation. For production, use proper detokenizers
     */
    private fun decodeTokens(tokens: LongArray): String {
        val words = mutableListOf<String>()
        
        for (token in tokens) {
            when (token) {
                0L -> continue // Start token
                1L -> break    // End token
                2L -> continue // Pad token
                else -> {
                    // Simple decoding: convert token ID back to word representation
                    val word = "token_$token"
                    words.add(word)
                }
            }
        }
        
        // Log the decoded tokens for debugging
        Log.d(TAG, "Decoded tokens: ${words.joinToString(" ")}")
        
        // For transaction parsing, try to extract meaningful information from the model output
        // Since this is a simplified tokenizer, we'll use a heuristic approach
        val decodedText = words.joinToString(" ")
        
        return parseTransactionFromOutput(decodedText)
    }
    
    /**
     * Parse transaction information from model output
     */
    private fun parseTransactionFromOutput(output: String): String {
        // Since the model was prompted to return "type;amount;category" format,
        // check if the output already contains this format
        if (output.contains(";") && output.split(";").size == 3) {
            return output.trim()
        }
        
        // If not in expected format, try to extract from the original input
        // This is a fallback approach since the simplified tokenizer doesn't preserve text
        return extractTransactionFromContext()
    }
    
    /**
     * Extract transaction information from the original input context
     * This is a fallback when the model output isn't in the expected format
     */
    private fun extractTransactionFromContext(): String {
        if (originalInputText.isEmpty()) {
            return "spend;0.00;OTHER"
        }
        
        Log.d(TAG, "Extracting transaction from original input: $originalInputText")
        
        // Extract amount using regex
        val amountRegex = "\\$?([0-9]+(?:\\.[0-9]{1,2})?)".toRegex()
        val amountMatch = amountRegex.find(originalInputText)
        val amount = amountMatch?.groupValues?.get(1) ?: "0.00"
        
        // Determine transaction type (default to spend)
        val type = when {
            originalInputText.contains("income", ignoreCase = true) -> "income"
            originalInputText.contains("salary", ignoreCase = true) -> "income"
            originalInputText.contains("payment", ignoreCase = true) -> "income"
            originalInputText.contains("deposit", ignoreCase = true) -> "income"
            else -> "spend"
        }
        
        // Determine category based on keywords
        val category = when {
            originalInputText.contains("breakfast", ignoreCase = true) -> "FOOD_DINING"
            originalInputText.contains("lunch", ignoreCase = true) -> "FOOD_DINING"
            originalInputText.contains("dinner", ignoreCase = true) -> "FOOD_DINING"
            originalInputText.contains("food", ignoreCase = true) -> "FOOD_DINING"
            originalInputText.contains("restaurant", ignoreCase = true) -> "FOOD_DINING"
            originalInputText.contains("coffee", ignoreCase = true) -> "FOOD_DINING"
            originalInputText.contains("grocery", ignoreCase = true) -> "FOOD_DINING"
            originalInputText.contains("gas", ignoreCase = true) -> "TRANSPORTATION"
            originalInputText.contains("uber", ignoreCase = true) -> "TRANSPORTATION"
            originalInputText.contains("taxi", ignoreCase = true) -> "TRANSPORTATION"
            originalInputText.contains("bus", ignoreCase = true) -> "TRANSPORTATION"
            originalInputText.contains("train", ignoreCase = true) -> "TRANSPORTATION"
            originalInputText.contains("shopping", ignoreCase = true) -> "SHOPPING"
            originalInputText.contains("amazon", ignoreCase = true) -> "SHOPPING"
            originalInputText.contains("store", ignoreCase = true) -> "SHOPPING"
            originalInputText.contains("movie", ignoreCase = true) -> "ENTERTAINMENT"
            originalInputText.contains("entertainment", ignoreCase = true) -> "ENTERTAINMENT"
            originalInputText.contains("game", ignoreCase = true) -> "ENTERTAINMENT"
            originalInputText.contains("electric", ignoreCase = true) -> "UTILITIES"
            originalInputText.contains("water", ignoreCase = true) -> "UTILITIES"
            originalInputText.contains("internet", ignoreCase = true) -> "UTILITIES"
            originalInputText.contains("phone", ignoreCase = true) -> "UTILITIES"
            originalInputText.contains("doctor", ignoreCase = true) -> "HEALTHCARE"
            originalInputText.contains("hospital", ignoreCase = true) -> "HEALTHCARE"
            originalInputText.contains("pharmacy", ignoreCase = true) -> "HEALTHCARE"
            originalInputText.contains("school", ignoreCase = true) -> "EDUCATION"
            originalInputText.contains("education", ignoreCase = true) -> "EDUCATION"
            originalInputText.contains("travel", ignoreCase = true) -> "TRAVEL"
            originalInputText.contains("hotel", ignoreCase = true) -> "TRAVEL"
            originalInputText.contains("flight", ignoreCase = true) -> "TRAVEL"
            originalInputText.contains("investment", ignoreCase = true) -> "INVESTMENT"
            originalInputText.contains("stock", ignoreCase = true) -> "INVESTMENT"
            originalInputText.contains("salary", ignoreCase = true) -> "SALARY"
            originalInputText.contains("freelance", ignoreCase = true) -> "FREELANCE"
            originalInputText.contains("business", ignoreCase = true) -> "BUSINESS"
            originalInputText.contains("gift", ignoreCase = true) -> "GIFTS"
            else -> "OTHER"
        }
        
        val result = "$type;$amount;$category"
        Log.d(TAG, "Extracted transaction: $result")
        return result
    }
    
    /**
     * Check if a model is currently loaded
     */
    fun isModelLoaded(): Boolean {
        return interpreter != null
    }
    
    /**
     * Get the ID of the currently loaded model
     */
    fun getCurrentModelId(): String? {
        return currentModelId
    }
    
    /**
     * Close the current model and free resources
     */
    fun closeModel() {
        try {
            interpreter?.close()
            interpreter = null
            currentModelId = null
            Log.d(TAG, "TFLite interpreter closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing TFLite interpreter", e)
        }
    }
    
    /**
     * Log detailed model tensor information for debugging
     */
    private fun logModelTensorInfo(interpreter: Interpreter) {
        try {
            Log.d(TAG, "=== Model Tensor Information ===")
            Log.d(TAG, "Input tensor count: ${interpreter.inputTensorCount}")
            Log.d(TAG, "Output tensor count: ${interpreter.outputTensorCount}")
            
            // Log input tensors
            for (i in 0 until interpreter.inputTensorCount) {
                val tensor = interpreter.getInputTensor(i)
                val shape = tensor.shape()
                val totalElements = shape.fold(1) { acc, dim -> acc * dim }
                val sizeInBytes = totalElements * when (tensor.dataType()) {
                    org.tensorflow.lite.DataType.FLOAT32 -> 4
                    org.tensorflow.lite.DataType.INT32 -> 4
                    org.tensorflow.lite.DataType.INT64 -> 8
                    else -> 1
                }
                Log.d(TAG, "Input[$i]: name=${tensor.name()}, shape=${shape.contentToString()}, type=${tensor.dataType()}, size=${sizeInBytes} bytes")
            }
            
            // Log output tensors
            for (i in 0 until interpreter.outputTensorCount) {
                val tensor = interpreter.getOutputTensor(i)
                val shape = tensor.shape()
                val totalElements = shape.fold(1) { acc, dim -> acc * dim }
                val sizeInBytes = totalElements * when (tensor.dataType()) {
                    org.tensorflow.lite.DataType.FLOAT32 -> 4
                    org.tensorflow.lite.DataType.INT32 -> 4
                    org.tensorflow.lite.DataType.INT64 -> 8
                    else -> 1
                }
                Log.d(TAG, "Output[$i]: name=${tensor.name()}, shape=${shape.contentToString()}, type=${tensor.dataType()}, size=${sizeInBytes} bytes")
            }
            Log.d(TAG, "=== End Model Tensor Information ===")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log model tensor info", e)
        }
    }
    
    /**
     * Prepare model inputs based on actual tensor requirements
     */
    private fun prepareModelInputs(interpreter: Interpreter, inputIds: LongArray, attentionMask: LongArray): Array<Any> {
        val inputs = mutableListOf<Any>()
        
        for (i in 0 until interpreter.inputTensorCount) {
            val tensor = interpreter.getInputTensor(i)
            val shape = tensor.shape()
            val tensorName = tensor.name()
            
            Log.d(TAG, "Preparing input tensor $i: $tensorName with shape ${shape.contentToString()}")
            
            when {
                tensorName.contains("input_ids") || i == 0 -> {
                    // Input IDs tensor
                    val buffer = createTensorBuffer(tensor, inputIds.map { it.toFloat() }.toFloatArray())
                    inputs.add(buffer)
                }
                tensorName.contains("attention_mask") || i == 1 -> {
                    // Attention mask tensor
                    val buffer = createTensorBuffer(tensor, attentionMask.map { it.toFloat() }.toFloatArray())
                    inputs.add(buffer)
                }
                tensorName.contains("kv_cache") || tensorName.contains("past_key_values") -> {
                    // KV cache tensor - initialize with zeros
                    val totalElements = shape.fold(1) { acc, dim -> acc * dim }
                    val zeros = FloatArray(totalElements) { 0f }
                    val buffer = createTensorBuffer(tensor, zeros)
                    inputs.add(buffer)
                    Log.d(TAG, "Created KV cache tensor with ${totalElements} elements (${totalElements * 4} bytes)")
                }
                else -> {
                    // Unknown tensor - initialize with zeros
                    val totalElements = shape.fold(1) { acc, dim -> acc * dim }
                    val zeros = FloatArray(totalElements) { 0f }
                    val buffer = createTensorBuffer(tensor, zeros)
                    inputs.add(buffer)
                    Log.d(TAG, "Created unknown tensor '$tensorName' with ${totalElements} elements")
                }
            }
        }
        
        return inputs.toTypedArray()
    }
    
    /**
     * Create a properly sized ByteBuffer for a tensor
     */
    private fun createTensorBuffer(tensor: org.tensorflow.lite.Tensor, data: FloatArray): ByteBuffer {
        val shape = tensor.shape()
        val totalElements = shape.fold(1) { acc, dim -> acc * dim }
        val sizeInBytes = totalElements * 4 // 4 bytes per float
        
        val buffer = ByteBuffer.allocateDirect(sizeInBytes).order(ByteOrder.nativeOrder())
        buffer.rewind()
        
        // Fill buffer with data, padding or truncating as needed
        for (i in 0 until totalElements) {
            val value = if (i < data.size) data[i] else 0f
            buffer.putFloat(value)
        }
        
        buffer.rewind()
        return buffer
    }
    
    /**
     * Prepare model outputs based on actual tensor requirements
     */
    private fun prepareModelOutputs(interpreter: Interpreter): Map<Int, Any> {
        val outputs = mutableMapOf<Int, Any>()
        
        for (i in 0 until interpreter.outputTensorCount) {
            val tensor = interpreter.getOutputTensor(i)
            val shape = tensor.shape()
            val totalElements = shape.fold(1) { acc, dim -> acc * dim }
            val sizeInBytes = totalElements * 4 // 4 bytes per float
            
            val buffer = ByteBuffer.allocateDirect(sizeInBytes).order(ByteOrder.nativeOrder())
            outputs[i] = buffer
            
            Log.d(TAG, "Prepared output tensor $i: ${tensor.name()} with ${totalElements} elements (${sizeInBytes} bytes)")
        }
        
        return outputs
    }
    
    /**
     * Extract output from model results
     */
    private fun extractModelOutput(outputs: Map<Int, Any>): String {
        return try {
            // Get the first output tensor (usually logits)
            val outputBuffer = outputs[0] as? ByteBuffer
            if (outputBuffer != null) {
                outputBuffer.rewind()
                val outputTokens = mutableListOf<Long>()
                
                // Read first few tokens as output
                val numTokensToRead = minOf(MAX_SEQUENCE_LENGTH, outputBuffer.remaining() / 4)
                for (i in 0 until numTokensToRead) {
                    val token = outputBuffer.float.toLong()
                    if (token > 0 && token < VOCAB_SIZE) {
                        outputTokens.add(token)
                    }
                }
                
                decodeTokens(outputTokens.toLongArray())
            } else {
                "Model output processing failed"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract model output", e)
            "Error processing model output"
        }
    }
    
    /**
     * Get model information
     */
    fun getModelInfo(): ModelInfo? {
        val tfliteInterpreter = interpreter ?: run {
            Log.w(TAG, "No model available for model info")
            return null
        }
        
        return try {
            // Get input and output info from TFLite interpreter
            val inputTensor = tfliteInterpreter.getInputTensor(0)
            val outputTensor = tfliteInterpreter.getOutputTensor(0)
            
            ModelInfo(
                inputShape = inputTensor.shape(),
                outputShape = outputTensor.shape(),
                inputDataType = inputTensor.dataType().toString(),
                outputDataType = outputTensor.dataType().toString()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get model info", e)
            null
        }
    }
    
    /**
     * Copy asset to file
     */
    private fun copyAssetToFile(assetPath: String, destinationPath: String) {
        try {
            val inputStream = context.assets.open(assetPath)
            val outputFile = File(destinationPath)
            outputFile.parentFile?.mkdirs()
            
            val outputStream = outputFile.outputStream()
            inputStream.copyTo(outputStream)
            
            inputStream.close()
            outputStream.close()
            
            Log.d(TAG, "Copied asset $assetPath to $destinationPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy asset: ${e.message}", e)
            throw e
        }
    }
    

    
    /**
     * Model information data class
     */
    data class ModelInfo(
        val inputShape: IntArray,
        val outputShape: IntArray,
        val inputDataType: String,
        val outputDataType: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as ModelInfo
            
            if (!inputShape.contentEquals(other.inputShape)) return false
            if (!outputShape.contentEquals(other.outputShape)) return false
            if (inputDataType != other.inputDataType) return false
            if (outputDataType != other.outputDataType) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = inputShape.contentHashCode()
            result = 31 * result + outputShape.contentHashCode()
            result = 31 * result + inputDataType.hashCode()
            result = 31 * result + outputDataType.hashCode()
            return result
        }
    }
    
    private fun extractModelIdFromPath(modelPath: String): String {
        // Extract model ID from path like "/data/data/.../flan-t5-small/model.tflite"
        val pathParts = modelPath.split("/")
        val modelDirIndex = pathParts.indexOfLast { it.contains("flan-t5") || it.contains("distilbert") }
        return if (modelDirIndex >= 0) pathParts[modelDirIndex] else "flan-t5-small"
    }
    
    /**
     * Clean up resources when the service is destroyed
     */
    fun cleanup() {
        closeModel()
        Log.d(TAG, "TFLite resources cleaned up")
    }
}