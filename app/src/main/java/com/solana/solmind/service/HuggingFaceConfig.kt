package com.solana.solmind.service

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration manager for Hugging Face API settings
 */
@Singleton
class HuggingFaceConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("huggingface_config", Context.MODE_PRIVATE)
    
    companion object {
        private const val API_TOKEN_KEY = "hf_api_token"
        private const val USE_AUTH_KEY = "use_auth"
        
        // Common Hugging Face model URLs for easy access
        object ModelUrls {
            const val FLAN_T5_SMALL = "https://huggingface.co/google/flan-t5-small/resolve/main/pytorch_model.bin"
            const val FLAN_T5_BASE = "https://huggingface.co/google/flan-t5-base/resolve/main/pytorch_model.bin"
            const val FLAN_T5_LARGE = "https://huggingface.co/google/flan-t5-large/resolve/main/pytorch_model.bin"
            const val DISTILBERT_BASE = "https://huggingface.co/distilbert-base-uncased/resolve/main/pytorch_model.bin"
            const val BERT_BASE = "https://huggingface.co/bert-base-uncased/resolve/main/pytorch_model.bin"
        }
        
        // Model repository identifiers
        object ModelRepos {
            const val FLAN_T5_SMALL = "google/flan-t5-small"
            const val FLAN_T5_BASE = "google/flan-t5-base"
            const val FLAN_T5_LARGE = "google/flan-t5-large"
            const val DISTILBERT_BASE = "distilbert-base-uncased"
            const val BERT_BASE = "bert-base-uncased"
        }
    }
    
    /**
     * Get the stored Hugging Face API token
     */
    fun getApiToken(): String? {
        return if (isAuthEnabled()) {
            prefs.getString(API_TOKEN_KEY, null)
        } else {
            null
        }
    }
    
    /**
     * Set the Hugging Face API token
     */
    fun setApiToken(token: String?) {
        prefs.edit().putString(API_TOKEN_KEY, token).apply()
    }
    
    /**
     * Check if authentication is enabled
     */
    fun isAuthEnabled(): Boolean {
        return prefs.getBoolean(USE_AUTH_KEY, false)
    }
    
    /**
     * Enable or disable authentication
     */
    fun setAuthEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(USE_AUTH_KEY, enabled).apply()
    }
    
    /**
     * Clear all stored configuration
     */
    fun clearConfig() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Get the authorization header value for API requests
     */
    fun getAuthHeader(): String? {
        val token = getApiToken()
        return if (token != null && isAuthEnabled()) {
            "Bearer $token"
        } else {
            null
        }
    }
    
    /**
     * Validate if the current configuration is valid for making API requests
     */
    fun isConfigValid(): Boolean {
        return if (isAuthEnabled()) {
            !getApiToken().isNullOrBlank()
        } else {
            true // No auth required
        }
    }
    
    /**
     * Get model download URL by model ID
     */
    fun getModelDownloadUrl(modelId: String, fileName: String = "pytorch_model.bin"): String? {
        return when (modelId) {
            "flan-t5-small" -> "https://huggingface.co/${ModelRepos.FLAN_T5_SMALL}/resolve/main/$fileName"
            "flan-t5-base" -> "https://huggingface.co/${ModelRepos.FLAN_T5_BASE}/resolve/main/$fileName"
            "flan-t5-large" -> "https://huggingface.co/${ModelRepos.FLAN_T5_LARGE}/resolve/main/$fileName"
            "distilbert-base" -> "https://huggingface.co/${ModelRepos.DISTILBERT_BASE}/resolve/main/$fileName"
            "bert-base" -> "https://huggingface.co/${ModelRepos.BERT_BASE}/resolve/main/$fileName"
            else -> null
        }
    }
    
    /**
     * Get model repository identifier by model ID
     */
    fun getModelRepo(modelId: String): String? {
        return when (modelId) {
            "flan-t5-small" -> ModelRepos.FLAN_T5_SMALL
            "flan-t5-base" -> ModelRepos.FLAN_T5_BASE
            "flan-t5-large" -> ModelRepos.FLAN_T5_LARGE
            "distilbert-base" -> ModelRepos.DISTILBERT_BASE
            "bert-base" -> ModelRepos.BERT_BASE
            else -> null
        }
    }
}