package com.ritesh.cashiro.data.service

import android.content.Context
import com.ritesh.cashiro.domain.service.LlmService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LlmService {
    
    override suspend fun initialize(modelPath: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun createConversation(
        systemPrompt: String,
        history: List<Pair<String, Boolean>>
    ): Result<Unit> {
        return Result.success(Unit)
    }
    
    override fun sendMessage(message: String): Flow<String> {
        return emptyFlow()
    }
    
    override fun hasActiveConversation(): Boolean {
        return false
    }
    
    override suspend fun closeConversation() {
    }
    
    override suspend fun reset() {
    }
    
    override fun isInitialized(): Boolean {
        return false
    }
}    
