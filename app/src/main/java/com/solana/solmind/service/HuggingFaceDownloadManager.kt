package com.solana.solmind.service

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
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
    
    companion object {
        private const val TAG = "HuggingFaceDownloadManager"
    }
    
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
        fileName: String = "pytorch_model.bin",
        authToken: String? = null
    ): Flow<DownloadProgress> = flow {
        try {
            Log.d(TAG, "Starting download for model: $modelId")
            Log.d(TAG, "Download URL: $downloadUrl")
            Log.d(TAG, "File name: $fileName")
            Log.d(TAG, "Auth token provided: ${authToken != null}")
            
            // Create model directory
            val modelDir = File(context.filesDir, "models/$modelId")
            if (!modelDir.exists()) {
                val created = modelDir.mkdirs()
                Log.d(TAG, "Model directory created: $created, path: ${modelDir.absolutePath}")
            }
            
            val modelFile = File(modelDir, fileName)
            Log.d(TAG, "Target file path: ${modelFile.absolutePath}")
            
            // Make the API call
            Log.d(TAG, "Making API call to download model file...")
            val response = apiService.downloadModelFile(
                url = downloadUrl,
                authToken = authToken?.let { "Bearer $it" }
            )
            
            Log.d(TAG, "API response received - Code: ${response.code()}, Message: ${response.message()}")
            
            if (!response.isSuccessful) {
                val errorMsg = "Failed to download model: HTTP ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMsg)
                throw Exception(errorMsg)
            }
            
            val responseBody = response.body()
            if (responseBody == null) {
                val errorMsg = "Response body is null"
                Log.e(TAG, errorMsg)
                throw Exception(errorMsg)
            }
            
            // Get content length for progress tracking
            val contentLength = responseBody.contentLength()
            Log.d(TAG, "Content length: $contentLength bytes")
            
            // Download with progress tracking
            downloadWithProgress(responseBody, modelFile, contentLength) { progress ->
                Log.v(TAG, "Download progress: ${progress.percentage}% (${progress.bytesDownloaded}/${progress.totalBytes})")
                emit(progress)
            }
            
            Log.d(TAG, "Download completed successfully for model: $modelId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for model $modelId: ${e.message}", e)
            throw Exception("Download failed: ${e.message}", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Download a model file without progress tracking (simpler version)
     */
    suspend fun downloadModel(
        modelId: String,
        downloadUrl: String,
        fileName: String = "pytorch_model.bin",
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
    ) {
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
    fun isModelDownloaded(modelId: String, fileName: String = "pytorch_model.bin"): Boolean {
        val modelFile = File(context.filesDir, "models/$modelId/$fileName")
        return modelFile.exists() && modelFile.length() > 0
    }
    
    /**
     * Get the local path of a downloaded model
     */
    fun getModelPath(modelId: String, fileName: String = "pytorch_model.bin"): String? {
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