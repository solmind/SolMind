package com.solana.solmind.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TensorFlowLiteInference @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null
    private var currentModelId: String? = null
    
    suspend fun loadModel(modelPath: String, modelId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Close existing interpreter if loading a different model
                if (currentModelId != modelId) {
                    interpreter?.close()
                    interpreter = null
                }
                
                if (interpreter == null) {
                    val modelFile = java.io.File(modelPath, "model.tflite")
                    
                    if (!modelFile.exists()) {
                        return@withContext false
                    }
                    
                    val fileInputStream = FileInputStream(modelFile)
                    val fileChannel = fileInputStream.channel
                    val modelBuffer: MappedByteBuffer = fileChannel.map(
                        FileChannel.MapMode.READ_ONLY,
                        0,
                        fileChannel.size()
                    )
                    
                    // Configure interpreter options for better performance
                    val options = Interpreter.Options().apply {
                        setNumThreads(4) // Use multiple threads for inference
                        setUseNNAPI(true) // Use Android Neural Networks API if available
                    }
                    
                    interpreter = Interpreter(modelBuffer, options)
                    currentModelId = modelId
                    
                    fileInputStream.close()
                }
                
                true
            } catch (e: Exception) {
                interpreter?.close()
                interpreter = null
                currentModelId = null
                false
            }
        }
    }
    
    suspend fun runInference(
        inputTokens: IntArray,
        maxInputLength: Int = 512,
        maxOutputLength: Int = 512
    ): IntArray? {
        return withContext(Dispatchers.IO) {
            try {
                val interpreter = this@TensorFlowLiteInference.interpreter ?: return@withContext null
                
                // Prepare input tensor
                val inputShape = intArrayOf(1, maxInputLength)
                val inputBuffer = TensorBuffer.createFixedSize(inputShape, org.tensorflow.lite.DataType.INT32)
                
                // Pad or truncate input tokens to match expected length
                val paddedInput = IntArray(maxInputLength)
                val copyLength = minOf(inputTokens.size, maxInputLength)
                System.arraycopy(inputTokens, 0, paddedInput, 0, copyLength)
                
                inputBuffer.loadArray(paddedInput)
                
                // Prepare output tensor
                val outputShape = intArrayOf(1, maxOutputLength)
                val outputBuffer = TensorBuffer.createFixedSize(outputShape, org.tensorflow.lite.DataType.INT32)
                
                // Run inference
                interpreter.run(inputBuffer.buffer, outputBuffer.buffer)
                
                outputBuffer.intArray
                
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun runTextClassification(
        inputTokens: IntArray,
        numClasses: Int = 10
    ): FloatArray? {
        return withContext(Dispatchers.IO) {
            try {
                val interpreter = this@TensorFlowLiteInference.interpreter ?: return@withContext null
                
                // For classification models like DistilBERT
                val inputShape = intArrayOf(1, inputTokens.size)
                val inputBuffer = TensorBuffer.createFixedSize(inputShape, org.tensorflow.lite.DataType.INT32)
                inputBuffer.loadArray(inputTokens)
                
                // Output is typically a probability distribution over classes
                val outputShape = intArrayOf(1, numClasses)
                val outputBuffer = TensorBuffer.createFixedSize(outputShape, org.tensorflow.lite.DataType.FLOAT32)
                
                interpreter.run(inputBuffer.buffer, outputBuffer.buffer)
                
                outputBuffer.floatArray
                
            } catch (e: Exception) {
                null
            }
        }
    }
    
    fun getModelInfo(): ModelInfo? {
        val interpreter = this.interpreter ?: return null
        
        return try {
            val inputTensor = interpreter.getInputTensor(0)
            val outputTensor = interpreter.getOutputTensor(0)
            
            ModelInfo(
                inputShape = inputTensor.shape(),
                outputShape = outputTensor.shape(),
                inputDataType = inputTensor.dataType(),
                outputDataType = outputTensor.dataType()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun isModelLoaded(): Boolean {
        return interpreter != null
    }
    
    fun getCurrentModelId(): String? {
        return currentModelId
    }
    
    fun closeModel() {
        interpreter?.close()
        interpreter = null
        currentModelId = null
    }
}

data class ModelInfo(
    val inputShape: IntArray,
    val outputShape: IntArray,
    val inputDataType: org.tensorflow.lite.DataType,
    val outputDataType: org.tensorflow.lite.DataType
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