package com.solana.ailedger.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solana.ailedger.data.model.LedgerEntry
import com.solana.ailedger.repository.LedgerRepository
import com.solana.ailedger.service.AIService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: LedgerRepository,
    private val aiService: AIService
) : ViewModel() {
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _extractedText = MutableStateFlow<String?>(null)
    val extractedText: StateFlow<String?> = _extractedText.asStateFlow()
    
    private val _suggestedEntry = MutableStateFlow<LedgerEntry?>(null)
    val suggestedEntry: StateFlow<LedgerEntry?> = _suggestedEntry.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun captureAndProcessImage(imageCapture: ImageCapture, executor: ExecutorService) {
        _isProcessing.value = true
        _error.value = null
        
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(
                createTempFile("receipt", ".jpg")
            ).build(),
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    output.savedUri?.let { uri ->
                        processImageFromUri(uri.path!!)
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    _isProcessing.value = false
                    _error.value = "Failed to capture image: ${exception.message}"
                }
            }
        )
    }
    
    fun processImageFromBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                
                // Extract text from image using ML Kit
                val extractedText = aiService.extractTextFromImage(bitmap)
                _extractedText.value = extractedText
                
                if (extractedText.isNotEmpty()) {
                    // Create suggested entry using AI
                    val suggestedEntry = repository.createEntryFromText(extractedText)
                    _suggestedEntry.value = suggestedEntry
                } else {
                    _error.value = "No text found in the image. Please try again with a clearer image."
                }
                
            } catch (e: Exception) {
                _error.value = "Error processing image: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    private fun processImageFromUri(imagePath: String) {
        viewModelScope.launch {
            try {
                // Load bitmap from file
                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap != null) {
                    processImageFromBitmap(bitmap)
                } else {
                    _error.value = "Failed to load captured image"
                    _isProcessing.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error loading image: ${e.message}"
                _isProcessing.value = false
            }
        }
    }
    
    fun clearSuggestedEntry() {
        _suggestedEntry.value = null
        _extractedText.value = null
    }
    
    fun clearError() {
        _error.value = null
    }
    
    private fun createTempFile(prefix: String, suffix: String): java.io.File {
        return java.io.File.createTempFile(prefix, suffix)
    }
    
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
    
    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}