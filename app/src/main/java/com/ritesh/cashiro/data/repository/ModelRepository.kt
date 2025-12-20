package com.ritesh.cashiro.data.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import com.ritesh.cashiro.core.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _modelState = MutableStateFlow(ModelState.NOT_DOWNLOADED)
    val modelState: Flow<ModelState> = _modelState.asStateFlow()
    
    fun getModelFile(): File {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), Constants.ModelDownload.MODEL_FILE_NAME)
        Log.d("ModelRepository", "Model file path: ${file.absolutePath}")
        return file
    }
    
    fun isModelDownloaded(): Boolean {
        val modelFile = getModelFile()
        val exists = modelFile.exists()
        val size = if (exists) modelFile.length() else 0
        val expectedSize = Constants.ModelDownload.MODEL_SIZE_BYTES
        // Allow 5% variance in file size as download sizes can vary
        // But also accept any file over 2GB as models can vary in size
        val minSize = minOf((expectedSize * 0.95).toLong(), 2L * 1024L * 1024L * 1024L) // 95% of expected or 2GB minimum
        val isDownloaded = exists && size >= minSize
        
        Log.d("ModelRepository", "Checking model: exists=$exists, size=$size bytes (${size/1024/1024}MB), expectedSize=$expectedSize, minSize=$minSize, isDownloaded=$isDownloaded")
        return isDownloaded
    }
    
    fun updateModelState(state: ModelState) {
        Log.d("ModelRepository", "Updating model state from ${_modelState.value} to $state")
        _modelState.value = state
    }
    
    fun checkModelState() {
        val newState = if (isModelDownloaded()) {
            ModelState.READY
        } else {
            ModelState.NOT_DOWNLOADED
        }
        Log.d("ModelRepository", "checkModelState: setting state to $newState")
        _modelState.value = newState
    }
    
    fun deleteModel(): Boolean {
        val modelFile = getModelFile()
        return if (modelFile.exists()) {
            val deleted = modelFile.delete()
            if (deleted) {
                _modelState.value = ModelState.NOT_DOWNLOADED
            }
            deleted
        } else {
            false
        }
    }
}

enum class ModelState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    READY,
    LOADING,
    ERROR
}