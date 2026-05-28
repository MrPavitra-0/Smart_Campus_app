package com.example.smartcampus.domain.repository

import com.example.smartcampus.domain.model.Message
import com.example.smartcampus.domain.model.User
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(chatId: String): Flow<List<Message>>
    suspend fun sendMessage(message: Message): Result<Unit>
    fun getUsers(): Flow<List<User>>
}