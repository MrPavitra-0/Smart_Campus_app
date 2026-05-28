package com.example.smartcampus.domain.usecase


import com.example.smartcampus.domain.model.Message
import com.example.smartcampus.domain.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(message: Message) =
        chatRepository.sendMessage(message)
}