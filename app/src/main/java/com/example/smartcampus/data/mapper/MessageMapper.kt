package com.example.smartcampus.data.mapper

import com.example.smartcampus.domain.model.Message

fun Map<String, Any?>.toMessage(id: String): Message {
    return Message(
        id = id,
        chatId = this["chatId"] as? String ?: "",
        senderId = this["senderId"] as? String ?: "",
        senderName = this["senderName"] as? String ?: "",
        text = this["text"] as? String ?: "",
        imageUrl = this["imageUrl"] as? String ?: "",
        fileUrl = this["fileUrl"] as? String ?: "",
        fileName = this["fileName"] as? String ?: "",
        audioUrl = this["audioUrl"] as? String ?: "",
        messageType = this["messageType"] as? String ?: "text",
        replyToId = this["replyToId"] as? String ?: "",
        replyToText = this["replyToText"] as? String ?: "",
        replyToSender = this["replyToSender"] as? String ?: "",
        isEdited = this["isEdited"] as? Boolean ?: false,
        editedAt = this["editedAt"] as? Long ?: 0L,
        deletedForAll = this["deletedForAll"] as? Boolean ?: false,
        deletedFor = (this["deletedFor"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        sentAt = this["sentAt"] as? Long ?: 0L
    )
}

fun Message.toMap(): Map<String, Any> {
    return mapOf(
        "chatId" to chatId,
        "senderId" to senderId,
        "senderName" to senderName,
        "text" to text,
        "imageUrl" to imageUrl,
        "fileUrl" to fileUrl,
        "fileName" to fileName,
        "audioUrl" to audioUrl,
        "messageType" to messageType,
        "replyToId" to replyToId,
        "replyToText" to replyToText,
        "replyToSender" to replyToSender,
        "isEdited" to isEdited,
        "editedAt" to editedAt,
        "deletedForAll" to deletedForAll,
        "deletedFor" to deletedFor,
        "sentAt" to sentAt
    )
}