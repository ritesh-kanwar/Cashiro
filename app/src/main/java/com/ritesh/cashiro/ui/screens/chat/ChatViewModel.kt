package com.ritesh.cashiro.ui.screens.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.database.entity.ChatMessage
import com.ritesh.cashiro.data.repository.LlmRepository
import com.ritesh.cashiro.data.repository.ModelRepository
import com.ritesh.cashiro.data.repository.ModelState
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.utils.TokenUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val llmRepository: LlmRepository,
    private val modelRepository: ModelRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    private val _contextMessage = MutableStateFlow<ChatMessage?>(null)
    
    val messages: StateFlow<List<ChatMessage>> = combine(
        llmRepository.getAllMessages(),
        _contextMessage
    ) { dbMessages, contextMsg ->
        if (dbMessages.isEmpty() && contextMsg != null) {
            // Show context message only when chat is empty
            listOf(contextMsg)
        } else {
            // Show actual chat messages
            dbMessages
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val modelState: StateFlow<ModelState> = modelRepository.modelState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = if (modelRepository.isModelDownloaded()) ModelState.READY else ModelState.NOT_DOWNLOADED
        )
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private val _currentResponse = MutableStateFlow("")
    val currentResponse: StateFlow<String> = _currentResponse.asStateFlow()
    
    val isDeveloperModeEnabled = userPreferencesRepository.isDeveloperModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    // Get all messages including system for accurate token count
    private val allMessagesIncludingSystem = llmRepository.getAllMessagesIncludingSystem()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Chat statistics for developer mode
    val chatStats = allMessagesIncludingSystem.combine(currentResponse) { allMsgs, current ->
        // Calculate system prompt tokens separately
        val systemPromptText = allMsgs.filter { it.isSystemPrompt }.joinToString(" ") { it.message }
        val systemPromptTokens = if (systemPromptText.isNotEmpty()) {
            TokenUtils.estimateTokens(systemPromptText)
        } else {
            0
        }
        
        // Calculate total tokens
        val allText = allMsgs.joinToString(" ") { it.message } + " " + current
        val totalChars = allText.length
        val estimatedTokens = TokenUtils.estimateTokens(allText)
        val maxTokens = 4096 // Qwen 2.5 with KV cache size 4096
        
        // Count only visible messages for UI
        val visibleCount = allMsgs.count { !it.isSystemPrompt }
        
        ChatStats(
            messageCount = visibleCount,
            totalCharacters = totalChars,
            estimatedTokens = estimatedTokens,
            systemPromptTokens = systemPromptTokens,
            maxTokens = maxTokens,
            contextUsagePercent = TokenUtils.calculateContextUsagePercent(estimatedTokens, maxTokens)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatStats()
    )
    
    init {
        Log.d("ChatViewModel", "Initializing ChatViewModel")
        modelRepository.checkModelState()
        
        // Log initial state
        val isDownloaded = modelRepository.isModelDownloaded()
        Log.d("ChatViewModel", "Initial model downloaded check: $isDownloaded")
        
        // Also observe state changes
        viewModelScope.launch {
            modelRepository.modelState.collect { state ->
                Log.d("ChatViewModel", "Model state changed to: $state")
            }
        }
        
        // Load initial context message for display
        viewModelScope.launch {
            loadContextMessage()
        }
    }
    
    private suspend fun loadContextMessage() {
        val contextMessage = llmRepository.getFormattedContextForDisplay()
        _contextMessage.value = ChatMessage(
            message = contextMessage,
            isUser = false,
            isSystemPrompt = false
        )
    }
    
    fun sendMessage(message: String) {
        if (message.isBlank() || _uiState.value.isLoading) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            _currentResponse.value = ""
            
            try {
                // Use streaming for better UX
                llmRepository.sendMessageStream(message)
                    .catch { error ->
                        Log.e("ChatViewModel", "Error in stream", error)
                        val errorMessage = when {
                            error.message?.contains("memory is full") == true -> 
                                "Chat memory is full. Please clear the chat to continue."
                            error.message?.contains("downloading") == true ->
                                "Model is downloading. Please wait."
                            error.message?.contains("not downloaded") == true ->
                                "AI model not downloaded. Go to Settings to download."
                            else -> error.message ?: "Failed to generate response"
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                    .collect { partialResponse ->
                        _currentResponse.value += partialResponse
                    }
                
                // Stream completed successfully
                Log.d("ChatViewModel", "Stream completed, resetting state")
                _uiState.value = _uiState.value.copy(isLoading = false)
                _currentResponse.value = ""
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Exception in sendMessage", e)
                val errorMessage = when {
                    e.message?.contains("memory is full") == true -> 
                        "Chat memory is full. Please clear the chat to continue."
                    e.message?.contains("downloading") == true ->
                        "Model is downloading. Please wait."
                    e.message?.contains("not downloaded") == true ->
                        "AI model not downloaded. Go to Settings to download."
                    else -> e.message ?: "Failed to send message"
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
                _currentResponse.value = ""
            }
        }
    }
    
    fun clearChat() {
        viewModelScope.launch {
            llmRepository.deleteAllMessages()
            _uiState.value = _uiState.value.copy(
                error = null
            )
            // Reload context message after clearing chat
            loadContextMessage()
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ChatStats(
    val messageCount: Int = 0,
    val totalCharacters: Int = 0,
    val estimatedTokens: Int = 0,
    val systemPromptTokens: Int = 0,
    val maxTokens: Int = 1280,
    val contextUsagePercent: Int = 0
)