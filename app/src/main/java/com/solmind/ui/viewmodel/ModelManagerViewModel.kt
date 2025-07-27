package com.solmind.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solmind.service.HuggingFaceModelInfo
import com.solmind.service.HuggingFaceFileInfo
import com.solmind.service.LanguageModel
import com.solmind.service.ModelManager
import com.solmind.service.ModelState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelManagerViewModel @Inject constructor(
    private val modelManager: ModelManager
) : ViewModel() {
    
    val selectedModel: StateFlow<LanguageModel?> = modelManager.selectedModel
    val modelStates: StateFlow<List<ModelState>> = modelManager.modelStates
    val downloadProgress: StateFlow<Map<String, Float>> = modelManager.downloadProgress
    val availableModels: StateFlow<List<LanguageModel>> = modelManager.availableModels
    
    fun selectModel(model: LanguageModel?) {
        modelManager.selectModel(model)
    }
    
    fun isModelDownloaded(modelId: String): Boolean {
        return modelManager.isModelDownloaded(modelId)
    }
    
    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            modelManager.downloadModel(modelId)
        }
    }
    
    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            modelManager.deleteModel(modelId)
        }
    }
    
    suspend fun getModelInfoFromHub(modelId: String): Result<HuggingFaceModelInfo>? {
        return modelManager.getModelInfoFromHub(modelId)
    }
    
    suspend fun getModelFilesFromHub(modelId: String): Result<List<HuggingFaceFileInfo>>? {
        return modelManager.getModelFilesFromHub(modelId)
    }
    
    fun isHuggingFaceConfigValid(): Boolean {
        return modelManager.isHuggingFaceConfigValid()
    }
    
    fun setHuggingFaceApiToken(token: String?) {
        modelManager.setHuggingFaceApiToken(token)
    }
    
    fun setHuggingFaceAuthEnabled(enabled: Boolean) {
        modelManager.setHuggingFaceAuthEnabled(enabled)
    }
    
    fun refreshModelSizes() {
        viewModelScope.launch {
            modelManager.loadModelSizes()
        }
    }
    
    fun loadModelSizes() {
        viewModelScope.launch {
            modelManager.loadModelSizes()
        }
    }
}