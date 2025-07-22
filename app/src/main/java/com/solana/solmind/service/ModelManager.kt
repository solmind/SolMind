package com.solana.solmind.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class LanguageModel(
    val id: String,
    val name: String,
    val description: String,
    val size: String,
    val isDefault: Boolean = false,
    val downloadUrl: String = "",
    val isLocal: Boolean = true,
    val requiresSubscription: Boolean = false
)

data class ModelConfig(
    val id: String,
    val name: String,
    val description: String,
    val huggingFaceId: String,
    val fileName: String,
    val isLocal: Boolean = true,
    val requiresSubscription: Boolean = false,
    val preConvertedRepo: String? = null // Optional pre-converted TFLite model repository
)

enum class ModelDownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    ERROR
}

data class ModelState(
    val model: LanguageModel,
    val status: ModelDownloadStatus,
    val progress: Float = 0f,
    val error: String? = null
)

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val huggingFaceDownloadManager: HuggingFaceDownloadManager,
    private val huggingFaceConfig: HuggingFaceConfig
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("model_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    init {
        // Load model sizes from API in the background
        scope.launch {
            loadAvailableModels()
        }
    }
    
    // Cache for model sizes to avoid repeated API calls
    private val modelSizeCache = mutableMapOf<String, String>()
    
    // StateFlow for available models with dynamic sizes
    private val _availableModels = MutableStateFlow<List<LanguageModel>>(emptyList())
    val availableModels: StateFlow<List<LanguageModel>> = _availableModels.asStateFlow()
    
    private val _selectedModel = MutableStateFlow<LanguageModel?>(null)
    val selectedModel: StateFlow<LanguageModel?> = _selectedModel.asStateFlow()
    
    private val _modelStates = MutableStateFlow<List<ModelState>>(emptyList())
    val modelStates: StateFlow<List<ModelState>> = _modelStates.asStateFlow()
    
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()
    
    companion object {
        private const val SELECTED_MODEL_KEY = "selected_model_id"
        private const val MODEL_DOWNLOADED_PREFIX = "model_downloaded_"
        
        // Base model configurations for TensorFlow Lite models
        private val BASE_MODEL_CONFIGS = listOf(
            ModelConfig(
                id = "smolvlm-256m-instruct",
                name = "SmolVLM 256M Instruct",
                description = "Compact vision-language model for multimodal understanding",
                huggingFaceId = "HuggingFaceTB/SmolVLM-256M-Instruct",
                preConvertedRepo = "litert-community/SmolVLM-256M-Instruct",
                fileName = "smalvlm-256m-instruct_q8_ekv2048.tflite",
                isLocal = true
            ),
            ModelConfig(
                id = "tinyllama-1.1b-chat",
                name = "TinyLlama 1.1B Chat",
                description = "Compact 1.1B parameter chat model optimized for LiteRT",
                huggingFaceId = "litert-community/TinyLlama-1.1B-Chat",
                fileName = "model.tflite",
                isLocal = true
            ),
            ModelConfig(
                id = "gemma3-1b-it",
                name = "Gemma3 1B IT",
                description = "Google's Gemma 3 1B instruction-tuned model optimized for LiteRT",
                huggingFaceId = "litert-community/Gemma3-1B-IT",
                fileName = "model.tflite",
                isLocal = true
            ),
            ModelConfig(
                id = "phi-4-mini-instruct",
                name = "Phi-4 Mini Instruct",
                description = "Microsoft's Phi-4 mini instruction model for LiteRT deployment",
                huggingFaceId = "litert-community/Phi-4-mini-instruct",
                fileName = "model.tflite",
                isLocal = true
            ),
            ModelConfig(
                id = "smollm-1.7b",
                name = "SmolLM 1.7B",
                description = "HuggingFace's SmolLM 1.7B - compact and efficient language model",
                huggingFaceId = "litert-community/SmolLM-1.7B",
                fileName = "model.tflite",
                isLocal = true
            ),
            ModelConfig(
                id = "solmind-cloud",
                name = "SolMind Cloud AI",
                description = "Advanced cloud-based AI model with superior accuracy",
                huggingFaceId = "",
                fileName = "",
                isLocal = false,
                requiresSubscription = true
            )
        )
    }
    
    /**
     * Get model size from Hugging Face API
     */
    private suspend fun getModelSize(huggingFaceId: String, fileName: String): String {
        // Define cache key at function scope
        val cacheKey = "$huggingFaceId/$fileName"
        
        return try {
            // Check cache first
            modelSizeCache[cacheKey]?.let { return it }
            
            // Skip for empty or specific IDs
            if (huggingFaceId.isEmpty() || huggingFaceId == "text_classification.tflite") {
                return "~50 MB"
            }
            
            Log.d("ModelManager", "Fetching model size for $huggingFaceId/$fileName")
            
            // Fetch from API
            val filesResult = huggingFaceDownloadManager.getModelFiles(
                modelId = huggingFaceId,
                authToken = huggingFaceConfig.getApiToken()
            )
            
            if (filesResult.isSuccess) {
                val files = filesResult.getOrNull() ?: emptyList()
                Log.d("ModelManager", "Found ${files.size} files for $huggingFaceId")
                
                // Try multiple common file names for LiteRT models
                val possibleFileNames = listOf(
                    fileName,
                    "model.tflite",
                    "tf_model.tflite",
                    "pytorch_model.bin",
                    "model.safetensors",
                    "model.onnx"
                )
                
                var targetFile: Any? = null
                var foundFileName = ""
                
                for (possibleName in possibleFileNames) {
                    val file = files.find { it.path == possibleName }
                    if (file != null) {
                        targetFile = file
                        foundFileName = possibleName
                        Log.d("ModelManager", "Found file: $foundFileName")
                        break
                    }
                }
                
                val sizeString = when {
                    targetFile != null -> {
                        val size = try {
                            val sizeField = targetFile::class.java.getDeclaredField("size")
                            sizeField.isAccessible = true
                            sizeField.get(targetFile) as? Long
                        } catch (e: Exception) {
                            try {
                                val lfsField = targetFile::class.java.getDeclaredField("lfs")
                                lfsField.isAccessible = true
                                val lfs = lfsField.get(targetFile)
                                if (lfs != null) {
                                    val lfsSize = lfs::class.java.getDeclaredField("size")
                                    lfsSize.isAccessible = true
                                    lfsSize.get(lfs) as? Long
                                } else null
                            } catch (e2: Exception) {
                                null
                            }
                        }
                        
                        if (size != null && size > 0) {
                            formatFileSize(size)
                        } else {
                            // Provide estimated sizes for known LiteRT models
                            when {
                                huggingFaceId.contains("tinyllama", ignoreCase = true) -> "~2.2 GB"
                                huggingFaceId.contains("gemma3-1b", ignoreCase = true) -> "~2.5 GB"
                                huggingFaceId.contains("phi-4-mini", ignoreCase = true) -> "~7.4 GB"
                                huggingFaceId.contains("smollm-1.7b", ignoreCase = true) -> "~3.4 GB"
                                else -> "~1-5 GB"
                            }
                        }
                    }
                    else -> {
                        Log.w("ModelManager", "No suitable file found for $huggingFaceId. Available files: ${files.map { it.path }}")
                        // Provide estimated sizes for known LiteRT models
                        when {
                            huggingFaceId.contains("tinyllama", ignoreCase = true) -> "~2.2 GB"
                            huggingFaceId.contains("gemma3-1b", ignoreCase = true) -> "~2.5 GB"
                            huggingFaceId.contains("phi-4-mini", ignoreCase = true) -> "~7.4 GB"
                            huggingFaceId.contains("smollm-1.7b", ignoreCase = true) -> "~3.4 GB"
                            else -> "~1-5 GB"
                        }
                    }
                }
                
                // Cache the result
                modelSizeCache[cacheKey] = sizeString
                Log.d("ModelManager", "Model size for $huggingFaceId: $sizeString")
                sizeString
            } else {
                Log.w("ModelManager", "Failed to fetch files for $huggingFaceId: ${filesResult.exceptionOrNull()?.message}")
                // Provide estimated sizes for known LiteRT models as fallback
                val estimatedSize = when {
                    huggingFaceId.contains("tinyllama", ignoreCase = true) -> "~2.2 GB"
                    huggingFaceId.contains("gemma3-1b", ignoreCase = true) -> "~2.5 GB"
                    huggingFaceId.contains("phi-4-mini", ignoreCase = true) -> "~7.4 GB"
                    huggingFaceId.contains("smollm-1.7b", ignoreCase = true) -> "~3.4 GB"
                    else -> "~1-5 GB"
                }
                modelSizeCache[cacheKey] = estimatedSize
                estimatedSize
            }
        } catch (e: Exception) {
            Log.e("ModelManager", "Failed to get model size for $huggingFaceId: ${e.message}", e)
            // Provide estimated sizes for known LiteRT models as fallback
            val estimatedSize = when {
                huggingFaceId.contains("tinyllama", ignoreCase = true) -> "~2.2 GB"
                huggingFaceId.contains("gemma3-1b", ignoreCase = true) -> "~2.5 GB"
                huggingFaceId.contains("phi-4-mini", ignoreCase = true) -> "~7.4 GB"
                huggingFaceId.contains("smollm-1.7b", ignoreCase = true) -> "~3.4 GB"
                else -> "~1-5 GB"
            }
            modelSizeCache[cacheKey] = estimatedSize
            estimatedSize
        }
    }
    
    /**
     * Format file size in human-readable format
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.0f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
    
    /**
     * Load available models without fetching sizes (sizes loaded on demand)
     */
    suspend fun loadAvailableModels() {
        withContext(Dispatchers.IO) {
            val models = BASE_MODEL_CONFIGS.map { config ->
                val downloadUrl = if (config.isLocal && config.huggingFaceId.isNotEmpty()) {
                    "https://huggingface.co/${config.huggingFaceId}/resolve/main/${config.fileName}"
                } else {
                    ""
                }
                
                LanguageModel(
                    id = config.id,
                    name = config.name,
                    description = config.description,
                    size = if (config.isLocal) "Loading..." else "Cloud",
                    downloadUrl = downloadUrl,
                    isLocal = config.isLocal,
                    requiresSubscription = config.requiresSubscription
                )
            }
            
            _availableModels.value = models
            
            // Initialize selected model if not set
            if (_selectedModel.value == null) {
                _selectedModel.value = getDefaultModel()
            }
            
            // Update model states with new model information
            updateModelStates()
        }
    }
    
    /**
     * Load model sizes dynamically when needed (e.g., when showing model list)
     */
    suspend fun loadModelSizes() {
        withContext(Dispatchers.IO) {
            val updatedModels = _availableModels.value.map { model ->
                if (model.isLocal && model.size == "Loading...") {
                    val config = BASE_MODEL_CONFIGS.find { it.id == model.id }
                    val size = if (config != null && config.huggingFaceId.isNotEmpty()) {
                        getModelSize(config.huggingFaceId, config.fileName)
                    } else {
                        "Unknown"
                    }
                    model.copy(size = size)
                } else {
                    model
                }
            }
            
            _availableModels.value = updatedModels
            updateModelStates()
        }
    }
    
    /**
     * Update model states based on current available models
     */
    private fun updateModelStates() {
        val updatedStates = getAvailableModels().map { model ->
            val isDownloaded = isModelDownloaded(model.id)
            val existingState = _modelStates.value.find { it.model.id == model.id }
            
            ModelState(
                model = model,
                status = existingState?.status ?: if (isDownloaded) ModelDownloadStatus.DOWNLOADED else ModelDownloadStatus.NOT_DOWNLOADED,
                progress = existingState?.progress ?: 0f,
                error = existingState?.error
            )
        }
        _modelStates.value = updatedStates
    }
    
    private fun getAvailableModels(): List<LanguageModel> {
        return _availableModels.value.ifEmpty {
            // Return base models with placeholder sizes if not loaded yet
            BASE_MODEL_CONFIGS.map { config ->
                val downloadUrl = if (config.isLocal && config.huggingFaceId.isNotEmpty()) {
                    "https://huggingface.co/${config.huggingFaceId}/resolve/main/${config.fileName}"
                } else {
                    ""
                }
                
                LanguageModel(
                    id = config.id,
                    name = config.name,
                    description = config.description,
                    size = if (config.isLocal) "Loading..." else "Cloud",
                    downloadUrl = downloadUrl,
                    isLocal = config.isLocal,
                    requiresSubscription = config.requiresSubscription
                )
            }
        }
    }
    
    private fun getDefaultModel(): LanguageModel? {
        val savedModelId = prefs.getString(SELECTED_MODEL_KEY, "smolvlm-256m-instruct")
        val availableModels = getAvailableModels()
        return if (availableModels.isNotEmpty()) {
            availableModels.find { it.id == savedModelId } ?: availableModels.first()
        } else {
            null
        }
    }
    
    private fun getInitialModelStates(): List<ModelState> {
        return getAvailableModels().map { model ->
            val isDownloaded = isModelDownloaded(model.id)
            ModelState(
                model = model,
                status = if (isDownloaded) ModelDownloadStatus.DOWNLOADED else ModelDownloadStatus.NOT_DOWNLOADED
            )
        }
    }
    
    fun selectModel(model: LanguageModel?) {
        _selectedModel.value = model
        model?.let {
            prefs.edit().putString(SELECTED_MODEL_KEY, it.id).apply()
        }
    }
    
    fun isModelDownloaded(modelId: String): Boolean {
        // Get the correct file name for this model
        val config = BASE_MODEL_CONFIGS.find { it.id == modelId }
        val fileName = config?.fileName ?: "model.tflite"
        
        // Use HuggingFaceDownloadManager to check if model exists
        val isFileDownloaded = huggingFaceDownloadManager.isModelDownloaded(modelId, fileName)
        val isMarkedDownloaded = prefs.getBoolean(MODEL_DOWNLOADED_PREFIX + modelId, false)
        return isFileDownloaded && isMarkedDownloaded
    }
    
    suspend fun downloadModel(modelId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Find the model configuration
                val model = getAvailableModels().find { it.id == modelId }
                    ?: return@withContext Result.failure(Exception("Model not found: $modelId"))
                
                val config = BASE_MODEL_CONFIGS.find { it.id == modelId }
                    ?: return@withContext Result.failure(Exception("Model config not found: $modelId"))
                
                // Update status to downloading
                updateModelStatus(modelId, ModelDownloadStatus.DOWNLOADING)
                
                val fileName = config.fileName
                var downloadUrl: String
                var downloadSuccess = false
                
                // First, try to download from pre-converted repository if available
                if (!config.preConvertedRepo.isNullOrEmpty()) {
                    try {
                        downloadUrl = "https://huggingface.co/${config.preConvertedRepo}/resolve/main/$fileName"
                        Log.d("ModelManager", "Attempting to download pre-converted model from: $downloadUrl")
                        
                        huggingFaceDownloadManager.downloadModelWithProgress(
                            modelId = modelId,
                            downloadUrl = downloadUrl,
                            fileName = fileName,
                            authToken = huggingFaceConfig.getApiToken()
                        ).collect { progress ->
                            updateDownloadProgress(modelId, progress.percentage / 100f)
                        }
                        
                        downloadSuccess = true
                        Log.d("ModelManager", "Successfully downloaded pre-converted model for $modelId")
                    } catch (e: Exception) {
                        Log.w("ModelManager", "Pre-converted model not available for $modelId, will try conversion: ${e.message}")
                        downloadSuccess = false
                    }
                }
                
                // If pre-converted download failed, fallback to original model (requires conversion)
                if (!downloadSuccess) {
                    if (model.downloadUrl.isEmpty()) {
                        return@withContext Result.failure(Exception("No download URL available for model: $modelId"))
                    }
                    
                    Log.d("ModelManager", "Downloading original model for conversion: ${model.downloadUrl}")
                    
                    huggingFaceDownloadManager.downloadModelWithProgress(
                        modelId = modelId,
                        downloadUrl = model.downloadUrl,
                        fileName = fileName,
                        authToken = huggingFaceConfig.getApiToken()
                    ).collect { progress ->
                        updateDownloadProgress(modelId, progress.percentage / 100f)
                    }
                    
                    Log.d("ModelManager", "Downloaded original model for $modelId - conversion may be required")
                }
                
                // Mark as downloaded in preferences
                prefs.edit().putBoolean(MODEL_DOWNLOADED_PREFIX + modelId, true).apply()
                updateModelStatus(modelId, ModelDownloadStatus.DOWNLOADED)
                
                Result.success(Unit)
            } catch (e: Exception) {
                updateModelStatus(modelId, ModelDownloadStatus.ERROR, e.message)
                Result.failure(e)
            }
        }
    }
    
    suspend fun deleteModel(modelId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Don't allow deleting the currently selected model
                if (_selectedModel.value?.id == modelId) {
                    return@withContext Result.failure(Exception("Cannot delete currently selected model"))
                }
                
                // Use HuggingFaceDownloadManager to delete model files
                val deleteResult = huggingFaceDownloadManager.deleteModel(modelId)
                if (deleteResult.isFailure) {
                    return@withContext deleteResult
                }
                
                // Update preferences
                prefs.edit().remove(MODEL_DOWNLOADED_PREFIX + modelId).apply()
                updateModelStatus(modelId, ModelDownloadStatus.NOT_DOWNLOADED)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun updateModelStatus(modelId: String, status: ModelDownloadStatus, error: String? = null) {
        val currentStates = _modelStates.value.toMutableList()
        val index = currentStates.indexOfFirst { it.model.id == modelId }
        if (index != -1) {
            currentStates[index] = currentStates[index].copy(
                status = status,
                error = error,
                progress = if (status == ModelDownloadStatus.DOWNLOADED) 1f else 0f
            )
            _modelStates.value = currentStates
        }
    }
    
    private fun updateDownloadProgress(modelId: String, progress: Float) {
        val currentProgress = _downloadProgress.value.toMutableMap()
        currentProgress[modelId] = progress
        _downloadProgress.value = currentProgress
        
        // Also update model state progress
        val currentStates = _modelStates.value.toMutableList()
        val index = currentStates.indexOfFirst { it.model.id == modelId }
        if (index != -1) {
            currentStates[index] = currentStates[index].copy(progress = progress)
            _modelStates.value = currentStates
        }
    }
    
    fun getModelPath(modelId: String): String? {
        if (!isModelDownloaded(modelId)) return null
        val config = BASE_MODEL_CONFIGS.find { it.id == modelId }
        val fileName = config?.fileName ?: "model.tflite"
        return huggingFaceDownloadManager.getModelPath(modelId, fileName)
    }
    
    fun getAvailableModelsList(): List<LanguageModel> = getAvailableModels()
    
    /**
     * Get model information from Hugging Face Hub
     */
    suspend fun getModelInfoFromHub(modelId: String): Result<HuggingFaceModelInfo> {
        val modelRepo = huggingFaceConfig.getModelRepo(modelId)
            ?: return Result.failure(Exception("Unknown model repository for: $modelId"))
        
        return huggingFaceDownloadManager.getModelInfo(
            modelId = modelRepo,
            authToken = huggingFaceConfig.getApiToken()
        )
    }
    
    /**
     * Get list of files in a model repository
     */
    suspend fun getModelFilesFromHub(modelId: String): Result<List<HuggingFaceFileInfo>> {
        val modelRepo = huggingFaceConfig.getModelRepo(modelId)
            ?: return Result.failure(Exception("Unknown model repository for: $modelId"))
        
        return huggingFaceDownloadManager.getModelFiles(
            modelId = modelRepo,
            authToken = huggingFaceConfig.getApiToken()
        )
    }
    
    /**
     * Check if Hugging Face configuration is valid
     */
    fun isHuggingFaceConfigValid(): Boolean {
        return huggingFaceConfig.isConfigValid()
    }
    
    /**
     * Set Hugging Face API token
     */
    fun setHuggingFaceApiToken(token: String?) {
        huggingFaceConfig.setApiToken(token)
    }
    
    /**
     * Enable or disable Hugging Face authentication
     */
    fun setHuggingFaceAuthEnabled(enabled: Boolean) {
        huggingFaceConfig.setAuthEnabled(enabled)
    }
}