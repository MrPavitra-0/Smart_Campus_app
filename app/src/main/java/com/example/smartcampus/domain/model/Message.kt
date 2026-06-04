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
    val messageType: String = "text", // "text", "image", "file", "audio"
    val sentAt: Long = 0L
)