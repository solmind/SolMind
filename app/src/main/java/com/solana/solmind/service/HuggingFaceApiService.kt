package com.solana.solmind.service

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Hugging Face API service for downloading models and accessing model information
 */
interface HuggingFaceApiService {
    
    /**
     * Download a model file from Hugging Face Hub
     * @param url The complete download URL for the model file
     * @param authToken Optional Hugging Face API token for private models
     */
    @Streaming
    @GET
    suspend fun downloadModelFile(
        @Url url: String,
        @Header("Authorization") authToken: String? = null
    ): Response<ResponseBody>
    
    /**
     * Get model information from Hugging Face Hub
     * @param modelId The model identifier (e.g., "google/flan-t5-small")
     * @param authToken Optional Hugging Face API token
     */
    @GET("api/models/{modelId}")
    suspend fun getModelInfo(
        @Path("modelId") modelId: String,
        @Header("Authorization") authToken: String? = null
    ): Response<HuggingFaceModelInfo>
    
    /**
     * List files in a model repository
     * @param modelId The model identifier
     * @param authToken Optional Hugging Face API token
     */
    @GET("api/models/{modelId}/tree/main")
    suspend fun getModelFiles(
        @Path("modelId") modelId: String,
        @Header("Authorization") authToken: String? = null
    ): Response<List<HuggingFaceFileInfo>>
}

/**
 * Data class representing Hugging Face model information
 */
data class HuggingFaceModelInfo(
    val id: String,
    val modelId: String,
    val author: String?,
    val sha: String,
    val downloads: Int,
    val likes: Int,
    val library_name: String?,
    val tags: List<String>,
    val pipeline_tag: String?,
    val createdAt: String,
    val lastModified: String,
    val private: Boolean,
    val gated: Boolean,
    val disabled: Boolean,
    val config: Map<String, Any>?
)

/**
 * Data class representing file information in a Hugging Face model repository
 */
data class HuggingFaceFileInfo(
    val path: String,
    val type: String, // "file" or "directory"
    val oid: String?,
    val size: Long?,
    val lfs: HuggingFaceLfsInfo?
)

/**
 * Data class for Git LFS file information
 */
data class HuggingFaceLfsInfo(
    val oid: String,
    val size: Long,
    val pointerSize: Int
)