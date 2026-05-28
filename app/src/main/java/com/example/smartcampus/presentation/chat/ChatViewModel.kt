package com.example.smartcampus.presentation.chat

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
                sentAt = System.currentTimeMillis()
            )
            sendMessageUseCase(message)
        }
    }
}