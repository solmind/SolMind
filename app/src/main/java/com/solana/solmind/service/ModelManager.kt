package com.solana.solmind.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class LanguageModel(
    val id: String,
    val name: String,
    val description: String,
    val size: String,
    val isDefault: Boolean = false
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
    private val context: Context
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
    }
    
    private fun getAvailableModels(): List<LanguageModel> {
        return listOf(
            LanguageModel(
                id = "google/flan-t5-small",
                name = "FLAN-T5 Small",
                description = "Google's instruction-tuned T5 model, optimized for mobile",
                size = "242 MB",
                isDefault = true
            ),
            LanguageModel(
                id = "microsoft/DialoGPT-small",
                name = "DialoGPT Small",
                description = "Microsoft's conversational AI model",
                size = "117 MB"
            ),
            LanguageModel(
                id = "distilbert-base-uncased",
                name = "DistilBERT Base",
                description = "Lightweight BERT model for text understanding",
                size = "268 MB"
            )
        )
    }
    
    private fun getDefaultModel(): LanguageModel {
        val savedModelId = prefs.getString(SELECTED_MODEL_KEY, "google/flan-t5-small")
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
        // Check if model file exists in app's internal storage
        val modelFile = File(context.filesDir, "models/$modelId/model.tflite")
        val isDownloaded = modelFile.exists() && prefs.getBoolean(MODEL_DOWNLOADED_PREFIX + modelId, false)
        return isDownloaded
    }
    
    suspend fun downloadModel(modelId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Update status to downloading
                updateModelStatus(modelId, ModelDownloadStatus.DOWNLOADING)
                
                // Simulate download progress
                for (progress in 0..100 step 10) {
                    kotlinx.coroutines.delay(200) // Simulate download time
                    updateDownloadProgress(modelId, progress / 100f)
                }
                
                // Create model directory
                val modelDir = File(context.filesDir, "models/$modelId")
                modelDir.mkdirs()
                
                // Create a placeholder model file (in real implementation, download actual model)
                val modelFile = File(modelDir, "model.tflite")
                modelFile.writeText("# Placeholder model file for $modelId\n# In production, this would be the actual TensorFlow Lite model")
                
                // Mark as downloaded
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
                
                // Delete model files
                val modelDir = File(context.filesDir, "models/$modelId")
                if (modelDir.exists()) {
                    modelDir.deleteRecursively()
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
        return File(context.filesDir, "models/$modelId/model.tflite").absolutePath
    }
    
    fun getAvailableModelsList(): List<LanguageModel> = getAvailableModels()
}