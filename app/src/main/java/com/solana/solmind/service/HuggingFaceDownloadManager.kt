package com.solana.solmind.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Download progress data class
 */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percentage: Float
)

/**
 * Download result sealed class
 */
sealed class DownloadResult {
    object Success : DownloadResult()
    data class Error(val message: String, val exception: Throwable? = null) : DownloadResult()
}

/**
 * Manages downloading models from Hugging Face Hub with progress tracking
 */
@Singleton
class HuggingFaceDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: HuggingFaceApiService
) {
    
    /**
     * Download a model file with progress tracking
     * @param modelId The model identifier for local storage
     * @param downloadUrl The complete download URL
     * @param fileName The name of the file to save (e.g., "model.tflite")
     * @param authToken Optional Hugging Face API token
     * @return Flow of download progress and final result
     */
    suspend fun downloadModelWithProgress(
        modelId: String,
        downloadUrl: String,
        fileName: String = "model.tflite",
        authToken: String? = null
    ): Flow<DownloadProgress> = flow {
        try {
            // Create model directory
            val modelDir = File(context.filesDir, "models/$modelId")
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }
            
            val modelFile = File(modelDir, fileName)
            
            // Make the API call
            val response = apiService.downloadModelFile(
                url = downloadUrl,
                authToken = authToken?.let { "Bearer $it" }
            )
            
            if (!response.isSuccessful) {
                throw Exception("Failed to download model: HTTP ${response.code()} - ${response.message()}")
            }
            
            val responseBody = response.body()
                ?: throw Exception("Response body is null")
            
            // Get content length for progress tracking
            val contentLength = responseBody.contentLength()
            
            // Download with progress tracking
            downloadWithProgress(responseBody, modelFile, contentLength) { progress ->
                emit(progress)
            }
            
        } catch (e: Exception) {
            throw Exception("Download failed: ${e.message}", e)
        }
    }
    
    /**
     * Download a model file without progress tracking (simpler version)
     */
    suspend fun downloadModel(
        modelId: String,
        downloadUrl: String,
        fileName: String = "model.tflite",
        authToken: String? = null
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            // Create model directory
            val modelDir = File(context.filesDir, "models/$modelId")
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }
            
            val modelFile = File(modelDir, fileName)
            
            // Make the API call
            val response = apiService.downloadModelFile(
                url = downloadUrl,
                authToken = authToken?.let { "Bearer $it" }
            )
            
            if (!response.isSuccessful) {
                return@withContext DownloadResult.Error(
                    "Failed to download model: HTTP ${response.code()} - ${response.message()}"
                )
            }
            
            val responseBody = response.body()
                ?: return@withContext DownloadResult.Error("Response body is null")
            
            // Save file
            responseBody.byteStream().use { inputStream ->
                FileOutputStream(modelFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            DownloadResult.Success
            
        } catch (e: Exception) {
            DownloadResult.Error("Download failed: ${e.message}", e)
        }
    }
    
    /**
     * Get model information from Hugging Face Hub
     */
    suspend fun getModelInfo(
        modelId: String,
        authToken: String? = null
    ): Result<HuggingFaceModelInfo> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getModelInfo(
                modelId = modelId,
                authToken = authToken?.let { "Bearer $it" }
            )
            
            if (response.isSuccessful) {
                response.body()?.let { modelInfo ->
                    Result.success(modelInfo)
                } ?: Result.failure(Exception("Model info response body is null"))
            } else {
                Result.failure(Exception("Failed to get model info: HTTP ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get list of files in a model repository
     */
    suspend fun getModelFiles(
        modelId: String,
        authToken: String? = null
    ): Result<List<HuggingFaceFileInfo>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getModelFiles(
                modelId = modelId,
                authToken = authToken?.let { "Bearer $it" }
            )
            
            if (response.isSuccessful) {
                response.body()?.let { files ->
                    Result.success(files)
                } ?: Result.failure(Exception("Model files response body is null"))
            } else {
                Result.failure(Exception("Failed to get model files: HTTP ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Helper function to download with progress tracking
     */
    private suspend fun downloadWithProgress(
        responseBody: ResponseBody,
        targetFile: File,
        contentLength: Long,
        onProgress: suspend (DownloadProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        val inputStream: InputStream = responseBody.byteStream()
        val outputStream = FileOutputStream(targetFile)
        
        try {
            val buffer = ByteArray(8192) // 8KB buffer
            var bytesDownloaded = 0L
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesDownloaded += bytesRead
                
                // Calculate progress
                val percentage = if (contentLength > 0) {
                    (bytesDownloaded.toFloat() / contentLength.toFloat()) * 100f
                } else {
                    0f // Unknown content length
                }
                
                // Emit progress
                onProgress(
                    DownloadProgress(
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = contentLength,
                        percentage = percentage
                    )
                )
            }
        } finally {
            inputStream.close()
            outputStream.close()
        }
    }
    
    /**
     * Check if a model file exists locally
     */
    fun isModelDownloaded(modelId: String, fileName: String = "model.tflite"): Boolean {
        val modelFile = File(context.filesDir, "models/$modelId/$fileName")
        return modelFile.exists() && modelFile.length() > 0
    }
    
    /**
     * Get the local path of a downloaded model
     */
    fun getModelPath(modelId: String, fileName: String = "model.tflite"): String? {
        val modelFile = File(context.filesDir, "models/$modelId/$fileName")
        return if (modelFile.exists()) modelFile.absolutePath else null
    }
    
    /**
     * Delete a downloaded model
     */
    suspend fun deleteModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelDir = File(context.filesDir, "models/$modelId")
            if (modelDir.exists()) {
                val deleted = modelDir.deleteRecursively()
                if (deleted) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete model directory"))
                }
            } else {
                Result.success(Unit) // Already deleted
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}