package com.example.smartcampus.presentation.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcampus.domain.model.Message
import com.example.smartcampus.domain.model.User
import com.example.smartcampus.domain.repository.ChatRepository
import com.example.smartcampus.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChatUiState {
    object Loading : ChatUiState()
    data class Success(val messages: List<Message>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _chatState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val chatState: StateFlow<ChatUiState> = _chatState

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _uploadState = MutableStateFlow<String?>(null)
    val uploadState: StateFlow<String?> = _uploadState

    init {
        fetchUsers()
    }

    fun fetchMessages(chatId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(chatId)
                .catch { e ->
                    _chatState.value = ChatUiState.Error(
                        e.message ?: "Failed to load messages"
                    )
                }
                .collect { messages ->
                    _chatState.value = ChatUiState.Success(messages)
                }
        }
    }

    private fun fetchUsers() {
        viewModelScope.launch {
            chatRepository.getUsers()
                .catch { }
                .collect { users ->
                    _users.value = users
                }
        }
    }

    fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        text: String
    ) {
        viewModelScope.launch {
            val message = Message(
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                text = text,
                messageType = "text",
                sentAt = System.currentTimeMillis()
            )
            sendMessageUseCase(message)
        }
    }

    fun sendImage(
        chatId: String,
        senderId: String,
        senderName: String,
        imageUri: Uri
    ) {
        viewModelScope.launch {
            _uploadState.value = "Uploading image..."
            chatRepository.sendImageMessage(chatId, senderId, senderName, imageUri)
                .onSuccess { _uploadState.value = null }
                .onFailure { _uploadState.value = "Failed to send image" }
        }
    }

    fun sendFile(
        chatId: String,
        senderId: String,
        senderName: String,
        fileUri: Uri,
        fileName: String
    ) {
        viewModelScope.launch {
            _uploadState.value = "Uploading file..."
            chatRepository.sendFileMessage(chatId, senderId, senderName, fileUri, fileName)
                .onSuccess { _uploadState.value = null }
                .onFailure { _uploadState.value = "Failed to send file" }
        }
    }

    fun sendAudio(
        chatId: String,
        senderId: String,
        senderName: String,
        audioUri: Uri
    ) {
        viewModelScope.launch {
            _uploadState.value = "Uploading audio..."
            chatRepository.sendAudioMessage(chatId, senderId, senderName, audioUri)
                .onSuccess { _uploadState.value = null }
                .onFailure { _uploadState.value = "Failed to send audio" }
        }
    }

    fun resetUploadState() {
        _uploadState.value = null
    }

    fun deleteForMe(chatId: String, messageId: String, userId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessageForMe(chatId, messageId, userId)
        }
    }

    fun deleteForEveryone(chatId: String, messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessageForEveryone(chatId, messageId)
        }
    }

    fun editMessage(chatId: String, messageId: String, newText: String) {
        viewModelScope.launch {
            val canEdit = (chatState.value as? ChatUiState.Success)
                ?.messages
                ?.find { it.id == messageId }
                ?.let { System.currentTimeMillis() - it.sentAt < 5 * 60 * 1000 }
                ?: false
            if (canEdit) {
                chatRepository.editMessage(chatId, messageId, newText)
                    .onFailure { _uploadState.value = "Failed to edit message" }
            } else {
                _uploadState.value = "Cannot edit after 5 minutes"
            }
        }
    }

    fun forwardMessage(
        message: Message,
        targetChatId: String,
        senderId: String,
        senderName: String
    ) {
        viewModelScope.launch {
            chatRepository.forwardMessage(message, targetChatId, senderId, senderName)
                .onSuccess { _uploadState.value = "Message forwarded!" }
                .onFailure { _uploadState.value = "Failed to forward message" }
        }
    }
}