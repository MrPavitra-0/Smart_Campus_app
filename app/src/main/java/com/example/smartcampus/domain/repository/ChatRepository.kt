package com.example.smartcampus.domain.repository

import android.net.Uri
import com.example.smartcampus.domain.model.Message
import com.example.smartcampus.domain.model.User
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(chatId: String): Flow<List<Message>>
    suspend fun sendMessage(message: Message): Result<Unit>
    suspend fun sendImageMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        imageUri: Uri
    ): Result<Unit>
    suspend fun sendFileMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        fileUri: Uri,
        fileName: String
    ): Result<Unit>
    suspend fun sendAudioMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        audioUri: Uri
    ): Result<Unit>
    fun getUsers(): Flow<List<User>>
}