package com.ritesh.cashiro.ui.screens.unrecognized

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.core.Constants
import com.ritesh.cashiro.data.database.entity.UnrecognizedSmsEntity
import com.ritesh.cashiro.data.repository.UnrecognizedSmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class UnrecognizedSmsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val unrecognizedSmsRepository: UnrecognizedSmsRepository
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _showReported = MutableStateFlow(true)
    val showReported: StateFlow<Boolean> = _showReported.asStateFlow()
    
    private val allMessages = unrecognizedSmsRepository.getAllVisible()
    
    val unrecognizedMessages: StateFlow<List<UnrecognizedSmsEntity>> = 
        combine(allMessages, _showReported) { messages, showReported ->
            if (showReported) {
                messages
            } else {
                messages.filter { !it.reported }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun toggleShowReported() {
        _showReported.value = !_showReported.value
    }
    
    fun reportMessage(message: UnrecognizedSmsEntity) {
        viewModelScope.launch {
            try {
                // URL encode the parameters
                val encodedMessage = URLEncoder.encode(message.smsBody, "UTF-8")
                val encodedSender = URLEncoder.encode(message.sender, "UTF-8")
                
                // Encrypt device data for verification
                val encryptedDeviceData = com.ritesh.cashiro.utils.DeviceEncryption.encryptDeviceData(context)
                Log.d("UnrecognizedSmsViewModel", "Encrypted device data: ${encryptedDeviceData?.take(50)}... (length: ${encryptedDeviceData?.length})")
                
                val encodedDeviceData = if (encryptedDeviceData != null) {
                    URLEncoder.encode(encryptedDeviceData, "UTF-8")
                } else {
                    ""
                }
                Log.d("UnrecognizedSmsViewModel", "Encoded device data: ${encodedDeviceData.take(50)}... (length: ${encodedDeviceData.length})")
                
                // Create the report URL using hash fragment for privacy
                val url = "${Constants.Links.WEB_PARSER_URL}/#message=$encodedMessage&sender=$encodedSender&device=$encodedDeviceData&autoparse=true"
                Log.d("UnrecognizedSmsViewModel", "Full URL length: ${url.length}")
                
                // Open in browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                
                // Mark as reported
                unrecognizedSmsRepository.markAsReported(listOf(message.id))
                
                Log.d("UnrecognizedSmsViewModel", "Opened report for message from: ${message.sender}")
            } catch (e: Exception) {
                Log.e("UnrecognizedSmsViewModel", "Error opening report", e)
            }
        }
    }
    
    fun deleteMessage(message: UnrecognizedSmsEntity) {
        viewModelScope.launch {
            try {
                // Delete the specific message
                unrecognizedSmsRepository.deleteMessage(message.id)
                Log.d("UnrecognizedSmsViewModel", "Deleted message from: ${message.sender}")
            } catch (e: Exception) {
                Log.e("UnrecognizedSmsViewModel", "Error deleting message", e)
            }
        }
    }
    
    fun deleteAllMessages() {
        viewModelScope.launch {
            try {
                unrecognizedSmsRepository.deleteAll()
                Log.d("UnrecognizedSmsViewModel", "Deleted all unrecognized messages")
            } catch (e: Exception) {
                Log.e("UnrecognizedSmsViewModel", "Error deleting all messages", e)
            }
        }
    }
}
