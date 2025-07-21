package com.solana.solmind.service

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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
    
    private val _selectedModel = MutableStateFlow(getDefaultModel())
    val selectedModel: StateFlow<LanguageModel> = _selectedModel.asStateFlow()
    
    private val _modelStates = MutableStateFlow(getInitialModelStates())
    val modelStates: StateFlow<List<ModelState>> = _modelStates.asStateFlow()
    
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()
    
    companion object {
        private const val SELECTED_MODEL_KEY = "selected_model_id"
        private const val MODEL_DOWNLOADED_PREFIX = "model_downloaded_"
        
        val AVAILABLE_MODELS = listOf(
            LanguageModel(
                id = "flan-t5-small",
                name = "FLAN-T5 Small",
                description = "Compact model for basic transaction parsing",
                size = "77 MB",
                downloadUrl = "https://huggingface.co/google/flan-t5-small/resolve/main/pytorch_model.bin",
                isLocal = true
            ),
            LanguageModel(
                id = "flan-t5-base",
                name = "FLAN-T5 Base",
                description = "Balanced model for general transaction analysis",
                size = "248 MB",
                downloadUrl = "https://huggingface.co/google/flan-t5-base/resolve/main/pytorch_model.bin",
                isLocal = true
            ),
            LanguageModel(
                id = "distilbert-base",
                name = "DistilBERT Base",
                description = "Fast and efficient model for transaction understanding",
                size = "268 MB",
                downloadUrl = "https://huggingface.co/distilbert-base-uncased/resolve/main/pytorch_model.bin",
                isLocal = true
            ),
            LanguageModel(
                id = "solmind-cloud",
                name = "SolMind Cloud AI",
                description = "Advanced cloud-based AI model with superior accuracy",
                size = "Cloud",
                downloadUrl = "",
                isLocal = false,
                requiresSubscription = true
            )
        )
    }
    
    private fun getAvailableModels(): List<LanguageModel> {
        return AVAILABLE_MODELS
    }
    
    private fun getDefaultModel(): LanguageModel {
        val savedModelId = prefs.getString(SELECTED_MODEL_KEY, "flan-t5-small")
        return getAvailableModels().find { it.id == savedModelId } ?: getAvailableModels().first()
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
    
    fun selectModel(model: LanguageModel) {
        _selectedModel.value = model
        prefs.edit().putString(SELECTED_MODEL_KEY, model.id).apply()
    }
    
    fun isModelDownloaded(modelId: String): Boolean {
        // Use HuggingFaceDownloadManager to check if model exists
        val isFileDownloaded = huggingFaceDownloadManager.isModelDownloaded(modelId, "pytorch_model.bin")
        val isMarkedDownloaded = prefs.getBoolean(MODEL_DOWNLOADED_PREFIX + modelId, false)
        return isFileDownloaded && isMarkedDownloaded
    }
    
    suspend fun downloadModel(modelId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Find the model configuration
                val model = getAvailableModels().find { it.id == modelId }
                    ?: return@withContext Result.failure(Exception("Model not found: $modelId"))
                
                if (model.downloadUrl.isEmpty()) {
                    return@withContext Result.failure(Exception("No download URL available for model: $modelId"))
                }
                
                // Update status to downloading
                updateModelStatus(modelId, ModelDownloadStatus.DOWNLOADING)
                
                // Use HuggingFaceDownloadManager to download with progress tracking
                huggingFaceDownloadManager.downloadModelWithProgress(
                    modelId = modelId,
                    downloadUrl = model.downloadUrl,
                    fileName = "pytorch_model.bin",
                    authToken = huggingFaceConfig.getApiToken()
                ).collect { progress ->
                    // Update download progress
                    updateDownloadProgress(modelId, progress.percentage / 100f)
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
                if (_selectedModel.value.id == modelId) {
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
        return huggingFaceDownloadManager.getModelPath(modelId, "pytorch_model.bin")
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