package com.example.smartcampus.domain.model

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val fileUrl: String = "",
    val fileName: String = "",
    val audioUrl: String = "",
    val messageType: String = "text",
    val replyToId: String = "",
    val replyToText: String = "",
    val replyToSender: String = "",
    val isEdited: Boolean = false,
    val editedAt: Long = 0L,
    val deletedForAll: Boolean = false,
    val deletedFor: List<String> = emptyList(),
    val sentAt: Long = 0L
)